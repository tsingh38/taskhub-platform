############################################################
# Monitoring (shared cluster-level)
############################################################

resource "kubernetes_secret" "alertmanager_slack" {
  metadata {
    name      = "alertmanager-slack-token"
    namespace = "monitoring"
  }

  data = {
    token = var.slack_webhook_url
  }

  type = "Opaque"

  depends_on = [kubernetes_namespace.monitoring]
}

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

  depends_on = [
    kubernetes_namespace.monitoring,
    kubernetes_secret.alertmanager_slack
  ]
}

############################################################
# DEV Environment
############################################################

resource "kubernetes_secret" "db_credentials_dev" {
  metadata {
    name      = "db-credentials"
    namespace = "dev"
  }

  data = {
    postgres-user     = var.db_user_dev
    postgres-password = var.db_password_dev

    # Bitnami chart expects this key for the application user password
    password          = var.db_password_dev
  }

  type = "Opaque"

  depends_on = [kubernetes_namespace.dev]
}

resource "helm_release" "postgres_dev" {
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

  set {
    name  = "auth.existingSecret"
    value = "db-credentials"
  }

  set {
    name  = "auth.username"
    value = var.db_user_dev
  }

  set {
    name  = "auth.database"
    value = "taskdb"
  }

  depends_on = [
    kubernetes_namespace.dev,
    kubernetes_secret.db_credentials_dev
  ]
}

resource "helm_release" "task_service_dev" {
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
    kubernetes_namespace.dev,
    helm_release.postgres_dev
  ]
}

############################################################
# PROD Environment
############################################################

resource "kubernetes_secret" "db_credentials_prod" {
  metadata {
    name      = "db-credentials"
    namespace = "prod"
  }

  data = {
    postgres-user     = var.db_user_prod
    postgres-password = var.db_password_prod

    # Bitnami chart expects this key for the application user password
    password          = var.db_password_prod
  }

  type = "Opaque"

  depends_on = [kubernetes_namespace.prod]
}

resource "helm_release" "postgres_prod" {
  name       = "postgres"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "postgresql"
  namespace  = "prod"
  version    = var.postgres_chart_version

  values = [
    file("../helm/values/postgres/values.prod.yaml")
  ]

  set {
    name  = "image.repository"
    value = "bitnamilegacy/postgresql"
  }

  set {
    name  = "image.tag"
    value = var.postgres_image_tag
  }

  set {
    name  = "auth.existingSecret"
    value = "db-credentials"
  }

  set {
    name  = "auth.username"
    value = var.db_user_prod
  }

  set {
    name  = "auth.database"
    value = "taskdb"
  }

  depends_on = [
    kubernetes_namespace.prod,
    kubernetes_secret.db_credentials_prod
  ]
}

resource "helm_release" "task_service_prod" {
  name      = "task-service"
  chart     = "../helm/charts/task-service"
  namespace = "prod"

  values = [
    file("../helm/charts/task-service/values.yaml"),
    file("../helm/charts/task-service/values-prod.yaml")
  ]

  set {
    name  = "image.tag"
    value = var.app_version
  }

  depends_on = [
    kubernetes_namespace.prod,
    helm_release.postgres_prod
  ]
}