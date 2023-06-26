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


# Usage:
#
# source perfgate_helper.sh
# start_collector
# Run your application
# convert_metrics_file
# upload_metrics_to_perfgate

set -o errexit
# set -o xtrace

METRICS_DIR=$(mktemp --directory)
readonly METRICS_DIR
WORKSPACE=${WORKSPACE-$(git rev-parse --show-toplevel)}
declare -r WORKSPACE
declare -r OTEL_CONFIG_YAML="${WORKSPACE}/worker/aws/telemetry/otel_collector/otel_collector_config.yaml"
"${WORKSPACE}"/builders/tools/pre-commit
ARCH="$("${WORKSPACE}"/builders/tools/get-architecture)"
readonly ARCH

declare -r COLLECTOR_NAME="otel-collector"
function start_collector() {
  printf "metrics directory: %s\n" "${METRICS_DIR}"
  declare -r COLLECTOR_HOSTNAME="otel-collector-host"
  declare -r CONTAINER_CONFIG_YAML=/otel-collector-config-demo.yaml
  declare -r COLLECTOR_IMAGE="otel/opentelemetry-collector-contrib:latest"
  declare -a -r COLLECTOR_DOCKER_FLAGS=(
    --rm
    --detach
    --name "${COLLECTOR_NAME}"
    --hostname "${COLLECTOR_HOSTNAME}"
    --volume "${OTEL_CONFIG_YAML}:${CONTAINER_CONFIG_YAML}"
    --volume "${METRICS_DIR}":/data/
    --user "$(id -u):$(id -g)"
  )
  declare -a -r COLLECTOR_FLAGS=(
    "--config=${CONTAINER_CONFIG_YAML}"
  )

  docker run "${COLLECTOR_DOCKER_FLAGS[@]}" "${COLLECTOR_IMAGE}" "${COLLECTOR_FLAGS[@]}"
}

function stop_collector() {
    docker container stop "${COLLECTOR_NAME}"
}


# Load the perfgate_uploader app
declare -r PERFGATE_APP_PATH=chrome/privacy_sandbox/potassium_engprod/perfgate
declare -r PERFGATE_IMAGE=perfgate_uploader_gce_"${ARCH}"_image

function load_perfgate_uploader() {
  docker load --input "${PERFGATE_TAR_DIR}/${PERFGATE_IMAGE}".tar
}

# Load the perfgate_exporter app
declare -r EXPORTER_APP_PATH=chrome/privacy_sandbox/potassium_engprod/otel/exporters/perfgate_exporter
declare -r EXPORTER_IMAGE=perfgate_exporter_gce_"${ARCH}"_image

function load_perfgate_converter() {
  declare -r EXPORTER_TAR_DIR="${KOKORO_BLAZE_DIR}"/perfgate_exporter_tar/blaze-bin/"${EXPORTER_APP_PATH}"
  docker load --input "${EXPORTER_TAR_DIR}/${EXPORTER_IMAGE}".tar
}

declare -r METRICS_DATA_PATH=/data
declare -r SAMPLEBATCH_FILE="${METRICS_DATA_PATH}"/samplebatch_file.textproto
declare -r QSINPUT_FILE="${METRICS_DATA_PATH}"/qsinput_file.textproto

function convert_metrics_file() {
  # Run the exporter to convert metrics to perfgate protobuf
  chmod 644 "${METRICS_DIR}"/metrics.json
  declare -a -r EXPORTTER_DOCKER_FLAGS=(
    --rm
    --volume "${METRICS_DIR}:${METRICS_DATA_PATH}"
    --user "$(id -u):$(id -g)"
  )
  declare -a -r EXPORTER_FLAGS=(
    --otelmetrics_file "${METRICS_DATA_PATH}"/metrics.json
    --samplebatch_file "${SAMPLEBATCH_FILE}"
    --qsinput_file "${QSINPUT_FILE}"
  )
  docker run "${EXPORTTER_DOCKER_FLAGS[@]}" bazel/"${EXPORTER_APP_PATH}:${EXPORTER_IMAGE}" "${EXPORTER_FLAGS[@]}"
}

function upload_metrics_to_perfgate() {
  # Run the perfgate uploader to upload metrics to perfgate
  chmod 644 "${METRICS_DIR}"/*.textproto
  declare -a -r PERFGATE_DOCKER_FLAGS=(
    --rm
    --volume "${METRICS_DIR}:${METRICS_DATA_PATH}"
    --user "$(id -u):$(id -g)"
  )
  declare -a -r PERFGATE_FLAGS=(
    --quickstoreinput_file "${QSINPUT_FILE}"
    --samplebatch_file "${SAMPLEBATCH_FILE}"
  )
  docker run "${PERFGATE_DOCKER_FLAGS[@]}" bazel/"${PERFGATE_APP_PATH}:${PERFGATE_IMAGE}" "${PERFGATE_FLAGS[@]}"
}

# Leaving the below commented until we have the right ACLs
# load_perfgate_converter
# load_perfgate_uploader
