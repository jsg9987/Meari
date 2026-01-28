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

# 👇 환경변수 선언 (기본값 포함)
ENV DB_PASSWORD=${DB_PASSWORD:-ssafy}
ENV JWT_SECRET_KEY=${JWT_SECRET_KEY:-default_secret}
ENV REDIS_PASSWORD=${REDIS_PASSWORD:-}
ENV FRONTEND_URL=${FRONTEND_URL:-http://localhost:5173}
ENV OPENVIDU_URL=${OPENVIDU_URL:-http://localhost:4443}
ENV OPENVIDU_SECRET=${OPENVIDU_SECRET:-MY_SECRET}

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# 👇 환경변수를 Spring Boot에 전달
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.datasource.password=${DB_PASSWORD}", \
    "-Djwt.secret-key=${JWT_SECRET_KEY}", \
    "-Dspring.data.redis.password=${REDIS_PASSWORD}", \
    "-Dfrontend.url=${FRONTEND_URL}", \
    "-Dopenvidu.url=${OPENVIDU_URL}", \
    "-Dopenvidu.secret=${OPENVIDU_SECRET}", \
    "-jar", \
    "app.jar"]