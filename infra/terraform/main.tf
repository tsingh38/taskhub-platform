resource "helm_release" "prometheus_stack" {
  name       = "monitoring-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = "monitoring"
  version    = var.prometheus_chart_version

  values = [
    file("../helm/values/monitoring/values.yaml"),
    file("../helm/values/monitoring/values-dev.yaml")
  ]
}

resource "kubernetes_secret" "db_credentials" {
  metadata {
    name      = "db-credentials"
    namespace = "dev"
  }

  data = {
    username          = var.db_user
    password          = var.db_password
    postgres-password = var.db_password
  }

  type = "Opaque"
}

resource "helm_release" "postgres" {
  name       = "postgres"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "postgresql"
  namespace  = "dev"
  version    = var.postgres_chart_version

  values = [
    file("../helm/values/postgres/values.dev.yaml")
  ]

  set {
    name  = "image.repository"
    value = "bitnamilegacy/postgresql"
  }

  set {
    name  = "image.tag"
    value = var.postgres_image_tag
  }

  # NEW: tell Bitnami Postgres to use the Terraform-created secret
  set {
    name  = "auth.existingSecret"
    value = "db-credentials"
  }

  # NEW: keep username out of Git; pass it from Terraform var (Jenkins -> TF_VAR_db_user)
  set {
    name  = "auth.username"
    value = var.db_user
  }

  # NEW: set database name explicitly (not secret)
  set {
    name  = "auth.database"
    value = "taskdb"
  }

  depends_on = [kubernetes_secret.db_credentials]
}

resource "kubernetes_secret" "alertmanager_slack" {
  metadata {
    name      = "alertmanager-slack-token"
    namespace = "monitoring"
  }

  data = {
    token = var.slack_webhook_url
  }

  type = "Opaque"
}

resource "helm_release" "task_service" {
  name      = "task-service"
  chart     = "../helm/charts/task-service"
  namespace = "dev"

  values = [
    file("../helm/charts/task-service/values.yaml"),
    file("../helm/charts/task-service/values-dev.yaml")
  ]

  set {
    name  = "image.tag"
    value = var.app_version
  }

  depends_on = [
    helm_release.postgres,
    kubernetes_secret.db_credentials
  ]
}