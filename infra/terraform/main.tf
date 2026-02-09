resource "kubernetes_namespace" "monitoring" {
  metadata { name = "monitoring" }
}

resource "kubernetes_namespace" "dev" {
  metadata { name = "dev" }
}

resource "helm_release" "prometheus_stack" {
  name       = "monitoring-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  version    = var.prometheus_chart_version

  values = [
    file("../k8s/monitoring/values.yaml"),
    file("../k8s/monitoring/values-dev.yaml")
  ]
}

resource "helm_release" "postgres" {
  name       = "postgres"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "postgresql"
  namespace  = kubernetes_namespace.dev.metadata[0].name
  version    = var.postgres_chart_version

  values = [
    file("../helm/postgres/values.dev.yaml")
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
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }

  data = {
    token = var.slack_webhook_url
  }

  type = "Opaque"
}

resource "helm_release" "task_service" {
  name       = "task-service"
  chart      = "../helm/task-service"
  namespace  = kubernetes_namespace.dev.metadata[0].name

  values = [
    file("../helm/task-service/values.yaml"),
    file("../helm/task-service/values-dev.yaml")
  ]

  set {
    name  = "image.tag"
    value = var.app_version
  }

  depends_on = [helm_release.postgres]
}