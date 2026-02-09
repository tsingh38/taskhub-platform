terraform {
  required_version = ">= 1.0.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12.0"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config" # Points to your K3s/K8s config on the new server
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}