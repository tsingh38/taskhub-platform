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

        stage('Application Lifecycle') {
            when {
                anyOf {
                    changeset "services/task-service/**"
                    changeset "infra/helm/task-service/**"
                    changeset "Jenkinsfile"
                }
            }

            stages {
                stage('Resolve Version') {
                    steps {
                        dir('services/task-service') {
                            script {
                                def version = sh(
                                    script: "./gradlew properties -q | grep '^version:' | awk '{print \$2}'",
                                    returnStdout: true
                                ).trim()
                                env.APP_VERSION = version
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

                stage('Docker Build & Push') {
                    steps {
                        dir('services/task-service') {
                            withCredentials([
                                usernamePassword(
                                    credentialsId: DOCKERHUB_CREDENTIALS,
                                    usernameVariable: 'USER',
                                    passwordVariable: 'PASS'
                                )
                            ]) {
                                sh """
                                  docker build -t ${REGISTRY}/${IMAGE_NAME}:${APP_VERSION}-${BUILD_NUMBER} .
                                  echo "\$PASS" | docker login -u "\$USER" --password-stdin
                                  docker push ${REGISTRY}/${IMAGE_NAME}:${APP_VERSION}-${BUILD_NUMBER}
                                """
                            }
                        }
                    }
                }

                stage('Deploy to DEV') {
                    when {
                      expression {
                        return env.GIT_BRANCH == 'origin/develop' || env.GIT_BRANCH == 'develop'
                      }
                    }
                    steps {
                        dir('infra/terraform') {
                            withCredentials([
                                string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK')
                            ]) {
                                sh """
                                  terraform init
                                  terraform apply \
                                    -var="app_version=${APP_VERSION}-${BUILD_NUMBER}" \
                                    -var="slack_webhook_url=\$SLACK_WEBHOOK" \
                                    -auto-approve
                                """
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        success { echo "Pipeline Success" }
        failure { echo "Pipeline Failed" }
    }
}