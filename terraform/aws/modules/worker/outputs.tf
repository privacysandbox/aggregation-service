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

output "worker_template_id" {
  value = aws_launch_template.worker_template.id
}

output "worker_template_version" {
  value = aws_launch_template.worker_template.latest_version
}

output "worker_enclave_role_arn" {
  value = aws_iam_role.enclave_role.arn
}

output "worker_role_name" {
  value = aws_iam_role.enclave_role.name
}
