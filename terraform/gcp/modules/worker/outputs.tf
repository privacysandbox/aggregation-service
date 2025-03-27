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

output "worker_service_account_email" {
  value       = local.worker_service_account_email
  description = "The worker service account email to provide to coordinator."
}

output "worker_template" {
  value       = google_compute_instance_template.worker_instance_template
  description = "The worker google_compute_instance_template."
}

output "grpc_collector_internal_lb_endpoint" {
  value       = google_compute_forwarding_rule.collector.ip_address
  description = "VPC internal IP address of the OTel collector load balancer."
}
