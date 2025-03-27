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

output "worker_managed_instance_group_name" {
  value       = google_compute_region_instance_group_manager.worker_instance_group.name
  description = "The worker managed instance group name."
}

output "autoscaler_name" {
  value       = google_compute_region_autoscaler.worker_autoscaler.name
  description = "The name of the worker autoscaler."
}
