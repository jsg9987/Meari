pipeline {
    agent any

    triggers {
        pollSCM('* * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Docker Image') {
            parallel {
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
                                        string(credentialsId: 'DOMAIN', variable: 'DOMAIN') // 각 EC2에 맞는 도메인값을 주입하기 위한 환경변수
                                    ]) {
                                        sh '''
                                        docker build \
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
                                          --build-arg DOMAIN="${DOMAIN}" \
                                          -t backend-image:latest .
                                        '''
                                    }
                                } else {
                                    sh 'docker build -t backend-image:latest .'
                                }
                            }
                        }
                    }
                }

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
                                    sh 'docker build -t frontend-image:latest .'
                                }
                            }
                        }
                    }
                }

                // --- 임시 FastAPI 빌드 (프로젝트 폴더가 없을 때 사용) ---
                stage('FastAPI Build') {
                    steps {
                        script {
                            // meari-ai 폴더가 없으므로 현재 경로에서 임시 Dockerfile 생성 후 빌드
                            // 8000번 포트로 단순히 응답만 해주는 초경량 Python 이미지입니다.
                            sh '''
                                echo "FROM python:3.9-slim" > Dockerfile.dummy
                                echo "CMD [\\"python\\", \\"-m\\", \\"http.server\\", \\"8000\\"]" >> Dockerfile.dummy
                                docker build -t meari-fastapi:latest -f Dockerfile.dummy .
                                rm Dockerfile.dummy
                            '''
                        }
                    }
                }
            }
        }
                // --- 개발 된다면 FastAPI 빌드 스테이지 추가!! ---
//                 stage('FastAPI Build') {
//                     steps {
//                         dir('meari-ai') { // FastAPI 소스 코드가 있는 디렉토리 이름으로 수정하세요
//                             script {
//                                 // FastAPI는 별도의 build-arg가 없다면 간단히 빌드합니다.
//                                 sh 'docker build -t meari-fastapi:latest .'
//                             }
//                         }
//                     }
//                 }
//             }
//         }
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
                    string(credentialsId: 'DOMAIN', variable: 'DOMAIN')
                ]) {
                    script {
                        sh '''
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
                            # 만약 application.yml의 변수명이 다르면 아래 이름을 수정하세요
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
                            echo "DOMAIN=${DOMAIN}" >> .env

                            docker-compose up -d --force-recreate frontend spring-api fastapi
                        '''
                        sh 'docker image prune -f'
                    }
                }
            }
        }
    }

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