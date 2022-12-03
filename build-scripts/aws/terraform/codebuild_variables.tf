/**
 * Copyright 2022 Google LLC
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

########################
# DO NOT EDIT MANUALLY #
########################

# This file is meant to be shared across all environments (either copied or
# symlinked). In order to make the upgrade process easier, this file should not
# be modified for environment-specific customization.

# Customization should occur either in a .tfvars file setting variable values or
# in a separate .tf file.

variable "region" {
  type        = string
  description = "Region to deploy services to and publish artifacts"
}

variable "compute_type" {
  type        = string
  description = "CodeBuild compute type to use for build."
  default     = "BUILD_GENERAL1_MEDIUM"
}

variable "build_artifacts_output_bucket" {
  type        = string
  description = "Bucket to output build artifacts"
}

variable "force_destroy_build_artifacts_output_bucket" {
  type        = bool
  description = "Force delete bucket for output build artifacts"
  default     = false
}
variable "ecr_repo_name" {
  type        = string
  description = "ECR repository name for build container"
  default     = "bazel-build-container"
}

variable "github_personal_access_token" {
  type        = string
  description = "Github Personal Access Token without permissions"
}

variable "aggregation_service_github_repo" {
  type        = string
  description = "Aggregation Service Github repository location"
  default     = "https://github.com/privacysandbox/aggregation-service"
}

variable "aggregation_service_github_repo_branch" {
  type        = string
  description = "Aggregation Service Github repository branch"
  default     = ""
}
