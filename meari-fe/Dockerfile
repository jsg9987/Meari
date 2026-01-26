# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# 의존성 설치 (캐싱 최적화)
COPY package*.json ./
RUN npm ci --only=production

# 소스 코드 복사 및 빌드
COPY . .
RUN npm run build

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