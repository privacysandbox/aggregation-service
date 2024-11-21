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

# Updated as of: 2024-11-18

CONTAINER_DEPS = {
    "amazonlinux_2": {
        "digest": "sha256:5ff0f1426f7d4dce857000e03f062c4d29fdb07b067e42c7104544883555c71d",
        "registry": "index.docker.io",
        "repository": "amazonlinux",
    },
    "aws_otel_collector": {
        "digest": "sha256:2a6183f63e637b940584e8ebf5335bd9a2581ca16ee400e2e74b7b488825adb4",
        "registry": "public.ecr.aws",
        "repository": "aws-observability/aws-otel-collector",
    },
    "java_base": {
        "digest": "sha256:587ce66b08faea2e2e1568d6bb6c5fd6b085909621f4c14762206d687ff7d202",
        "registry": "gcr.io",
        "repository": "distroless/java17-debian11",
    },
    "java_base_gcp": {
        "digest": "sha256:da08cd3597b09c8073a5f4c3f1d226826fc35f16643c3243a6f8c85a2ee3efbf",
        "registry": "gcr.io",
        "repository": "distroless/java17-debian12",
    },
}
