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
                            sh 'chmod +x ./gradlew'
                            sh './gradlew clean build -x test --refresh-dependencies'
                            sh 'docker build -t backend-image:latest .'
                        }
                    }
                }

                stage('Frontend Build') {
                    steps {
                        dir('meari-fe') {
                            sh 'docker build -t frontend-image:latest .'
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'release'  // release 브랜치만 배포
            }
            steps {
                withCredentials([
                    string(credentialsId: 'DB_PASSWORD', variable: 'DB_PW'),
                    string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PW'),
                    string(credentialsId: 'RABBITMQ_PASSWORD', variable: 'RABBITMQ_PW'),
                    string(credentialsId: 'OPENVIDU_SECRET', variable: 'OV_SECRET'),
                    string(credentialsId: 'OPENVIDU_DOMAIN', variable: 'OV_DOMAIN')
                ]) {
                    script {
                        // 1. .env 파일 생성
                        sh """
                        # --- Database 설정 ---
                        echo "DB_PASSWORD=${DB_PW}" > .env

                        # --- Redis 설정 ---
                        echo "REDIS_PASSWORD=${REDIS_PW}" >> .env

                        # --- RabbitMQ 설정 ---
                        echo "RABBITMQ_PASSWORD=${RABBITMQ_PW}" >> .env

                        # --- OpenVidu 설정 ---
                        echo "OPENVIDU_SECRET=${OV_SECRET}" >> .env
                        echo "OPENVIDU_DOMAIN=${OV_DOMAIN}" >> .env
                        """

                        // 2. 배포 실행
                        sh 'docker-compose down frontend spring-api || true'
                        sh 'docker-compose up -d frontend spring-api'
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

                // Mattermost 알림 (설정되어 있는 경우)
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

                // Mattermost 알림 (설정되어 있는 경우)
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