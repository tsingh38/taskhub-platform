variable "prometheus_chart_version" {
  type    = string
  default = "56.6.2"
}

variable "postgres_chart_version" {
  type    = string
  default = "15.5.21"
}

variable "postgres_image_tag" {
  type    = string
  default = "16.4.0-debian-12-r0"
}

variable "app_version" {
  type        = string
  description = "Managed by Jenkins"
}

variable "slack_webhook_url" {
  type      = string
  sensitive = true
}

variable "kubeconfig_path" {
  type        = string
  description = "Path to kubeconfig file"
}