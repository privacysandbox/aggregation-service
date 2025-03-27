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
variable "environment" {
  description = "Environment where this service is deployed (e.g. dev, prod)."
  type        = string
}

variable "project_id" {
  description = "GCP Project ID in which this module will be created."
  type        = string
}

variable "regions" {
  description = "Regions with Cloud NAT support."
  type        = set(string)
  validation {
    # This limit is based on the size of the IP range being dedicated to the
    # the subnets for the connectors. Not relevant if VPC connectors are not
    # being created.
    condition     = length(var.regions) <= 16
    error_message = "Only 16 or less number of regions is supported."
  }
}

variable "create_connectors" {
  description = "Whether to create Serverless VPC Access connectors in each region."
  type        = bool
}

variable "connector_machine_type" {
  description = "Machine type of the Serverless VPC Access connector."
  type        = string
}
