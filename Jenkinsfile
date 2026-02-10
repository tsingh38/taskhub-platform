pipeline {
  agent any

  environment {
    REGISTRY = "tsingh38"
    IMAGE_NAME = "taskhub"
    DOCKERHUB_CREDENTIALS = "dockerhub-tsingh38-taskhub"

    // Local kubeconfig on the SAME machine (works for jenkins user)
    KUBECONFIG = "/var/lib/jenkins/.kube/config"

    // Option B: persistent local state (outside workspace)
    TF_STATE_FILE = "/var/lib/jenkins/terraform-state/taskhub-dev/terraform.tfstate"
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
              withCredentials([usernamePassword(
                credentialsId: DOCKERHUB_CREDENTIALS,
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
              )]) {
                sh '''
                  set -eu
                  IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"

                  echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
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
              withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK')]) {
                sh '''
                  set -eu

                  echo "Using kubeconfig: $KUBECONFIG"
                  echo "Using TF state : $TF_STATE_FILE"

                  # Hard fail if kubeconfig missing
                  test -f "$KUBECONFIG"
                  ls -la "$KUBECONFIG"

                  # Ensure state dir exists (Option B)
                  mkdir -p "$(dirname "$TF_STATE_FILE")"

                  # Prove Jenkins user can reach the cluster
                  kubectl --kubeconfig "$KUBECONFIG" get nodes

                  # Option B: namespaces must exist BEFORE Terraform/Helm
                  kubectl --kubeconfig "$KUBECONFIG" get ns dev >/dev/null 2>&1 || kubectl --kubeconfig "$KUBECONFIG" create ns dev
                  kubectl --kubeconfig "$KUBECONFIG" get ns monitoring >/dev/null 2>&1 || kubectl --kubeconfig "$KUBECONFIG" create ns monitoring

                  # Pass Terraform variables via environment (clean + avoids Groovy interpolation warnings)
                  export TF_VAR_kubeconfig_path="$KUBECONFIG"
                  export TF_VAR_app_version="${APP_VERSION}-${BUILD_NUMBER}"
                  export TF_VAR_slack_webhook_url="$SLACK_WEBHOOK"

                  terraform init -input=false

                  # CRITICAL: always use the persistent state file (Option B)
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