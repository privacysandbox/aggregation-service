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
output "network" {
  description = "The created network self-link."
  value       = module.vpc_network.network_self_link
}

output "egress_internet_tag" {
  description = "Instance tag to enable route to the internet."
  value       = local.egress_internet_tag
}

output "connectors" {
  description = "Serverless VPC Access connector IDs by region."
  value = (
    length(module.serverless-connector) > 0 ?
    zipmap(tolist(var.regions), tolist(module.serverless-connector[0].connector_ids)) :
    null
  )
}
