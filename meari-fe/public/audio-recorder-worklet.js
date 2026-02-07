/**
 * AudioWorkletProcessor for real-time PCM audio recording
 * This worklet captures raw PCM audio data from the microphone
 */
class AudioRecorderWorklet extends AudioWorkletProcessor {
  constructor() {
    super();
    this.isRecording = false;
    this.buffers = [];

    // Listen for messages from the main thread
    this.port.onmessage = (event) => {
      if (event.data.command === 'start') {
        this.isRecording = true;
        this.buffers = [];
      } else if (event.data.command === 'stop') {
        this.isRecording = false;
        // Send all collected buffers back to main thread
        this.port.postMessage({
          eventType: 'recordingComplete',
          buffers: this.buffers,
        });
        this.buffers = [];
      }
    };
  }

  process(inputs, outputs, parameters) {
    // Get the first input (microphone)
    const input = inputs[0];

    if (this.isRecording && input && input.length > 0) {
      // Get the first channel (mono recording)
      const channelData = input[0];

      if (channelData) {
        // Copy the audio data (Float32Array)
        const buffer = new Float32Array(channelData.length);
        buffer.set(channelData);
        this.buffers.push(buffer);
      }
    }

    // Return true to keep the processor alive
    return true;
  }
}

// Register the processor
registerProcessor('audio-recorder-worklet', AudioRecorderWorklet);
