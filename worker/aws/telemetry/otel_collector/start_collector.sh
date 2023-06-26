#!/bin/bash
# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

docker load -i /opt/otel/aws_otel_image.tar

declare COLLECTOR_IMAGE=public.ecr.aws/aws-observability/aws-otel-collector:v0.28.0
declare -r COLLECTOR_NAME="otel-collector"
declare -a DOCKER_FLAGS=(
  --detach
  --publish "127.0.0.1:4317:4317"
  --name "${COLLECTOR_NAME}"
  --volume /opt/otel/otel_collector_config.yaml:/otel_collector_config.yaml
  --user "$(id -u):$(id -g)"
  --network host
)

declare -a -r COLLECTOR_FLAGS=(
  "--config=/otel_collector_config.yaml"
)

docker run "${DOCKER_FLAGS[@]}" "${COLLECTOR_IMAGE}" "${COLLECTOR_FLAGS[@]}"
