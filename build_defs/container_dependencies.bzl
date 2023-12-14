# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################################
# The following map contains container dependencies used by the `container_pull`
# bazel rules. Docker images under the corresponding repository will be pulled
# based on the image digest.
#
# Container dependencies:
#  - amazonlinux_2: The official Amazon Linux image; needed for reproducibly
#    building AL2 binaries (e.g. //cc/aws/proxy)
#  - aws_otel_collector: AWS OpenTelemetry Collector.
#  - java_base: Distroless image for running Java.
################################################################################

# Updated as of: 2023-11-22

CONTAINER_DEPS = {
    "amazonlinux_2": {
        "digest": "sha256:3edc68cf3de9902da9e6c81662c48e7f5b3dd1f304bccba56960db48e54e71e8",
        "registry": "index.docker.io",
        "repository": "amazonlinux",
    },
    "aws_otel_collector": {
        "digest": "sha256:2a6183f63e637b940584e8ebf5335bd9a2581ca16ee400e2e74b7b488825adb4",
        "registry": "public.ecr.aws",
        "repository": "aws-observability/aws-otel-collector",
    },
    "java_base": {
        "digest": "sha256:3147bb05aca27b48e7b9042444488a127e3f8a256c8534a277791375a1b105a9",
        "registry": "gcr.io",
        "repository": "distroless/java17-debian11",
    },
}
