terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.0.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.0.0"
    }
  }
}

provider "kubernetes" {
  config_path = "/root/.kube/config"
}

# DO NOT LEAVE THIS EMPTY
provider "helm" {
  kubernetes {
    config_path = "/root/.kube/config"
  }
}