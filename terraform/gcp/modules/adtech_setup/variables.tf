/**
 * Copyright 2023 Google LLC
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

variable "project" {
  description = "GCP Project ID in which this module will be created."
  type        = string
}

variable "deploy_service_account_email" {
  description = "Fully qualified name of existing deploy service account to assign permissions to."
  type        = string
  default     = ""
}

variable "deploy_service_account_name" {
  description = "Name of deploy service account to create and assign permissions to."
  type        = string
  default     = ""
}

variable "worker_service_account_email" {
  description = "Fully qualified name of existing worker service account to assign permissions to."
  type        = string
  default     = ""
}

variable "worker_service_account_name" {
  description = "Name of worker service account to create and assign permissions to."
  type        = string
  default     = ""
}

variable "service_account_token_creator_list" {
  description = "List of user accounts allowed to create service account tokens."
  type        = list(string)
}

variable "artifact_repo_name" {
  description = "The artifact registry repository name for adtech Services images."
  type        = string
}

variable "artifact_repo_location" {
  description = "The artifact registry repository location for adtech Services images."
  type        = string
}

variable "data_bucket_name" {
  description = "The bucket name for storing report batch files, domain files and summary reports."
  type        = string
}

variable "data_bucket_versioning" {
  description = "The versioning option for data bucket. Default is true (enabled)."
  type        = bool
}

variable "data_bucket_location" {
  description = "The bucket location for data bucket."
  type        = string
  default     = "us"
}

variable "deploy_sa_role_name" {
  description = "The custom role name for deploy service account."
  type        = string
}

variable "worker_sa_role_name" {
  description = "The custom role name for worker service account."
  type        = string
}
