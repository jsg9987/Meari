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
                                // release 또는 origin/release 체크
                                def isReleaseBranch = env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'

                                if (isReleaseBranch) {
                                    // release 브랜치: Jenkins Credentials 사용
                                    withCredentials([
                                        string(credentialsId: 'DB_PASSWORD', variable: 'DB_PW'),
                                        string(credentialsId: 'JWT_SECRET_KEY', variable: 'JWT_KEY'),
                                        string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PW'),
                                        string(credentialsId: 'FRONTEND_URL', variable: 'FE_URL'),
                                        string(credentialsId: 'OPENVIDU_URL', variable: 'OV_URL'),
                                        string(credentialsId: 'OPENVIDU_SECRET', variable: 'OV_SECRET')
                                    ]) {
                                        sh '''
                                        docker build \
                                          --build-arg DB_PASSWORD="${DB_PW}" \
                                          --build-arg JWT_SECRET_KEY="${JWT_KEY}" \
                                          --build-arg REDIS_PASSWORD="${REDIS_PW}" \
                                          --build-arg FRONTEND_URL="${FE_URL}" \
                                          --build-arg OPENVIDU_URL="${OV_URL}" \
                                          --build-arg OPENVIDU_SECRET="${OV_SECRET}" \
                                          -t backend-image:latest .
                                        '''
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
                                // release 또는 origin/release 체크
                                def isReleaseBranch = env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'

                                if (isReleaseBranch) {
                                    // release 브랜치: Jenkins Credentials 사용
                                    withCredentials([
                                        string(credentialsId: 'VITE_BASE_SERVER_URL', variable: 'BE_URL')
                                    ]) {
                                        sh '''
                                        docker build \
                                          --build-arg VITE_BASE_SERVER_URL="${BE_URL}" \
                                          --build-arg VITE_USE_MOCK_API=false \
                                          -t frontend-image:latest .
                                        '''
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
                expression {
                    // release 또는 origin/release 브랜치일 때 배포
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
                    string(credentialsId: 'VITE_BASE_SERVER_URL', variable: 'BE_URL')
                ]) {
                    script {
                        // .env 파일 생성 - 각 라인을 echo로 추가
                        sh '''
                            echo "# --- Database 설정 ---" > .env
                            echo "DB_PASSWORD=${DB_PW}" >> .env
                            echo "" >> .env
                            echo "# --- JWT 설정 ---" >> .env
                            echo "JWT_SECRET_KEY=${JWT_KEY}" >> .env
                            echo "" >> .env
                            echo "# --- Redis 설정 ---" >> .env
                            echo "REDIS_PASSWORD=${REDIS_PW}" >> .env
                            echo "" >> .env
                            echo "# --- RabbitMQ 설정 ---" >> .env
                            echo "RABBITMQ_PASSWORD=${RABBITMQ_PW}" >> .env
                            echo "" >> .env
                            echo "# --- Frontend 설정 ---" >> .env
                            echo "FRONTEND_URL=${FE_URL}" >> .env
                            echo "" >> .env
                            echo "# --- OpenVidu 설정 ---" >> .env
                            echo "OPENVIDU_URL=${OV_URL}" >> .env
                            echo "OPENVIDU_SECRET=${OV_SECRET}" >> .env
                            echo "" >> .env
                            echo "# --- Backend URL (for frontend) ---" >> .env
                            echo "VITE_BASE_SERVER_URL=${BE_URL}" >> .env
                        '''

                        // 배포 실행
                        sh 'docker-compose down || true'
                        sh 'docker-compose up -d'
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

                // release 또는 origin/release 체크
                def isReleaseBranch = env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'

                if (isReleaseBranch) {
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

                // release 또는 origin/release 체크
                def isReleaseBranch = env.GIT_BRANCH == 'release' || env.GIT_BRANCH == 'origin/release'

                if (isReleaseBranch) {
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