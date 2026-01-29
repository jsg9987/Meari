import axiosInstance from './axiosInstance';

// ===== 타입 정의 (명세 반영) =====

export interface KopicSentence {
    kopic_sentence_id: number;
    text_ko: string;
    kopic_sentence_url: string; // 이미지 또는 상황 URL
    // 프론트엔드 전용 필드 (명세에는 없지만 UI 구성을 위해 필요)
    time_limit?: number;
}

export interface KopicDetailedAnalysis {
    missed_point: string;
    correction: string;
    tip: string;
}

export interface KopicAnalyzeResponse {
    kopic_report_id: number;
    status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
}

// 리포트 아이템 (프론트엔드 표시용 확장 타입)
export interface KopicReportItem extends KopicAnalyzeResponse {
    kopic_sentence_id: number;
    text_ko: string;
    user_answer: string;
    audio_url: string;
    accuracy: number;
    intonation: number;
    detailed_analysis: KopicDetailedAnalysis | null;
}

export interface GetKopicSentencesResponse {
    success: boolean;
    data: KopicSentence[];
    error: { code: string; message: string } | null;
}

export interface KopicAnalyzeRequest {
    kopic_sentence_id: number;
    audio_url: string;
}

export interface KopicAnalyzeApiResponse {
    success: boolean;
    data: KopicAnalyzeResponse;
    error: { code: string; message: string } | null;
}

// ===== Mock 데이터 =====

const mockSentences: Record<number, KopicSentence[]> = {
    1: [
        {
            kopic_sentence_id: 10,
            text_ko: "카페 주문 상황 - 안녕하세요. 아이스 아메리카노 한 잔이랑 치즈케이크 하나 주세요.",
            kopic_sentence_url: "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600",
            time_limit: 60
        },
        {
            kopic_sentence_id: 11,
            text_ko: "영화관 예매 상황 - 오늘 저녁 7시 영화 두 장 예매하고 싶은데 가운데 자리로 가능할까요?",
            kopic_sentence_url: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600",
            time_limit: 60
        },
        {
            kopic_sentence_id: 12,
            text_ko: "병원 접수 상황 - 감기 증상이 있어서 오늘 진료를 받고 싶은데 접수 가능한가요?",
            kopic_sentence_url: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600",
            time_limit: 60
        },
        {
            kopic_sentence_id: 13,
            text_ko: "식당 예약 상황 - 오늘 오후 6시에 두 명 예약 가능한지 확인 부탁드립니다.",
            kopic_sentence_url: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600",
            time_limit: 60
        },
        {
            kopic_sentence_id: 14,
            text_ko: "길 안내 요청 상황 - 이 근처에 지하철역이 어디 있는지 알려주실 수 있나요?",
            kopic_sentence_url: "https://images.unsplash.com/photo-1601297183305-6df142704ea2?w=600",
            time_limit: 60
        }
    ]
};

// Mock 리포트 결과 생성 (폴링 시뮬레이션용)
const generateMockResult = (sentenceId: number, textKo: string): KopicReportItem => {
    return {
        kopic_report_id: sentenceId + 1000,
        status: 'COMPLETED',
        kopic_sentence_id: sentenceId,
        text_ko: textKo,
        user_answer: textKo + " (사용자 답변 예시)",
        audio_url: `https://example.com/audio_${sentenceId}.wav`,
        accuracy: 85 + Math.floor(Math.random() * 10),
        intonation: 80 + Math.floor(Math.random() * 10),
        detailed_analysis: {
            missed_point: "억양이 조금 평조입니다.",
            correction: textKo,
            tip: "문장 끝을 조금 더 올려보세요."
        }
    };
};

// ===== Mock API 함수 =====

export const getKopicSentencesMock = async (themeId: number): Promise<GetKopicSentencesResponse> => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                success: true,
                data: mockSentences[themeId] || mockSentences[1], // 기본값 제공
                error: null,
            });
        }, 500);
    });
};

export const submitKopicAnswerMock = async (
    sentenceId: number,
    _audioBlob: Blob,
    textKo: string
): Promise<KopicReportItem> => {
    // 실제 명세는 202 Accepted + PROCESSING 이지만, 
    // Mock에서는 프론트 구현 편의를 위해(순차적 결과를 보여주기 위해)
    // 딜레이 후 분석 완료된 데이터를 반환하도록 시뮬레이션합니다.

    const delay = 2000 + Math.random() * 2000; // 2~4초 딜레이

    return new Promise((resolve) => {
        setTimeout(() => {
            resolve(generateMockResult(sentenceId, textKo));
        }, delay);
    });
};

// ===== 실제 API 함수 =====

export const getKopicSentences = async (themeId: number) => {
    const useMock = import.meta.env.VITE_USE_MOCK_CONTENTS === 'true';
    if (useMock) {
        return { data: await getKopicSentencesMock(themeId) };
    }
    // URL: GET /api/v1/contents?theme_id={id}
    const response = await axiosInstance.get<GetKopicSentencesResponse>(`/api/v1/contents`, {
        params: { theme_id: themeId }
    });
    return response;
};

export const submitKopicAnswer = async (
    sentenceId: number,
    audioBlob: Blob,
    textKo: string
): Promise<KopicReportItem> => {
    const useMock = import.meta.env.VITE_USE_MOCK_CONTENTS === 'true';
    if (useMock) {
        return await submitKopicAnswerMock(sentenceId, audioBlob, textKo);
    }

    // TODO: 실제 환경에서는 먼저 오디오 파일을 업로드하여 audio_url을 확보해야 합니다.
    // 현재는 S3 업로드 로직이 부재하므로, 가상의 URL을 사용하거나 
    // 백엔드가 Multipart를 지원하도록 협의가 필요할 수 있습니다.
    // 여기서는 명세에 따라 JSON에 audio_url을 실어 보냅니다.

    // 1. (가상) 파일 업로드 로직 위치
    // const audioUrl = await uploadFile(audioBlob);
    const fakeAudioUrl = `https://s3-bucket.com/uploads/kopic/${sentenceId}_${Date.now()}.wav`;

    // 2. 분석 요청
    try {
        const response = await axiosInstance.post<KopicAnalyzeApiResponse>(`/api/v1/kopic/report`, {
            kopic_sentence_id: sentenceId,
            audio_url: fakeAudioUrl
        });

        if (response.data.success) {
            // 명세상으로는 status: PROCESSING 만 옴.
            // 실제로는 여기서 폴링 로직이 필요하거나, 리포트 페이지에서 조회를 시도해야 함.
            // 프론트엔드 비동기 처리를 유지하기 위해 임시로 완료 데이터를 구성해 리턴 (Mocking 성격)

            // 주의: 실제 연동 시에는 이 부분이 'Processing' 상태를 리턴하고, 
            // 리포트 페이지에서 해당 ID로 결과를 주기적으로 조회하는 로직으로 변경되어야 할 수 있습니다.
            return generateMockResult(sentenceId, textKo);
        } else {
            throw new Error(response.data.error?.message || 'Analysis failed');
        }
    } catch (error) {
        console.error('Kopic analysis error:', error);
        // 에러 발생 시 더미 데이터 리턴 (흐름 끊김 방지)
        return {
            kopic_report_id: 0,
            status: 'FAILED',
            kopic_sentence_id: sentenceId,
            text_ko: textKo,
            user_answer: '분석 요청 실패',
            audio_url: '',
            accuracy: 0,
            intonation: 0,
            detailed_analysis: null
        };
    }
};
