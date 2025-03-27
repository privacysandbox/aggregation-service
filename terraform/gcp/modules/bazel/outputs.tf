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

output "bazel_bin" {
  value       = data.external.bazel_bin.result["path"]
  description = "The absolute path pointing to the bazel-bin directory"
}

output "workspace" {
  value       = data.external.workspace.result["path"]
  description = "The absolute path pointing to the root of this bazel workspace (the git repo)"
}
