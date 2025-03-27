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
# Variables for parameter names and values.
################################################################################

variable "shared_parameter_names" {
  description = "Parameter names shared in CPIO."
  type = object({
    log_option                = string
    cpu_thread_count          = string
    cpu_thread_pool_queue_cap = string
    io_thread_count           = string
    io_thread_pool_queue_cap  = string
  })
  default = {
    log_option                = "cmrt_sdk_log_option"
    cpu_thread_count          = "cmrt_sdk_shared_cpu_thread_count"
    cpu_thread_pool_queue_cap = "cmrt_sdk_shared_cpu_thread_pool_queue_cap"
    io_thread_count           = "cmrt_sdk_shared_io_thread_count"
    io_thread_pool_queue_cap  = "cmrt_sdk_shared_io_thread_pool_queue_cap"
  }
}

variable "shared_parameter_values" {
  description = "Parameter value shared in CPIO."
  type = object({
    log_option                = string
    cpu_thread_count          = string
    cpu_thread_pool_queue_cap = string
    io_thread_count           = string
    io_thread_pool_queue_cap  = string
  })
  default = {
    log_option                = null
    cpu_thread_count          = null
    cpu_thread_pool_queue_cap = null
    io_thread_count           = null
    io_thread_pool_queue_cap  = null
  }
}

variable "job_client_parameter_names" {
  description = "Parameter names for job client."
  type = object({
    job_queue_name            = string
    job_table_name            = string
    job_spanner_instance_name = string
    job_spanner_database_name = string
  })
  default = {
    job_queue_name            = "cmrt_sdk_job_client_job_queue_name"
    job_table_name            = "cmrt_sdk_job_client_job_table_name"
    job_spanner_instance_name = "cmrt_sdk_gcp_job_client_spanner_instance_name"
    job_spanner_database_name = "cmrt_sdk_gcp_job_client_spanner_database_name"
  }
}

variable "job_client_parameter_values" {
  description = "Parameter value for job client."
  type = object({
    job_queue_name            = string
    job_table_name            = string
    job_spanner_instance_name = string
    job_spanner_database_name = string
  })
  default = {
    job_queue_name            = "JobQueue"
    job_table_name            = "JobTable"
    job_spanner_instance_name = "job-spanner"
    job_spanner_database_name = "job-database"
  }
}

variable "crypto_client_parameter_names" {
  description = "Parameter names for crypto client."
  type = object({
    hpke_kem  = string
    hpke_kdf  = string
    hpke_aead = string
  })
  default = {
    hpke_kem  = "cmrt_sdk_crypto_client_hpke_kem"
    hpke_kdf  = "cmrt_sdk_crypto_client_hpke_kdf"
    hpke_aead = "cmrt_sdk_crypto_client_hpke_aead"
  }
}

variable "crypto_client_parameter_values" {
  description = "Parameter value for crypto client."
  type = object({
    hpke_kem  = string
    hpke_kdf  = string
    hpke_aead = string
  })
  default = {
    hpke_kem  = null
    hpke_kdf  = null
    hpke_aead = null
  }
}
