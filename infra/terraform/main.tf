# --- STEP 1: NAMESPACES ---
resource "kubernetes_namespace" "monitoring" {
  metadata { name = "monitoring" }
}

resource "kubernetes_namespace" "dev" {
  metadata { name = "dev" }
}

# --- STEP 2: THE MONITORING STACK ---
resource "helm_release" "prometheus_stack" {
  name       = "monitoring-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  version    = "56.6.2" # Fixed version to prevent the lookup panic

  values = [
    file("../k8s/monitoring/values.yaml"),
    file("../k8s/monitoring/values-dev.yaml")
  ]
}

# --- STEP 3: THE DATABASE ---
resource "helm_release" "postgres" {
  name       = "postgres"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "postgresql"
  namespace  = kubernetes_namespace.dev.metadata[0].name
  version    = "15.5.0" # Explicitly setting a stable chart version

  values = [
    file("../helm/postgres/values.dev.yaml")
  ]

  set {
    name  = "image.tag"
    value = "16" # This will pull the latest stable PG 16
  }
}

# --- STEP 4: THE SLACK SECRET ---
resource "kubernetes_secret" "alertmanager_slack" {
  metadata {
    name      = "alertmanager-slack-token"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }

  # Ensure var.slack_webhook_url is in your variables.tf and terraform.tfvars
  data = {
    token = var.slack_webhook_url
  }

  type = "Opaque"
}

# --- STEP 5: THE APPLICATION (TASK SERVICE) ---
resource "helm_release" "task_service" {
  name       = "task-service"
  chart      = "../helm/task-service" # Local path to your chart
  namespace  = kubernetes_namespace.dev.metadata[0].name

  values = [
    file("../helm/task-service/values.yaml"),
    file("../helm/task-service/values-dev.yaml")
  ]

  # Inject the version passed from the CLI/Jenkins
  set {
    name  = "image.tag"
    value = var.app_version
  }

  depends_on = [helm_release.postgres]
}