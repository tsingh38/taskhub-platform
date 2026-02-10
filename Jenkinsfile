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
                    changeset "infra/terraform/**"
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
                              set -eu
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
                                sh '''
                                  set -eu
                                  IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"

                                  echo "$PASS" | docker login -u "$USER" --password-stdin
                                  docker build -t "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" .
                                  docker push "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                                '''
                            }
                        }
                    }
                }

                stage('Deploy to DEV') {
                    when {
                        expression {
                            return (env.GIT_BRANCH == 'origin/develop' || env.GIT_BRANCH == 'develop' || env.BRANCH_NAME == 'develop')
                        }
                    }
                    steps {
                        dir('infra/terraform') {
                            withCredentials([
                                string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK'),
                                file(credentialsId: 'kubeconfig-dev', variable: 'KUBECONFIG')
                            ]) {
                                sh '''
                                  set -eu

                                  export KUBECONFIG="$KUBECONFIG"
                                  IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"

                                  # Create namespaces if missing (idempotent)
                                  kubectl get ns dev >/dev/null 2>&1 || kubectl create ns dev
                                  kubectl get ns monitoring >/dev/null 2>&1 || kubectl create ns monitoring

                                  terraform init
                                  terraform apply \
                                    -var="kubeconfig_path=$KUBECONFIG" \
                                    -var="app_version=$IMAGE_TAG" \
                                    -var="slack_webhook_url=$SLACK_WEBHOOK" \
                                    -auto-approve
                                '''
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