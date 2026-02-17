pipeline {
  agent any

  parameters {
    booleanParam(name: 'BOOTSTRAP_MONITORING', defaultValue: false, description: 'One-time: import monitoring resources into state')
    booleanParam(name: 'BOOTSTRAP_DEV',        defaultValue: false, description: 'One-time: import dev resources into state')
    booleanParam(name: 'BOOTSTRAP_PROD',       defaultValue: false, description: 'One-time: import prod resources into state')
  }

  environment {
    REGISTRY = "tsingh38"
    IMAGE_NAME = "taskhub"
    DOCKERHUB_CREDENTIALS = "dockerhub-tsingh38-taskhub"
    KUBECONFIG = "/var/lib/jenkins/.kube/config"

    TF_STATE_MONITORING = "/var/lib/jenkins/terraform-state/taskhub-monitoring/terraform.tfstate"
    TF_STATE_DEV        = "/var/lib/jenkins/terraform-state/taskhub-dev/terraform.dev.tfstate"
    TF_STATE_PROD       = "/var/lib/jenkins/terraform-state/taskhub-prod/terraform.prod.tfstate"

    TRIVY_CACHE_DIR = "/var/lib/jenkins/.cache/trivy"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    // -------------------------
    // BOOTSTRAP: MONITORING
    // -------------------------
    stage('Terraform Monitoring (one-time)') {
      when { expression { return params.BOOTSTRAP_MONITORING == true } }
      steps {
        dir('infra/terraform/monitoring') {
          withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK')]) {
            sh '''
              set -eu
              test -f "$KUBECONFIG"
              mkdir -p "$(dirname "$TF_STATE_MONITORING")"

              export TF_VAR_kubeconfig_path="$KUBECONFIG"
              export TF_VAR_slack_webhook_url="$SLACK_WEBHOOK"

              terraform init -reconfigure -input=false -backend-config="path=$TF_STATE_MONITORING"

              # Import namespace/secret if they exist but are missing in state
              terraform import -input=false kubernetes_namespace.monitoring monitoring || true
              terraform import -input=false kubernetes_secret.alertmanager_slack monitoring/alertmanager-slack-token || true

              # Import helm release if it exists but is missing in state
              terraform import -input=false helm_release.prometheus_stack monitoring/monitoring-stack || true

              terraform plan
              terraform apply -auto-approve -input=false
            '''
          }
        }
      }
    }

    // -------------------------
    // BOOTSTRAP: DEV
    // -------------------------
    stage('Terraform DEV (one-time)') {
      when { expression { return params.BOOTSTRAP_DEV == true } }
      steps {
        dir('infra/terraform/dev') {
          withCredentials([
            string(credentialsId: 'db-user', variable: 'DB_USER_DEV'),
            string(credentialsId: 'db-password', variable: 'DB_PASSWORD_DEV')
          ]) {
            sh '''
              set -eu
              test -f "$KUBECONFIG"
              mkdir -p "$(dirname "$TF_STATE_DEV")"

              export TF_VAR_kubeconfig_path="$KUBECONFIG"
              export TF_VAR_db_user_dev="$DB_USER_DEV"
              export TF_VAR_db_password_dev="$DB_PASSWORD_DEV"
              # app_version not needed for import; only needed for apply/upgrade
              export TF_VAR_app_version="bootstrap"

              terraform init -reconfigure -input=false -backend-config="path=$TF_STATE_DEV"

              terraform import -input=false kubernetes_namespace.dev dev || true
              terraform import -input=false kubernetes_secret.db_credentials_dev dev/db-credentials || true

              terraform import -input=false helm_release.postgres_dev dev/postgres || true
              terraform import -input=false helm_release.task_service_dev dev/task-service || true

              terraform plan
              terraform apply -auto-approve -input=false
            '''
          }
        }
      }
    }

    // -------------------------
    // BOOTSTRAP: PROD
    // -------------------------
    stage('Terraform PROD (one-time)') {
      when { expression { return params.BOOTSTRAP_PROD == true } }
      steps {
        dir('infra/terraform/prod') {
          withCredentials([
            string(credentialsId: 'db-user-prod', variable: 'DB_USER_PROD'),
            string(credentialsId: 'db-password-prod', variable: 'DB_PASSWORD_PROD')
          ]) {
            sh '''
              set -eu
              test -f "$KUBECONFIG"
              mkdir -p "$(dirname "$TF_STATE_PROD")"

              export TF_VAR_kubeconfig_path="$KUBECONFIG"
              export TF_VAR_db_user_prod="$DB_USER_PROD"
              export TF_VAR_db_password_prod="$DB_PASSWORD_PROD"
              export TF_VAR_app_version="bootstrap"

              terraform init -reconfigure -input=false -backend-config="path=$TF_STATE_PROD"

              terraform import -input=false kubernetes_namespace.prod prod || true
              terraform import -input=false kubernetes_secret.db_credentials_prod prod/db-credentials || true

              terraform import -input=false helm_release.postgres_prod prod/postgres || true
              terraform import -input=false helm_release.task_service_prod prod/task-service || true

              terraform plan
              terraform apply -auto-approve -input=false
            '''
          }
        }
      }
    }

    // -------------------------
    // Build pipeline continues
    // -------------------------
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

              if [ -n "${TAG_NAME:-}" ]; then
                IMAGE_TAG="${TAG_NAME}"
              else
                IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"
              fi

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

              if [ -n "${TAG_NAME:-}" ]; then
                IMAGE_TAG="${TAG_NAME}"
              else
                IMAGE_TAG="${APP_VERSION}-${BUILD_NUMBER}"
              fi

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

    // -------------------------
    // Deploy MONITORING (normal)
    // -------------------------
    stage('Deploy MONITORING') {
      when {
        expression {
          return (env.GIT_BRANCH == 'origin/develop' || env.GIT_BRANCH == 'develop' || env.BRANCH_NAME == 'develop')
        }
      }
      steps {
        dir('infra/terraform/monitoring') {
          withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK')]) {
            sh '''
              set -eu
              test -f "$KUBECONFIG"
              mkdir -p "$(dirname "$TF_STATE_MONITORING")"

              export TF_VAR_kubeconfig_path="$KUBECONFIG"
              export TF_VAR_slack_webhook_url="$SLACK_WEBHOOK"

              terraform init -input=false -backend-config="path=$TF_STATE_MONITORING"

              # Safety import (prevents "cannot re-use name" / state-wiped scenarios)
              terraform import -input=false kubernetes_namespace.monitoring monitoring || true
              terraform import -input=false kubernetes_secret.alertmanager_slack monitoring/alertmanager-slack-token || true
              terraform import -input=false helm_release.prometheus_stack monitoring/monitoring-stack || true

              terraform apply -auto-approve -input=false
            '''
          }
        }
      }
    }

    // -------------------------
    // Deploy DEV (normal)
    // -------------------------
    stage('Deploy DEV') {
      when {
        expression {
          return (env.GIT_BRANCH == 'origin/develop' || env.GIT_BRANCH == 'develop' || env.BRANCH_NAME == 'develop')
        }
      }
      steps {
        dir('infra/terraform/dev') {
          withCredentials([
            string(credentialsId: 'db-user', variable: 'DB_USER_DEV'),
            string(credentialsId: 'db-password', variable: 'DB_PASSWORD_DEV')
          ]) {
            sh '''
              set -eu
              test -f "$KUBECONFIG"
              mkdir -p "$(dirname "$TF_STATE_DEV")"

              export TF_VAR_kubeconfig_path="$KUBECONFIG"
              export TF_VAR_app_version="${APP_VERSION}-${BUILD_NUMBER}"
              export TF_VAR_db_user_dev="$DB_USER_DEV"
              export TF_VAR_db_password_dev="$DB_PASSWORD_DEV"

              terraform init -reconfigure -input=false -backend-config="path=$TF_STATE_DEV"

              # Safety imports (if state is empty but objects exist)
              terraform import -input=false kubernetes_namespace.dev dev || true
              terraform import -input=false kubernetes_secret.db_credentials_dev dev/db-credentials || true
              terraform import -input=false helm_release.postgres_dev dev/postgres || true
              terraform import -input=false helm_release.task_service_dev dev/task-service || true

              terraform apply -auto-approve -input=false
            '''
          }
        }
      }
    }

    // -------------------------
    // Deploy PROD (manual approval)
    // -------------------------
    stage('Deploy PROD (Manual Approval)') {
      when { expression { return (env.TAG_NAME != null && env.TAG_NAME?.trim()) } }
      steps {
        input message: "Deploy tag ${env.TAG_NAME} to PROD?", ok: "Approve"

        dir('infra/terraform/prod') {
          withCredentials([
            string(credentialsId: 'db-user-prod', variable: 'DB_USER_PROD'),
            string(credentialsId: 'db-password-prod', variable: 'DB_PASSWORD_PROD')
          ]) {
            sh '''
              set -eu
              test -f "$KUBECONFIG"
              mkdir -p "$(dirname "$TF_STATE_PROD")"

              export TF_VAR_kubeconfig_path="$KUBECONFIG"
              export TF_VAR_app_version="${TAG_NAME}"
              export TF_VAR_db_user_prod="$DB_USER_PROD"
              export TF_VAR_db_password_prod="$DB_PASSWORD_PROD"

              terraform init -input=false -backend-config="path=$TF_STATE_PROD"

              # Safety imports
              terraform import -input=false kubernetes_namespace.prod prod || true
              terraform import -input=false kubernetes_secret.db_credentials_prod prod/db-credentials || true
              terraform import -input=false helm_release.postgres_prod prod/postgres || true
              terraform import -input=false helm_release.task_service_prod prod/task-service || true

              terraform apply -auto-approve -input=false
            '''
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