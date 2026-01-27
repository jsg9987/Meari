pipeline {
    agent any

    // 1분마다 GitLab 폴링
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
                                if (env.GIT_BRANCH == 'release') {
                                    // release 브랜치: Jenkins Credentials 사용
                                    withCredentials([
                                        string(credentialsId: 'DB_PASSWORD', variable: 'DB_PW'),
                                        string(credentialsId: 'JWT_SECRET_KEY', variable: 'JWT_KEY'),
                                        string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PW'),
                                        string(credentialsId: 'FRONTEND_URL', variable: 'FE_URL'),
                                        string(credentialsId: 'OPENVIDU_URL', variable: 'OV_URL'),
                                        string(credentialsId: 'OPENVIDU_SECRET', variable: 'OV_SECRET')
                                    ]) {
                                        sh """
                                        docker build \
                                          --build-arg DB_PASSWORD='${DB_PW}' \
                                          --build-arg JWT_SECRET_KEY='${JWT_KEY}' \
                                          --build-arg REDIS_PASSWORD='${REDIS_PW}' \
                                          --build-arg FRONTEND_URL='${FE_URL}' \
                                          --build-arg OPENVIDU_URL='${OV_URL}' \
                                          --build-arg OPENVIDU_SECRET='${OV_SECRET}' \
                                          -t backend-image:latest .
                                        """
                                    }
                                } else {
                                    // 다른 브랜치: 기본값 사용
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
                                if (env.GIT_BRANCH == 'release') {
                                    // release 브랜치: Jenkins Credentials 사용
                                    withCredentials([
                                        string(credentialsId: 'VITE_BASE_SERVER_URL', variable: 'BE_URL')
                                    ]) {
                                        sh """
                                        docker build \
                                          --build-arg VITE_BASE_SERVER_URL='${BE_URL}' \
                                          --build-arg VITE_USE_MOCK_API=false \
                                          -t frontend-image:latest .
                                        """
                                    }
                                } else {
                                    // 다른 브랜치: 기본값 사용
                                    sh 'docker build -t frontend-image:latest .'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'release'  // release 브랜치만 배포!
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
                    string(credentialsId: 'VITE_BASE_SERVER_URL', variable: 'BE_URL')
                ]) {
                    script {
                        // .env 파일 생성
                        sh """
                        cat > .env << EOF
# --- Database 설정 ---
DB_PASSWORD=${DB_PW}

# --- JWT 설정 ---
JWT_SECRET_KEY=${JWT_KEY}

# --- Redis 설정 ---
REDIS_PASSWORD=${REDIS_PW}

# --- RabbitMQ 설정 ---
RABBITMQ_PASSWORD=${RABBITMQ_PW}

# --- Frontend 설정 ---
FRONTEND_URL=${FE_URL}

# --- OpenVidu 설정 ---
OPENVIDU_URL=${OV_URL}
OPENVIDU_SECRET=${OV_SECRET}

# --- Backend URL (for frontend) ---
VITE_BASE_SERVER_URL=${BE_URL}
EOF
                        """

                        // 배포 실행
                        sh 'docker compose down frontend spring-api || true'
                        sh 'docker compose up -d frontend spring-api'
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

                if (env.GIT_BRANCH == 'release') {
                    message = "✅ 배포 성공!: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                }

                // Mattermost 알림
                try {
                    mattermostSend (
                        color: 'good',
                        message: message + " (<${env.BUILD_URL}|상세보기>)"
                    )
                } catch (Exception e) {
                    echo "Mattermost 알림 실패: ${e.message}"
                }

                echo message
            }
        }
        failure {
            script {
                def message = "🚨 빌드 실패! - Branch: ${env.GIT_BRANCH} #${env.BUILD_NUMBER}"

                if (env.GIT_BRANCH == 'release') {
                    message = "🚨 배포 실패(확인요망): ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                }

                // Mattermost 알림
                try {
                    mattermostSend (
                        color: 'danger',
                        message: message + " (<${env.BUILD_URL}|상세보기>)"
                    )
                } catch (Exception e) {
                    echo "Mattermost 알림 실패: ${e.message}"
                }

                echo message
            }
        }
    }
}