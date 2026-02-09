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
  namespace  = "monitoring" # Hardcode this for the test
  version    = "56.6.2"     # Explicitly set the version to avoid the lookup panic

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

  # We use the values you already defined in your repo
  values = [
    file("../helm/postgres/values.dev.yaml")
  ]
}

# --- STEP 4: THE SLACK SECRET ---
# Why: This solves the "Git Rejected my Token" problem.
# We pull the token from a variable (defined in step 3 below).
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

# --- STEP 5: THE APPLICATION (TASK SERVICE) ---
resource "helm_release" "task_service" {
  name       = "task-service"
  chart      = "../helm/task-service"
  namespace  = kubernetes_namespace.dev.metadata[0].name

  values = [
    file("../helm/task-service/values.yaml"),
    file("../helm/task-service/values-dev.yaml")
  ]

  depends_on = [helm_release.postgres]
}