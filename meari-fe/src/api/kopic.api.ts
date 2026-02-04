import axiosInstance from './axiosInstance';

// ===== 타입 정의 =====
export interface KopicSentence {
    kopic_sentence_id: number;
    text_ko: string;
    kopic_sentence_url: string;
    time_limit?: number;
}

export interface KopicDetailedAnalysis {
    feedback: {
        missed_point: string;
        correction: string;
        tip: string;
    };
    original_sentence: string;
    target_sentence: string;
}

export interface KopicAnalyzeResponse {
    kopic_report_id: number;
    status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
    kopic_total_report_id?: number;
}

export interface KopicReportItem {
    kopic_report_id: number;
    status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
    kopic_sentence_id: number;
    text_ko: string;
    accuracy?: number | null;
    total_score?: number | null;
    detailed_analysis?: KopicDetailedAnalysis | null;
    user_answer?: string;
    audio_url?: string;
}

export interface GetKopicSentencesResponse {
    success: boolean;
    data: KopicSentence[];
    error: { code: string; message: string } | null;
}

export interface KopicAnalyzeRequest {
    kopic_total_report_id: number;
    kopic_sentence_id: number;
}

export interface KopicAnalyzeApiResponse {
    success: boolean;
    data: KopicAnalyzeResponse;
    error: { code: string; message: string } | null;
}

export interface KopicTotalReportItem {
    kopic_report_id: number;
    kopic_sentence_id: number;
    text_ko: string;
    accuracy: number;
    intonation: number;
    total_score: number;
    detailed_analysis: KopicDetailedAnalysis | null;
}

export interface KopicTotalReportResponse {
    success: boolean;
    data: {
        kopic_total_report_id: number;
        status: 'PROCESSING' | 'COMPLETED';
        avg_accuracy?: number | null;
        total_score?: number | null;
        sentence_count?: number | null;
        completed_count?: number | null;
        total_count?: number | null;
        report_data?: KopicTotalReportItem[] | null;
    };
    error: { code: string; message: string } | null;
}

export interface KopicTotalReportCreateResponse {
    success: boolean;
    data: { kopic_total_report_id: number };
    error: { code: string; message: string } | null;
}

// ===== Mock 데이터 =====
const mockSentences: Record<number, KopicSentence[]> = {
    1: [
        {
            kopic_sentence_id: 10,
            text_ko: '카페 주문 상황 - 안녕하세요. 아이스 아메리카노 한 잔이랑 치즈케이크 하나 주세요.',
            kopic_sentence_url: 'https://samplelib.com/lib/preview/mp3/sample-3s.mp3',
            time_limit: 60
        },
        {
            kopic_sentence_id: 11,
            text_ko: '영화관 예매 상황 - 오늘 저녁 7시 영화 두 장 예매하고 싶은데 가운데 자리로 가능할까요?',
            kopic_sentence_url: 'https://samplelib.com/lib/preview/mp3/sample-6s.mp3',
            time_limit: 60
        },
        {
            kopic_sentence_id: 12,
            text_ko: '병원 접수 상황 - 감기 증상이 있어서 오늘 진료를 받고 싶은데 접수 가능한가요?',
            kopic_sentence_url: 'https://samplelib.com/lib/preview/mp3/sample-9s.mp3',
            time_limit: 60
        },
        {
            kopic_sentence_id: 13,
            text_ko: '식당 예약 상황 - 오늘 오후 6시에 두 명 예약 가능한지 확인 부탁드립니다.',
            kopic_sentence_url: 'https://samplelib.com/lib/preview/mp3/sample-12s.mp3',
            time_limit: 60
        },
        {
            kopic_sentence_id: 14,
            text_ko: '길 안내 요청 상황 - 이 근처에 지하철역이 어디 있는지 알려주실 수 있나요?',
            kopic_sentence_url: 'https://samplelib.com/lib/preview/mp3/sample-15s.mp3',
            time_limit: 60
        }
    ]
};

// Mock 리포트 결과 생성 (테스트용)
const generateMockResult = (sentenceId: number, textKo: string): KopicReportItem => {
    return {
        kopic_report_id: sentenceId + 1000,
        status: 'COMPLETED',
        kopic_sentence_id: sentenceId,
        text_ko: textKo,
        user_answer: `${textKo} (모의 발화)`,
        audio_url: `https://example.com/audio_${sentenceId}.wav`,
        accuracy: 85 + Math.floor(Math.random() * 10),
        total_score: 80 + Math.floor(Math.random() * 10),
        detailed_analysis: {
            feedback: {
                missed_point: '핵심 정보가 부족합니다.',
                correction: '구체적인 정보를 포함해 답해보세요.',
                tip: '상황에 맞는 인사나 부탁 표현을 추가하면 자연스럽습니다.'
            },
            original_sentence: `${textKo} (모의 발화)`,
            target_sentence: textKo
        }
    };
};

// ===== Mock API 함수 =====
export const getKopicSentencesMock = async (themeId: number): Promise<GetKopicSentencesResponse> => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                success: true,
                data: mockSentences[themeId] || mockSentences[1],
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
    const delay = 2000 + Math.random() * 2000;
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
    const response = await axiosInstance.get<GetKopicSentencesResponse>(`/contents`, {
        params: { theme_id: themeId }
    });
    return response;
};

export const submitKopicAnswer = async (
    kopicTotalReportId: number,
    sentenceId: number,
    audioBlob: Blob,
    textKo: string
): Promise<KopicAnalyzeResponse> => {
    const useMock = import.meta.env.VITE_USE_MOCK_CONTENTS === 'true';
    if (useMock) {
        const mock = await submitKopicAnswerMock(sentenceId, audioBlob, textKo);
        return {
            kopic_report_id: mock.kopic_report_id,
            status: 'COMPLETED',
        };
    }

    // multipart/form-data (request JSON + audio file)
    const requestData: KopicAnalyzeRequest = {
        kopic_total_report_id: kopicTotalReportId,
        kopic_sentence_id: sentenceId
    };

    const formData = new FormData();
    const requestBlob = new Blob([JSON.stringify(requestData)], { type: 'application/json' });
    formData.append('request', requestBlob);
    formData.append('audio', audioBlob, `kopic_${sentenceId}.wav`);

    const response = await axiosInstance.post<KopicAnalyzeApiResponse>('/kopic/evaluate', formData, {
        headers: { 'Content-Type': undefined }
    });

    if (!response.data.success) {
        throw new Error(response.data.error?.message || 'Analysis failed');
    }

    return response.data.data;
};

// ===== total-report 생성 =====
export const createKopicTotalReport = async (themeId: number) => {
    // POST /api/v1/kopic/total-report
    const response = await axiosInstance.post<KopicTotalReportCreateResponse>(`/kopic/total-report`, {
        theme_id: themeId,
    });
    return response;
};

// ===== total-report 조회(진행률/최종 결과) =====
export const getKopicTotalReport = async (totalReportId: number) => {
    const response = await axiosInstance.get<KopicTotalReportResponse>(`/kopic/total-report/${totalReportId}`);
    return response;
};
