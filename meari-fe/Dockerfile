# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# pnpm 설치
RUN npm install -g pnpm

# 의존성 설치 (캐싱 최적화)
COPY package*.json ./
RUN pnpm install

# 소스 코드 복사
COPY . .

# 👇 빌드 시 환경변수 주입
ARG VITE_BASE_SERVER_URL=http://localhost:8080/api/v1
ARG VITE_USE_MOCK_API=false

ENV VITE_BASE_SERVER_URL=${VITE_BASE_SERVER_URL}
ENV VITE_USE_MOCK_API=${VITE_USE_MOCK_API}

# 빌드 실행
RUN pnpm run build

# Production stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# 기본 nginx 설정 제거
RUN rm -rf /etc/nginx/conf.d/default.conf

# 커스텀 nginx 설정 복사
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf

# 빌드된 파일 복사
COPY --from=build /app/dist /usr/share/nginx/html

# 80 포트 노출
EXPOSE 80

# nginx 실행
CMD ["nginx", "-g", "daemon off;"]