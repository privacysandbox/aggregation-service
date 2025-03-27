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

################################################################################
# Write parameters into cloud.
################################################################################

##### shared parameters start #####
module "shared_log_option" {
  count           = var.shared_parameter_values.log_option == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.shared_parameter_names.log_option
  parameter_value = var.shared_parameter_values.log_option
}

module "shared_cpu_thread_count" {
  count           = var.shared_parameter_values.cpu_thread_count == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.shared_parameter_names.cpu_thread_count
  parameter_value = var.shared_parameter_values.cpu_thread_count
}

module "shared_cpu_thread_pool_queue_cap" {
  count           = var.shared_parameter_values.cpu_thread_pool_queue_cap == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.shared_parameter_names.cpu_thread_pool_queue_cap
  parameter_value = var.shared_parameter_values.cpu_thread_pool_queue_cap
}

module "shared_io_thread_count" {
  count           = var.shared_parameter_values.io_thread_count == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.shared_parameter_names.io_thread_count
  parameter_value = var.shared_parameter_values.io_thread_count
}

module "shared_io_thread_pool_queue_cap" {
  count           = var.shared_parameter_values.io_thread_pool_queue_cap == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.shared_parameter_names.io_thread_pool_queue_cap
  parameter_value = var.shared_parameter_values.io_thread_pool_queue_cap
}
##### shared parameters start #####

##### JobClient parameters start #####
module "job_spanner_instance_name" {
  count           = var.job_client_parameter_values.job_spanner_instance_name == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.job_client_parameter_names.job_spanner_instance_name
  parameter_value = module.jobtable.instance_name
}

module "job_spanner_database_name" {
  count           = var.job_client_parameter_values.job_spanner_database_name == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.job_client_parameter_names.job_spanner_database_name
  parameter_value = module.jobtable.database_name
}

module "job_table_name" {
  count           = var.job_client_parameter_values.job_table_name == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.job_client_parameter_names.job_table_name
  parameter_value = var.job_client_parameter_values.job_table_name
}

module "job_queue_name" {
  count           = var.job_client_parameter_values.job_queue_name == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.job_client_parameter_names.job_queue_name
  parameter_value = module.jobqueue.queue_pubsub_topic_name
}
##### JobClient parameters end #####

##### CryptoClient parameters start #####
module "crypto_client_hpke_kem" {
  count           = var.crypto_client_parameter_values.hpke_kem == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.crypto_client_parameter_names.hpke_kem
  parameter_value = var.crypto_client_parameter_values.hpke_kem
}

module "crypto_client_hpke_kdf" {
  count           = var.crypto_client_parameter_values.hpke_kdf == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.crypto_client_parameter_names.hpke_kdf
  parameter_value = var.crypto_client_parameter_values.hpke_kdf
}

module "crypto_client_hpke_aead" {
  count           = var.crypto_client_parameter_values.hpke_aead == null ? 0 : 1
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = var.crypto_client_parameter_names.hpke_aead
  parameter_value = var.crypto_client_parameter_values.hpke_aead
}
##### CryptoClient parameters end #####
