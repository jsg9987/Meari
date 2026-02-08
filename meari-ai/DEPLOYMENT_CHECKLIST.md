# 억양 분석 배포 체크리스트

## 📦 Dependencies 점검

### ✅ requirements.txt 확인

```txt
fastapi==0.115.5
uvicorn[standard]==0.32.1
pika==1.3.2
torch==2.5.1
torchaudio==2.5.1
transformers==4.46.3
datasets==3.1.0
aioboto3[boto3]==13.2.0
requests==2.32.3
python-dotenv==1.0.1
soundfile==0.12.1
librosa==0.10.1
fastdtw==0.3.4
ffmpeg-python==0.2.0
praat-parselmouth==0.4.3  ← 억양 분석 추가
scipy>=1.7.0              ← 억양 분석 추가
```

---

## 🚨 배포 환경 필수 체크사항

### 1. Parselmouth 설치 확인 ⚠️ 중요!

**위험도**: 🔴 높음

**문제**: Parselmouth는 C++ 확장 모듈이므로 컴파일 환경 필요

**확인 방법**:
```bash
pip install praat-parselmouth==0.4.3
python -c "import parselmouth; print('OK')"
```

**실패 시 해결**:

#### Ubuntu/Debian
```bash
# 빌드 도구 설치
sudo apt-get update
sudo apt-get install -y build-essential python3-dev

# 다시 설치
pip install praat-parselmouth==0.4.3
```

#### CentOS/RHEL
```bash
sudo yum groupinstall -y "Development Tools"
sudo yum install -y python3-devel

pip install praat-parselmouth==0.4.3
```

#### Docker 환경
```dockerfile
FROM python:3.10

# 빌드 도구 추가
RUN apt-get update && \
    apt-get install -y build-essential && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
```

**성공 확인**:
```python
import parselmouth
print(parselmouth.__version__)  # 0.4.3
```

---

### 2. scipy 설치 확인

**위험도**: 🟡 중간

**확인**:
```bash
python -c "import scipy; print(scipy.__version__)"
```

scipy는 보통 문제 없이 설치됨 (numpy 의존성만 있으면 OK)

---

### 3. ffmpeg 시스템 설치 확인

**위험도**: 🟡 중간

**확인**:
```bash
ffmpeg -version
```

**설치 안되어 있으면**:

#### Ubuntu/Debian
```bash
sudo apt-get update
sudo apt-get install -y ffmpeg
```

#### CentOS/RHEL
```bash
sudo yum install -y epel-release
sudo yum install -y ffmpeg
```

#### Docker
```dockerfile
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*
```

---

### 4. torch/torchaudio CPU 버전 확인

**위험도**: 🟢 낮음

**현재 설정**: `torch==2.5.1` (CPU + CUDA)

**배포 서버가 CPU only면**:
```bash
# requirements.txt 수정 (선택적)
torch==2.5.1+cpu
torchaudio==2.5.1+cpu
--extra-index-url https://download.pytorch.org/whl/cpu
```

일반적으로 기본 버전 사용해도 문제 없음 (용량만 큼)

---

## 🧪 배포 전 테스트

### 1. 빠른 통합 테스트

```bash
cd meari-ai

# 파이썬 버전 확인 (3.10+ 권장)
python --version

# 의존성 설치
pip install -r requirements.txt

# Parselmouth 임포트 테스트
python -c "import parselmouth; print('✅ Parselmouth OK')"

# scipy 임포트 테스트
python -c "from scipy.interpolate import interp1d; print('✅ scipy OK')"

# 억양 분석 테스트
python test_parselmouth_integration.py
```

**예상 결과**:
```
✅ Parselmouth OK
✅ scipy OK

🎯 Parselmouth 통합 테스트
📂 테스트 파일:
   정답: app/data/257.wav
   사용자: app/data/257_mine.wav

점수: 94
피드백: 억양이 매우 자연스럽습니다!

✅ 테스트 완료
```

---

### 2. API 서버 테스트

```bash
# FastAPI 서버 시작
cd meari-ai
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000

# 로그 확인
tail -f logs/app.log | grep "Intonation"
```

**확인 사항**:
- 서버 시작 성공
- 억양 분석 요청 시 에러 없음
- 로그에 "Intonation Score Calculation" 출력

---

## 🔍 배포 후 모니터링

### 1. 로그 확인

```bash
# 억양 분석 로그
grep "Intonation" app.log

# Parselmouth 에러 확인
grep "Parselmouth" app.log

# scipy 에러 확인
grep "scipy" app.log
```

### 2. 성능 지표

**정상 범위**:
- 억양 분석 시간: 1-3초 (문장당)
- 메모리 사용: +100MB (Parselmouth 로딩)
- CPU 사용: 중간 (pitch 추출 시)

### 3. 에러 패턴 모니터링

**자주 발생할 수 있는 에러**:

#### 에러 1: `ModuleNotFoundError: No module named 'parselmouth'`
→ Parselmouth 설치 안됨 → 빌드 도구 설치 후 재설치

#### 에러 2: `ImportError: cannot import name 'interp1d'`
→ scipy 설치 안됨 → `pip install scipy>=1.7.0`

#### 에러 3: `score = -1` 비율 높음
→ 오디오 품질 낮음 → 클라이언트에 녹음 가이드 제공

---

## 📊 배포 시나리오별 체크리스트

### Scenario A: 로컬 개발 환경

- [x] Python 3.10+
- [x] requirements.txt 설치
- [x] ffmpeg 설치 (선택)
- [x] test_parselmouth_integration.py 실행

---

### Scenario B: Ubuntu 서버 (EC2, GCP, etc.)

- [ ] Python 3.10+ 설치
- [ ] `build-essential` 설치 (Parselmouth용)
- [ ] `ffmpeg` 설치
- [ ] requirements.txt 설치
- [ ] Parselmouth 임포트 테스트
- [ ] FastAPI 서버 시작
- [ ] 억양 분석 API 테스트

**명령어 순서**:
```bash
# 1. 시스템 의존성
sudo apt-get update
sudo apt-get install -y python3.10 python3-pip build-essential ffmpeg

# 2. Python 의존성
cd meari-ai
pip install -r requirements.txt

# 3. 테스트
python -c "import parselmouth; print('OK')"
python test_parselmouth_integration.py

# 4. 서버 시작
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

---

### Scenario C: Docker 배포

**Dockerfile 예시**:

```dockerfile
FROM python:3.10-slim

# 시스템 의존성
RUN apt-get update && \
    apt-get install -y \
        build-essential \
        ffmpeg \
        && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Python 의존성
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 앱 복사
COPY . .

# 포트 노출
EXPOSE 8000

# 서버 시작
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**빌드 및 실행**:
```bash
# 빌드
docker build -t meari-ai:latest .

# 실행
docker run -d \
  -p 8000:8000 \
  -e AWS_ACCESS_KEY=xxx \
  -e AWS_SECRET_KEY=xxx \
  meari-ai:latest

# 로그 확인
docker logs -f <container_id>

# Parselmouth 테스트
docker exec <container_id> python -c "import parselmouth; print('OK')"
```

---

### Scenario D: Kubernetes 배포

**주의사항**:
- Parselmouth는 컴파일 필요 → 이미지 빌드 시 포함
- ffmpeg도 이미지에 포함
- CPU 리소스 충분히 할당 (pitch 추출)

**리소스 권장**:
```yaml
resources:
  requests:
    cpu: 1000m      # 1 core
    memory: 2Gi
  limits:
    cpu: 2000m      # 2 cores
    memory: 4Gi
```

---

## 🐛 트러블슈팅

### 문제 1: Parselmouth 설치 실패

**증상**:
```
ERROR: Could not build wheels for praat-parselmouth
```

**해결**:
1. 빌드 도구 설치
   ```bash
   sudo apt-get install -y build-essential python3-dev
   ```

2. pip 업그레이드
   ```bash
   pip install --upgrade pip setuptools wheel
   ```

3. 다시 설치
   ```bash
   pip install praat-parselmouth==0.4.3
   ```

---

### 문제 2: ffmpeg not found

**증상**:
```
FileNotFoundError: [Errno 2] No such file or directory: 'ffmpeg'
```

**해결**:
```bash
# Ubuntu
sudo apt-get install -y ffmpeg

# CentOS
sudo yum install -y epel-release
sudo yum install -y ffmpeg

# 확인
ffmpeg -version
```

---

### 문제 3: 억양 점수 항상 -1

**증상**: 모든 오디오에서 `score = -1` 반환

**원인**:
- 오디오 품질 실제로 낮음
- Parselmouth pitch 추출 실패
- 샘플 레이트 불일치

**디버깅**:
```python
# 로그 확인
logger.setLevel(logging.DEBUG)

# Pitch 추출 테스트
python debug_pitch.py
```

**해결**:
- 오디오 품질 향상 (클라이언트)
- 샘플 레이트 16kHz로 통일 확인
- Parselmouth 설치 재확인

---

## ✅ 최종 체크리스트

배포 전에 모두 체크:

### 필수 (🔴)
- [ ] Parselmouth 설치 확인
- [ ] scipy 설치 확인
- [ ] ffmpeg 설치 확인 (선택적이지만 권장)
- [ ] test_parselmouth_integration.py 성공
- [ ] API 서버 시작 성공
- [ ] 억양 분석 API 응답 정상

### 권장 (🟡)
- [ ] 로그 모니터링 설정
- [ ] 성능 지표 수집
- [ ] 에러 알림 설정
- [ ] 프론트엔드에 가이드 전달 (FRONTEND_INTONATION_GUIDE.md)

### 선택 (🟢)
- [ ] Docker 이미지 빌드 테스트
- [ ] Kubernetes 리소스 설정
- [ ] 부하 테스트 (억양 분석 동시 요청)

---

## 📞 배포 이슈 발생 시

1. 로그 확인: `tail -f app.log | grep ERROR`
2. Parselmouth 확인: `python -c "import parselmouth"`
3. 테스트 실행: `python test_parselmouth_integration.py`
4. 이슈 트래킹: 에러 메시지와 함께 보고

---

## 📝 변경 이력

- 2026-02-09: 억양 분석 기능 추가 (Parselmouth 통합)
- requirements.txt: `praat-parselmouth==0.4.3`, `scipy>=1.7.0` 추가
- API 응답 구조 개선 (프론트엔드 친화적)
