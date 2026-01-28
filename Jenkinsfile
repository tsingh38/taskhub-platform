pipeline {
    agent any

    environment {
        REGISTRY = "tsingh38"
        IMAGE_NAME = "taskhub"
        DOCKERHUB_CREDENTIALS = "dockerhub-tsingh38-taskhub"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Resolve Version') {
            steps {
                dir('services/task-service') {
                    script {
                        def version = sh(
                            script: "./gradlew properties -q | grep '^version:' | awk '{print \$2}'",
                            returnStdout: true
                        ).trim()

                        env.APP_VERSION = version
                        echo "Resolved app version: ${env.APP_VERSION}"
                    }
                }
            }
        }

        stage('Build & Test') {
            steps {
                dir('services/task-service') {
                    sh '''
                      chmod +x gradlew
                      ./gradlew clean build
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir('services/task-service') {
                    sh """
                      docker build -t ${REGISTRY}/${IMAGE_NAME}:${APP_VERSION} .
                    """
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKERHUB_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                      echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                      docker push ${REGISTRY}/${IMAGE_NAME}:${APP_VERSION}
                    """
                }
            }
        }

        stage('Deploy to DEV') {
            when {
                branch 'develop'
            }
            steps {
                echo "Deploying ${APP_VERSION} to DEV"
                // placeholder – helm / kubectl later
            }
        }
    }

    post {
        success {
            echo "✅ Image pushed: ${REGISTRY}/${IMAGE_NAME}:${APP_VERSION}"
        }
        failure {
            echo "❌ CI failed"
        }
    }
}