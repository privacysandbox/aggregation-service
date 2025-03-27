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

module "adtech_setup" {
  source  = "../../modules/adtech_setup"
  project = var.project

  service_account_token_creator_list = var.service_account_token_creator_list

  deploy_service_account_email = var.deploy_service_account_email
  deploy_service_account_name  = var.deploy_service_account_name
  worker_service_account_email = var.worker_service_account_email
  worker_service_account_name  = var.worker_service_account_name

  artifact_repo_name     = var.artifact_repo_name
  artifact_repo_location = var.artifact_repo_location

  data_bucket_name       = var.data_bucket_name
  data_bucket_location   = var.data_bucket_location
  data_bucket_versioning = var.data_bucket_versioning

  deploy_sa_role_name = var.deploy_sa_role_name
  worker_sa_role_name = var.worker_sa_role_name
}
