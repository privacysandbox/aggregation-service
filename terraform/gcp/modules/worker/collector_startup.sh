#!/bin/bash

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
# Add an OpenTelemetry collector
wget https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v0.81.0/otelcol-contrib_0.81.0_linux_amd64.deb
sudo dpkg -i otelcol-contrib_0.81.0_linux_amd64.deb
systemctl stop otelcol-contrib.service

# Generate otel filter yaml file.
generate_filter_yaml_file(){
  # shellcheck disable=SC2034
  allowed_values=$1
  # shellcheck disable=SC2034
  output_yaml_file=$2
  # allowed_values can be empty due to the tag not set in terraform or it's empty.
  # shellcheck disable=SC2157
  if [ -z "$${allowed_values}" ];
  then
     values_list="- \n"
  else
    # shellcheck disable=SC2034
    IFS=',' read -r -a allowed_values_array <<< "$${allowed_values}"
    values_list=""
    # shellcheck disable=SC1083,SC2231
    for value in $${allowed_values_array[@]}
    do
        values_list+="- $value \n"
    done
  fi
  echo -e "$${values_list}" > "$${output_yaml_file}"
}

# shellcheck disable=SC2154
generate_filter_yaml_file "${otel_metrics}" /etc/otelcol-contrib/metrics.yaml
# shellcheck disable=SC2154
generate_filter_yaml_file "${otel_spans}" /etc/otelcol-contrib/spans.yaml

# Configure OpenTelemetry collector for GCP monitoring
# shellcheck disable=SC2154
cat > /etc/otelcol-contrib/config.yaml << COLLECTOR_CONFIG
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: :${collector_port}
processors:
  batch/traces:
    timeout: 1s
    send_batch_size: 1
  batch/metrics:
    timeout: 60s
  batch/logging:
    timeout: 60s
    send_batch_size: 1
  resource:
    attributes:
    - key: "custom_namespace"
      value: ${environment_name}
      action: upsert
  filter/spans:
    spans:
      include:
        match_type: strict
        span_names: \$${file:/etc/otelcol-contrib/spans.yaml}
  filter/metrics:
    metrics:
      include:
        match_type: strict
        metric_names: \$${file:/etc/otelcol-contrib/metrics.yaml}
  filter/logs:
    logs:
      include:
        severity_number:
          min: ${min_log_level}
exporters:
  googlecloud:
    metric:
      resource_filters:
        # configures all resources to be passed on to GCP
        # https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/googlecloudexporter/README.md
        - regex: .*
    log:
      default_log_name: /gcp/aggregate-service/logs/${environment_name}
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [resource, batch/traces, filter/spans]
      exporters: [googlecloud]
    metrics:
      receivers: [otlp]
      processors: [resource, batch/metrics, filter/metrics]
      exporters: [googlecloud]
    logs:
      receivers: [otlp]
      processors: [resource, batch/logging, filter/logs]
      exporters: [googlecloud]
COLLECTOR_CONFIG
systemctl start otelcol-contrib.service
