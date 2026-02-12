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

  depends_on = [helm_release.postgres]
}