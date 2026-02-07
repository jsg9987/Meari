import { useState, useRef, useCallback } from 'react';

interface UseAudioRecorderOptions {
  onRecordingComplete?: (audioBlob: Blob) => void;
  onError?: (error: Error) => void;
}

export function useAudioRecorder({ onRecordingComplete, onError }: UseAudioRecorderOptions = {}) {
  const [isRecording, setIsRecording] = useState(false);
  const audioContextRef = useRef<AudioContext | null>(null);
  const audioWorkletNodeRef = useRef<AudioWorkletNode | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const sourceNodeRef = useRef<MediaStreamAudioSourceNode | null>(null);
  const isRecordingRef = useRef(false);
  const sampleRateRef = useRef<number>(48000);

  const cleanup = useCallback(() => {
    // 오디오 노드 정리
    if (sourceNodeRef.current) {
      sourceNodeRef.current.disconnect();
      sourceNodeRef.current = null;
    }

    if (audioWorkletNodeRef.current) {
      audioWorkletNodeRef.current.disconnect();
      audioWorkletNodeRef.current = null;
    }

    if (audioContextRef.current) {
      audioContextRef.current.close();
      audioContextRef.current = null;
    }

    // 스트림 정리
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
  }, []);

  const startRecording = useCallback(async () => {
    try {
      // 마이크 권한 요청 및 스트림 가져오기
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      // AudioContext 생성
      const audioContext = new AudioContext();
      audioContextRef.current = audioContext;
      sampleRateRef.current = audioContext.sampleRate;

      // AudioWorklet 모듈 로드
      await audioContext.audioWorklet.addModule('/audio-recorder-worklet.js');

      // AudioWorkletNode 생성
      const workletNode = new AudioWorkletNode(audioContext, 'audio-recorder-worklet');
      audioWorkletNodeRef.current = workletNode;

      // 마이크 스트림을 AudioContext에 연결
      const sourceNode = audioContext.createMediaStreamSource(stream);
      sourceNodeRef.current = sourceNode;

      // 소스 노드를 워크렛에 연결
      sourceNode.connect(workletNode);
      // destination에는 연결하지 않음 (마이크 피드백 방지)

      // 워크렛에서 메시지 수신 처리
      workletNode.port.onmessage = (event) => {
        if (event.data.eventType === 'recordingComplete') {
          const buffers: Float32Array[] = event.data.buffers;

          // PCM 데이터를 WAV로 변환
          const wavBlob = pcmToWav(buffers, sampleRateRef.current);
          onRecordingComplete?.(wavBlob);

          // 정리
          cleanup();
        }
      };

      // 녹음 시작 명령 전송
      workletNode.port.postMessage({ command: 'start' });

      isRecordingRef.current = true;
      setIsRecording(true);
    } catch (error) {
      console.error('Failed to start recording:', error);
      cleanup();
      onError?.(error as Error);
    }
  }, [onRecordingComplete, onError, cleanup]);

  const stopRecording = useCallback(() => {
    if (audioWorkletNodeRef.current && isRecordingRef.current) {
      // 녹음 중지 명령 전송
      audioWorkletNodeRef.current.port.postMessage({ command: 'stop' });
      isRecordingRef.current = false;
      setIsRecording(false);
    }
  }, []);

  return {
    isRecording,
    startRecording,
    stopRecording,
  };
}

/**
 * PCM 데이터 배열을 WAV Blob으로 변환
 */
function pcmToWav(buffers: Float32Array[], sampleRate: number): Blob {
  // 모든 버퍼를 하나로 합치기
  const totalLength = buffers.reduce((acc, buffer) => acc + buffer.length, 0);
  const pcmData = new Float32Array(totalLength);

  let offset = 0;
  for (const buffer of buffers) {
    pcmData.set(buffer, offset);
    offset += buffer.length;
  }

  // WAV 파일 생성
  const numberOfChannels = 1; // Mono
  const bitDepth = 16;
  const format = 1; // PCM

  const bytesPerSample = bitDepth / 8;
  const blockAlign = numberOfChannels * bytesPerSample;
  const dataLength = pcmData.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataLength);
  const view = new DataView(buffer);

  // WAV 헤더 작성
  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataLength, true);
  writeString(view, 8, 'WAVE');
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true); // fmt chunk size
  view.setUint16(20, format, true);
  view.setUint16(22, numberOfChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * blockAlign, true); // byte rate
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, bitDepth, true);
  writeString(view, 36, 'data');
  view.setUint32(40, dataLength, true);

  // PCM 데이터 작성
  floatTo16BitPCM(view, 44, pcmData);

  return new Blob([buffer], { type: 'audio/wav' });
}

function writeString(view: DataView, offset: number, string: string): void {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i));
  }
}

function floatTo16BitPCM(view: DataView, offset: number, input: Float32Array): void {
  for (let i = 0; i < input.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, input[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }
}
