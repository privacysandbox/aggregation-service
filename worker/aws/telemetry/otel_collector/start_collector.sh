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

# Get terraform environment name and set it in collector workspace.
token="$(curl -s --fail-with-body -X PUT http://169.254.169.254/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 120")"
region="$(curl -s --fail-with-body http://169.254.169.254/latest/meta-data/placement/region -H "X-aws-ec2-metadata-token: ${token}")"
instance_id="$(curl -s --fail-with-body http://169.254.169.254/latest/meta-data/instance-id -H "X-aws-ec2-metadata-token: ${token}")"
tags="$(aws --region "${region}" ec2 describe-tags --filters Name=resource-id,Values="${instance_id}")"
tags_kv="$(echo "${tags}" | jq "[.Tags[] | {key:.Key, value:.Value}] | from_entries")"
env_name="$(echo "${tags_kv}" | jq -r ".environment")"
allowed_metrics="$(echo "${tags_kv}" | jq -r ".otel_metrics" | jq -r '.[]')"
allowed_spans="$(echo "${tags_kv}" | jq -r ".otel_spans" | jq -r '.[]')"
min_log_level="$(echo "${tags_kv}" | jq -r ".min_log_level")"

# Generate otel filter yaml file.
generate_filter_yaml_file(){
  allowed_values=$1
  output_yaml_file=$2
  # allowed_values can be empty due to the tag not set in terraform or it's empty.
  if [ -z "${allowed_values}" ];
  then
     values_list="- \n"
  else
    mapfile -t allowed_values_arr <<< "$allowed_values"
    values_list=""
    for value in "${allowed_values_arr[@]}"
    do
        values_list+="- $value \n"
    done
  fi
  echo -e "${values_list}" > "${output_yaml_file}"
}

generate_filter_yaml_file "${allowed_metrics}" /opt/otel/metrics.yaml
generate_filter_yaml_file "${allowed_spans}" /opt/otel/spans.yaml

declare COLLECTOR_IMAGE=public.ecr.aws/aws-observability/aws-otel-collector:v0.36.0
declare -r COLLECTOR_NAME="otel-collector"
declare -a DOCKER_FLAGS=(
  --detach
  --publish "127.0.0.1:4317:4317"
  --name "${COLLECTOR_NAME}"
  --volume /opt/otel/otel_collector_config.yaml:/otel_collector_config.yaml
  --volume /opt/otel/metrics.yaml:/metrics.yaml
  --volume /opt/otel/spans.yaml:/spans.yaml
  --user "$(id -u):$(id -g)"
  --network host
  --env "ENV_NAME=${env_name}"
  --env "MIN_LOG_LEVEL=${min_log_level}"
)

declare -a -r COLLECTOR_FLAGS=(
  "--config=/otel_collector_config.yaml"
)

docker run "${DOCKER_FLAGS[@]}" "${COLLECTOR_IMAGE}" "${COLLECTOR_FLAGS[@]}"
