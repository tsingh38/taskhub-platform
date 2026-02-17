pipeline {
  agent any

  environment {
    REGISTRY = "tsingh38"
    IMAGE_NAME = "taskhub"
    DOCKERHUB_CREDENTIALS = "dockerhub-tsingh38-taskhub"
    KUBECONFIG = "/var/lib/jenkins/.kube/config"
    TF_STATE_FILE = "/var/lib/jenkins/terraform-state/taskhub-dev/terraform.tfstate"
    TRIVY_CACHE_DIR = "/var/lib/jenkins/.cache/trivy"
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
          changeset "infra/helm/charts/task-service/**"
          changeset "infra/helm/values/**"
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
                ./gradlew clean test
              '''
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: 'services/task-service/build/test-results/test/*.xml'
              archiveArtifacts artifacts: 'services/task-service/build/reports/tests/test/**', allowEmptyArchive: true
            }
          }
        }

        stage('Docker Build & Push') {
          steps {
            dir('services/task-service') {
              withCredentials([usernamePassword(
                credentialsId: DOCKERHUB_CREDENTIALS,
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
              )]) {
                sh '''
                  set -eu
                  IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"
                  IMAGE="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

                  echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                  docker build -t "$IMAGE" .
                  docker push "$IMAGE"
                '''
              }
            }
          }
        }

        stage('Trivy Scan (HIGH/CRITICAL gate)') {
          steps {
            dir('services/task-service') {
              withCredentials([usernamePassword(
                credentialsId: DOCKERHUB_CREDENTIALS,
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
              )]) {
                sh '''
                  set -eu
                  IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"
                  IMAGE="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

                  mkdir -p "$TRIVY_CACHE_DIR"

                  docker run --rm \
                    -v "$TRIVY_CACHE_DIR:/root/.cache/" \
                    -v "$WORKSPACE:/work" \
                    aquasec/trivy:latest \
                    image \
                    --timeout 5m \
                    --no-progress \
                    --severity HIGH,CRITICAL \
                    --exit-code 1 \
                    --format json \
                    -o /work/trivy-report.json \
                    --username "$DOCKER_USER" \
                    --password "$DOCKER_PASS" \
                    "$IMAGE"
                '''
              }
            }
          }
          post {
            always {
              archiveArtifacts artifacts: 'trivy-report.json', fingerprint: true, onlyIfSuccessful: false
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
                string(credentialsId: 'db-user', variable: 'DB_USER'),
                string(credentialsId: 'db-password', variable: 'DB_PASSWORD')
              ]) {
                sh '''
                  set -eu

                  test -f "$KUBECONFIG"
                  mkdir -p "$(dirname "$TF_STATE_FILE")"

                  kubectl --kubeconfig "$KUBECONFIG" get nodes

                  kubectl --kubeconfig "$KUBECONFIG" get ns dev >/dev/null 2>&1 || kubectl --kubeconfig "$KUBECONFIG" create ns dev
                  kubectl --kubeconfig "$KUBECONFIG" get ns monitoring >/dev/null 2>&1 || kubectl --kubeconfig "$KUBECONFIG" create ns monitoring

                  export TF_VAR_kubeconfig_path="$KUBECONFIG"
                  export TF_VAR_app_version="${APP_VERSION}-${BUILD_NUMBER}"
                  export TF_VAR_slack_webhook_url="$SLACK_WEBHOOK"
                  export TF_VAR_db_user="$DB_USER"
                  export TF_VAR_db_password="$DB_PASSWORD"

                  terraform init -input=false
                  terraform apply -auto-approve -input=false -state="$TF_STATE_FILE"
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