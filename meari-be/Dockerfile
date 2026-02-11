# Build stage
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

# Gradle 캐싱을 위한 의존성 먼저 다운로드
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 보안을 위한 non-root 유저 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 환경변수 ARG로 받기 (빌드 타임)
ARG DB_PASSWORD
ARG JWT_SECRET_KEY
ARG REDIS_PASSWORD
ARG FRONTEND_URL
ARG OPENVIDU_URL
ARG OPENVIDU_SECRET
ARG AWS_ACCESS_KEY
ARG AWS_SECRET_KEY
ARG AWS_S3_BUCKET
ARG GEMINI_API_KEY

# 환경변수로 설정 (런타임)
ENV DB_PASSWORD=${DB_PASSWORD}
ENV JWT_SECRET_KEY=${JWT_SECRET_KEY}
ENV REDIS_PASSWORD=${REDIS_PASSWORD}
ENV FRONTEND_URL=${FRONTEND_URL}
ENV OPENVIDU_URL=${OPENVIDU_URL}
ENV OPENVIDU_SECRET=${OPENVIDU_SECRET}
ENV AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
ENV AWS_SECRET_KEY=${AWS_SECRET_KEY}
ENV AWS_S3_BUCKET=${AWS_S3_BUCKET}
ENV GEMINI_API_KEY=${GEMINI_API_KEY}

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]