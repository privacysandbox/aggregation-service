/**
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

variable "environment" {
  type        = string
  description = "Description for the environment, e.g. dev, staging, production."
}

variable "coordinator_name" {
  type        = string
  description = "Name of the coordinator. Must be either 'COORDINATOR_A' or 'COORDINATOR_B'."
  validation {
    condition = contains([
      "COORDINATOR_A", "COORDINATOR_B"
    ], var.coordinator_name)
    error_message = "The 'coordinator_name' variable must be either 'COORDINATOR_A' or 'COORDINATOR_B'."
  }
}

variable "gcp_project_number" {
  type        = string
  description = "GCP Project number in which wip allowed service account was created in."
}

variable "workload_identity_pool_id" {
  type        = string
  description = "Workload Identity Pool to manage GCP access for operators."
}

variable "workload_identity_pool_provider_id" {
  type        = string
  description = "Workload Identity Pool Provider used to manage federation for AWS Account."
}

variable "sa_email" {
  type        = string
  description = "Service Account email used for WIP impersonation."
}
