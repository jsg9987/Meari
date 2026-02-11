pipeline {
    agent any

    triggers {
        pollSCM('H/5 * * * *')  // 5분마다 Git 변경사항 체크 (CPU 부하 감소)
    }

    parameters {
        booleanParam(
            name: 'NO_CACHE_BUILD',
            defaultValue: false,
            description: 'Force docker build --no-cache (use ONLY once after dockerignore change)'
        )
    }

    stages {
        // 코드 내려받기
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // 빌드 단계: 백엔드, 프론트엔드, FastAPI 순차 빌드 (CPU 부하 감소)
        // --- Spring Boot 백엔드 빌드 ---
        stage('Backend Build') {
            steps {
                dir('meari-be') {
                    script {
                        def isReleaseBranch = env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'
                        if (isReleaseBranch) {
                            withCredentials([
                                string(credentialsId: 'DB_PASSWORD', variable: 'DB_PW'),
                                string(credentialsId: 'JWT_SECRET_KEY', variable: 'JWT_KEY'),
                                string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PW'),
                                string(credentialsId: 'FRONTEND_URL', variable: 'FE_URL'),
                                string(credentialsId: 'OPENVIDU_URL', variable: 'OV_URL'),
                                string(credentialsId: 'OPENVIDU_SECRET', variable: 'OV_SECRET'),
                                string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_KEY'),
                                string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET'),
                                string(credentialsId: 'AWS_S3_BUCKET', variable: 'S3_BUCKET'),
                                string(credentialsId: 'GEMINI_API_KEY', variable: 'GEMINI_KEY'),
                                string(credentialsId: 'CLOUDINARY_CLOUD_NAME', variable: 'CLOUDINARY_CLOUD_NAME'),
                                string(credentialsId: 'CLOUDINARY_API_KEY', variable: 'CLOUDINARY_API_KEY'),
                                string(credentialsId: 'CLOUDINARY_API_SECRET', variable: 'CLOUDINARY_API_SECRET')
                            ]) {
                                // Docker 이미지 생성 (캐시 활용으로 빌드 시간 단축)
                                sh '''
                                docker build \
                                  --cache-from backend-image:latest \
                                  --build-arg DB_PASSWORD="${DB_PW}" \
                                  --build-arg JWT_SECRET_KEY="${JWT_KEY}" \
                                  --build-arg REDIS_PASSWORD="${REDIS_PW}" \
                                  --build-arg FRONTEND_URL="${FE_URL}" \
                                  --build-arg OPENVIDU_URL="${OV_URL}" \
                                  --build-arg OPENVIDU_SECRET="${OV_SECRET}" \
                                  --build-arg AWS_ACCESS_KEY="${AWS_KEY}" \
                                  --build-arg AWS_SECRET_KEY="${AWS_SECRET}" \
                                  --build-arg AWS_S3_BUCKET="${S3_BUCKET}" \
                                  --build-arg GEMINI_API_KEY="${GEMINI_KEY}" \
                                  --build-arg CLOUDINARY_CLOUD_NAME="${CLOUDINARY_CLOUD_NAME}" \
                                  --build-arg CLOUDINARY_API_KEY="${CLOUDINARY_API_KEY}" \
                                  --build-arg CLOUDINARY_API_SECRET="${CLOUDINARY_API_SECRET}" \
                                  -t backend-image:latest .
                                '''
                            }
                        } else {
                            sh 'docker build --cache-from backend-image:latest -t backend-image:latest .'
                        }
                    }
                }
            }
        }

        // --- 프론트엔드 빌드 ---
        stage('Frontend Build') {
            steps {
                dir('meari-fe') {
                    script {
                        def isReleaseBranch = env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'
                        if (isReleaseBranch) {
                            withCredentials([
                                string(credentialsId: 'VITE_BASE_SERVER_URL', variable: 'BE_URL')
                            ]) {
                                sh '''
                                docker build \
                                  --cache-from frontend-image:latest \
                                  --build-arg VITE_BASE_SERVER_URL="${BE_URL}" \
                                  --build-arg VITE_USE_MOCK_API=false \
                                  --build-arg VITE_USE_MOCK_AUTH=false \
                                  --build-arg VITE_USE_MOCK_ROOMS=false \
                                  --build-arg VITE_USE_MOCK_WEBRTC=false \
                                  --build-arg VITE_USE_MOCK_CONTENTS=false \
                                  -t frontend-image:latest .
                                '''
                            }
                        } else {
                            sh 'docker build --cache-from frontend-image:latest -t frontend-image:latest .'
                        }
                    }
                }
            }
        }

        // --- FastAPI 빌드 ---
        stage('FastAPI Build') {
            steps {
                dir('meari-ai') {
                    script {
                        if (params.NO_CACHE_BUILD) {
                            echo '🚨 NO CACHE FastAPI build'
                            sh 'docker build --no-cache -t meari-fastapi:latest .'
                        } else {
                            echo '♻️ Cached FastAPI build'
                            sh 'docker build --cache-from meari-fastapi:latest -t meari-fastapi:latest .'
                        }
                    }
                }
            }
        }

        // 배포 단계: release 브랜치에 푸시될 때만 실행
        stage('Deploy') {
            when {
                expression {
                    return env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'
                }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'DB_PASSWORD', variable: 'DB_PW'),
                    string(credentialsId: 'JWT_SECRET_KEY', variable: 'JWT_KEY'),
                    string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PW'),
                    string(credentialsId: 'RABBITMQ_PASSWORD', variable: 'RABBITMQ_PW'),
                    string(credentialsId: 'FRONTEND_URL', variable: 'FE_URL'),
                    string(credentialsId: 'OPENVIDU_URL', variable: 'OV_URL'),
                    string(credentialsId: 'OPENVIDU_SECRET', variable: 'OV_SECRET'),
                    string(credentialsId: 'VITE_BASE_SERVER_URL', variable: 'BE_URL'),
                    string(credentialsId: 'GEMINI_API_KEY', variable: 'GEMINI_KEY'),
                    string(credentialsId: 'AWS_ACCESS_KEY', variable: 'AWS_KEY'),
                    string(credentialsId: 'AWS_SECRET_KEY', variable: 'AWS_SECRET'),
                    string(credentialsId: 'AWS_S3_BUCKET', variable: 'S3_BUCKET'),
                    string(credentialsId: 'CLOUDINARY_CLOUD_NAME', variable: 'CLOUDINARY_CLOUD_NAME'),
                    string(credentialsId: 'CLOUDINARY_API_KEY', variable: 'CLOUDINARY_API_KEY'),
                    string(credentialsId: 'CLOUDINARY_API_SECRET', variable: 'CLOUDINARY_API_SECRET')
                ]) {
                    script {
                        sh '''
                            # .env 파일 생성
                            echo "DB_PASSWORD=${DB_PW}" > .env
                            echo "JWT_SECRET_KEY=${JWT_KEY}" >> .env
                            echo "REDIS_PASSWORD=${REDIS_PW}" >> .env
                            echo "RABBITMQ_PASSWORD=${RABBITMQ_PW}" >> .env
                            echo "FRONTEND_URL=${FE_URL}" >> .env
                            echo "OPENVIDU_URL=${OV_URL}" >> .env
                            echo "OPENVIDU_SECRET=${OV_SECRET}" >> .env
                            echo "OPENVIDU_DOMAIN=localhost" >> .env
                            echo "VITE_BASE_SERVER_URL=${BE_URL}" >> .env
                            # --- JWT 설정 (기본값 주입) ---
                            echo "JWT_ACCESS_TOKEN_EXPIRE_PERIOD=43200000" >> .env
                            echo "JWT_REFRESH_TOKEN_EXPIRE_PERIOD=1209600000" >> .env
                            echo "GEMINI_API_KEY=${GEMINI_KEY}" >> .env
                            echo "GEMINI_MODEL=gemini-2.5-flash" >> .env
                            echo "GEMINI_BASE_URL=https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta" >> .env
                            echo "AWS_ACCESS_KEY=${AWS_KEY}" >> .env
                            echo "AWS_SECRET_KEY=${AWS_SECRET}" >> .env
                            echo "AWS_S3_BUCKET=${S3_BUCKET}" >> .env
                            echo "AWS_REGION=ap-northeast-2" >> .env
                            echo "CLOUD_AWS_PRESIGNED_URL_VIDEO_EXPIRATION=3600" >> .env
                            echo "CLOUD_AWS_PRESIGNED_URL_UPLOAD_EXPIRATION=900" >> .env
                            echo "CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME}" >> .env
                            echo "CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY}" >> .env
                            echo "CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET}" >> .env

                            # docker-compose로 배포 (최신 yml 파일 사용)
                            docker compose -f docker-compose.yml up -d --remove-orphans spring-api frontend fastapi
                        '''
                        sh 'docker image prune -f'
                    }
                }
            }
        }
    }

    // 빌드 완료 후 Mattermost 알림
    post {
        success {
            script {
                def message = "✅ 빌드 성공! - Branch: ${env.GIT_BRANCH} #${env.BUILD_NUMBER}"
                if (env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release') {
                    message = "✅ 배포 성공!: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                }
                try {
                    mattermostSend(color: 'good', message: message + " (<${env.BUILD_URL}|상세보기>)")
                } catch (e) { echo "Mattermost 알림 실패" }
                echo message
            }
        }
        failure {
            script {
                def message = "🚨 빌드 실패! - Branch: ${env.GIT_BRANCH} #${env.BUILD_NUMBER}"
                if (env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release') {
                    message = "🚨 배포 실패(확인요망): ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                }
                try {
                    mattermostSend(color: 'danger', message: message + " (<${env.BUILD_URL}|상세보기>)")
                } catch (e) { echo "Mattermost 알림 실패" }
                echo message
            }
        }
    }
}