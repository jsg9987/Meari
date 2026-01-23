# 1. 프로젝트 개요 및 핵심 기능 정의서
- 서비스 명칭 및 목표: 4인 준실시간 한국어 쉐도잉 및 AI 발음 평가 서비스(화상 카메라, 음성만 실시간이고 유저별 문장 녹음은 동영상 자막 + 역할별 타임스탬프 시간에 따라 브라우저에서 녹음해 업로드)
- 주요 기능 리스트 및 API 명세서:




# 2. 상세 기술 스택(Tech Stack)
- Backend: Java 21, Spring Boot 3.5.9, Spring Data JPA, Spring Security(JWT + Redis), WebSocket(STOMP), WebRTC(COTURN, Kurento(어렵지 않고 유용하다면 추가))
- Frontend: React, TailwindCSS, Zustand
- AI 서버(gpu ec2): FastAPI, Wav2Vec 2.0 기반 음소 분석 모델 + 기능들
- DB: PostgreSQL(+ JSONB 활용), Redis
- Messaging: RabbitMQ(Spring Boot <-> FastAPI 비동기 통신) -> 고민해보고 직접 내장할 것인지 결정 필요
- Infrastructure: AWS(EC2, S3, CloudFront), Docker, Nginx, Jenkins, GitLab

# 3. DB 설계 및 전략
- JSONB: 분석 결과를 JSONB 타입에 문장별 분석 정보를 통째로 저장하려고 고려 중
- Redis: 실시간 상태 데이터나 실시간 쉐도잉 세션(방정보, 라운드1, 실시간 정보....)를 저장해서 RDBMS가 아닌 Redis에서 빠르게빠르게 처리
- 파일 저장 전략: 모든 미디어 파일을 S3에 저장하려함, 보안을 위해 Presigned URL을 사용한다. => 모두 S3에 저장하는 것이 좋을지 고민 필요

# 4. 인프라 및 통신 아키텍쳐 설명