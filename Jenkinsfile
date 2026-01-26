pipeline {
    agent any

    // 1분마다 GitLab 폴링
    triggers {
        pollSCM('* * * * *')
    }

    environment {
        // Docker 이미지 이름
        BACKEND_IMAGE = "meari-spring:latest"
        FRONTEND_IMAGE = "meari-frontend:latest"

        // GitLab Credentials ID (Jenkins에서 설정)
        GITLAB_CREDENTIALS = credentials('gitlab-credentials')

        // 환경 변수 Credentials (Jenkins에서 설정)
        DB_PASSWORD = credentials('db-password')
        REDIS_PASSWORD = credentials('redis-password')
        RABBITMQ_PASSWORD = credentials('rabbitmq-password')
        OPENVIDU_SECRET = credentials('openvidu-secret')
        OPENVIDU_DOMAIN = credentials('openvidu-domain')
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "=== Checkout Stage ==="
                    echo "Branch: ${env.GIT_BRANCH}"
                    checkout scm
                }
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    echo "=== Building Backend ==="
                    dir('meari-be') {
                        sh '''
                            docker build -t ${BACKEND_IMAGE} .
                        '''
                    }
                }
            }
        }

        stage('Test Backend') {
            steps {
                script {
                    echo "=== Testing Backend ==="
                    dir('meari-be') {
                        sh '''
                            ./gradlew test --no-daemon
                        '''
                    }
                }
            }
        }

        stage('Build Frontend') {
            steps {
                script {
                    echo "=== Building Frontend ==="
                    sh '''
                        docker build -t ${FRONTEND_IMAGE} -f meari-fe/Dockerfile .
                    '''
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'release'
            }
            steps {
                script {
                    echo "=== Deploying to EC2 ==="

                    // .env 파일 생성
                    sh '''
                        cat > .env << EOF
DB_PASSWORD=${DB_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
OPENVIDU_SECRET=${OPENVIDU_SECRET}
OPENVIDU_DOMAIN=${OPENVIDU_DOMAIN}
EOF
                    '''

                    // Docker Compose로 배포
                    sh '''
                        docker compose down spring-api frontend || true
                        docker compose up -d spring-api frontend
                    '''

                    // .env 파일 삭제 (보안)
                    sh 'rm -f .env'
                }
            }
        }

        stage('Clean Up') {
            steps {
                script {
                    echo "=== Cleaning Up ==="
                    sh '''
                        docker image prune -f
                    '''
                }
            }
        }
    }

    post {
        success {
            script {
                def message = """
                ✅ 빌드 성공!
                - Branch: ${env.GIT_BRANCH}
                - Build: #${env.BUILD_NUMBER}
                - Stage: ${env.STAGE_NAME}
                """

                if (env.GIT_BRANCH == 'release') {
                    message += "\n🚀 배포 완료!"
                }

                echo message
            }
        }

        failure {
            script {
                def message = """
                ❌ 빌드 실패!
                - Branch: ${env.GIT_BRANCH}
                - Build: #${env.BUILD_NUMBER}
                - Stage: ${env.STAGE_NAME}
                """

                echo message
            }
        }

        always {
            cleanWs()
        }
    }
}
