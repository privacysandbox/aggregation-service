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

output "frontend_api_id" {
  value = module.frontend.frontend_api_id
}

output "create_job_endpoint" {
  value = module.frontend.create_job_endpoint
}

output "get_job_endpoint" {
  value = module.frontend.get_job_endpoint
}

output "notifications_sns_topic_arn" {
  value       = one(module.notifications[*].notifications_sns_topic_arn)
  description = "The ARN of the SNS notifications topic."
}

output "worker_role_name" {
  value       = module.worker_service.worker_role_name
  description = "Name of the role used by the worker service."
}
