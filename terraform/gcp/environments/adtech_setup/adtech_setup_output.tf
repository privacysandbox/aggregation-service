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

output "worker_service_account_email" {
  value       = module.adtech_setup.worker_service_account_email
  description = "The worker service account email used by adtech services workers."
}

output "deploy_service_account_email" {
  value       = module.adtech_setup.deploy_service_account_email
  description = "The deploy service account email used to deploy adtech services."
}

output "data_bucket_name" {
  value       = module.adtech_setup.data_bucket_name
  description = "The bucket name for storing reports and outputs."
}
