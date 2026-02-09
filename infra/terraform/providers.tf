terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0" # Stable 2.x branch to avoid v3.x breaking changes
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.0" # Stable 2.x branch
    }
  }
}

provider "kubernetes" {
  config_path = "/root/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "/root/.kube/config"
  }
}