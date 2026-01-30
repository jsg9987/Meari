# 스크립트 저장 파이프라인 (Video Saving Pipeline)

CSV 파일로 업로드된 스크립트를 파싱하고, 형태소 분석 및 단어 매칭을 수행하여 DB에 저장하는 파이프라인입니다.

---

## 패키지 구조

```
pipeline/videosaving/
├── dto/
│   └── HomonymWordDto.java          # 동음이의어 정보 DTO
├── homonym/service/
│   └── HomonymDisambiguationService.java  # 동음이의어 처리 (OpenAI)
├── nlp/
│   ├── controller/
│   │   └── NlpController.java       # NLP API 엔드포인트
│   ├── dto/
│   │   └── MorphemeAnalysisResponseDto.java  # 형태소 분석 결과 DTO
│   └── service/
│       ├── NlpService.java          # NLP 서비스 인터페이스
│       └── NlpServiceImpl.java      # KOMORAN 기반 구현체
├── openai/
│   ├── dto/
│   │   ├── OpenAiRequestDto.java    # OpenAI 요청 DTO
│   │   └── OpenAiResponseDto.java   # OpenAI 응답 DTO
│   └── service/
│       └── OpenAiService.java       # OpenAI API 클라이언트
├── service/
│   └── ScriptSavingService.java     # 메인 파이프라인 서비스
└── README.md
```

---

## 전체 흐름

```
CSV 업로드
    │
    ▼
┌─────────────────────────────────────┐
│  1. CSV 파싱 (CsvScriptParser)      │
│  • BOM 제거, 헤더 검증              │
│  • ScriptCsvRowDto 리스트 생성      │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  2. 문장 저장                        │
│  • Content, Role 조회               │
│  • Sentence 엔티티 생성 & DB 저장   │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  3. 형태소 분석 (KOMORAN)           │
│  • 품사 필터링 (명사, 동사, 형용사) │
│  • 불용어 제거                      │
│  • 동사/형용사 사전형 변환          │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  4. 단어 매칭                        │
│  • Word 테이블 조회                 │
│  • 1개: SentenceWord 즉시 연결      │
│  • 2개+: 동음이의어 리스트에 보관   │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  5. 동음이의어 처리 (OpenAI)        │
│  • 전체 스크립트 맥락으로 판단      │
│  • 1회 API 호출로 일괄 처리         │
│  • 선택된 Word로 SentenceWord 연결  │
└─────────────────┬───────────────────┘
                  │
                  ▼
              완료 응답
```

---

## 상세 설명

### 1단계: CSV 파싱

**담당:** `CsvScriptParser`

**CSV 포맷:**
```csv
content_id,sequence,role_id,start_time,end_time,text_ko,text_vn
1,1,2,0.0,3.5,안녕하세요,Xin chào
1,2,3,3.5,6.0,반갑습니다,Rất vui được gặp bạn
```

**처리 내용:**
- UTF-8 BOM 자동 제거
- 필수 헤더 검증
- 시간 포맷 파싱 (`MM:SS.ms` 또는 숫자)

---

### 2단계: 문장 저장

**담당:** `ScriptSavingService.saveSentencesWithMorphemeAnalysis()`

**엔티티 생성:**
```java
Sentence sentence = Sentence.builder()
    .content(content)      // Content 엔티티
    .role(role)            // Role 엔티티 (화자)
    .sequence(sequence)    // 문장 순서
    .startTime(startTime)  // 시작 시간 (초)
    .endTime(endTime)      // 종료 시간 (초)
    .textKo(textKo)        // 한국어 대사
    .textVn(textVn)        // 베트남어 번역
    .build();
```

---

### 3단계: 형태소 분석

**담당:** `NlpServiceImpl` (KOMORAN 라이브러리)

**분석 대상 품사:**
| 품사 코드 | 설명 | 예시 |
|----------|------|------|
| NNG | 일반명사 | 학교, 친구 |
| NNP | 고유명사 | 서울, 김철수 |
| VV | 동사 | 먹다, 가다 |
| VA | 형용사 | 예쁘다, 크다 |
| MM | 관형사 | 새, 헌 |

**불용어 (제외):**
```
하다, 되다, 있다, 없다, 같다, 보다, 가다, 오다, 주다, 받다, ...
좋다, 나쁘다, 크다, 작다, 많다, 적다, ...
나, 너, 우리, 것, 거, ...
```

**사전형 변환:**
- `먹` (VV) → `먹다`
- `예쁘` (VA) → `예쁘다`

---

### 4단계: 단어 매칭

**담당:** `ScriptSavingService.linkWordsToSentence()`

**로직:**
```
형태소 "배" 추출
    │
    ▼
Word 테이블에서 "배" 조회
    │
    ├─ 0개 → 스킵 (학습 대상 단어 아님)
    │
    ├─ 1개 → SentenceWord 즉시 생성
    │
    └─ 2개+ → HomonymWordDto로 보관 (동음이의어)
```

---

### 5단계: 동음이의어 처리

**담당:** `HomonymDisambiguationService` + `OpenAiService`

**OpenAI 프롬프트 구조:**
```
## 전체 스크립트
[1] 오늘 배가 아파요
[2] 배를 먹으면 나을 거예요

## 동음이의어 목록
### 1. '배'
등장 위치: [1] 오늘 배가 아파요
후보:
  1. 과일의 일종
  2. 사람의 신체 부위
  3. 물 위에 뜨는 탈것

### 2. '배'
등장 위치: [2] 배를 먹으면 나을 거예요
후보:
  1. 과일의 일종
  2. 사람의 신체 부위
  3. 물 위에 뜨는 탈것
```

**OpenAI 응답:**
```json
[2, 1]
```
- 문장 1의 '배' → 2번 (신체 부위)
- 문장 2의 '배' → 1번 (과일)

---

## API 엔드포인트

### 스크립트 업로드
```
POST /api/v1/admin/scripts/upload
Content-Type: multipart/form-data

file: (CSV 파일)
```

**응답:**
```json
{
  "success": true,
  "data": 25,  // 저장된 문장 수
  "error": null
}
```

### NLP 테스트 API
```
GET /api/v1/nlp/morpheme-analysis?text=안녕하세요
GET /api/v1/nlp/extract-nouns?text=서울에서 친구를 만났습니다
```

---

## 설정

### application.yml
```yaml
openai:
  api-url: https://api.openai.com/v1/chat/completions
  api-key: ${OPENAI_API_KEY}
  model: gpt-4o-mini
```

---

## 관련 테이블

### Sentence
| 컬럼 | 타입 | 설명 |
|------|------|------|
| sentence_id | BIGINT | PK |
| content_id | BIGINT | FK → Content |
| role_id | BIGINT | FK → Role |
| sequence | INT | 문장 순서 |
| start_time | DECIMAL | 시작 시간 (초) |
| end_time | DECIMAL | 종료 시간 (초) |
| text_ko | VARCHAR | 한국어 대사 |
| text_vn | VARCHAR | 베트남어 번역 |

### SentenceWord
| 컬럼 | 타입 | 설명 |
|------|------|------|
| sentence_word_id | BIGINT | PK |
| sentence_id | BIGINT | FK → Sentence |
| word_id | BIGINT | FK → Word |
| sequence | INT | 단어 순서 |

### Word
| 컬럼 | 타입 | 설명 |
|------|------|------|
| word_id | BIGINT | PK |
| word_kr | VARCHAR | 한국어 단어 |
| definition_kr | VARCHAR | 한국어 뜻 |
| word_vn | VARCHAR | 베트남어 단어 |
