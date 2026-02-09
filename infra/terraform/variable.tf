variable "slack_webhook_url" {
  description = "The URL for Slack notifications"
  type        = string
  sensitive   = true
}

variable "app_version" {
  type        = string
  description = "The Docker image tag to deploy"
}