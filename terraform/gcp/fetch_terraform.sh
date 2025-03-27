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

# This script can be used standalone to fetch terraform scripts from
# the control-plane-shared-libraries repository at the release version
# the aggregation service release is using. This script is used by
# download_prebuilt_dependencies.sh

set -o errexit

WORK_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
readonly WORKSPACE_DIR="${WORK_DIR}"/../..
VERSION="$(<"${WORKSPACE_DIR}"/VERSION)"
CONTAINER_NAME_AND_TAG="worker_mp_gcp_prod:${VERSION}"
CONTAINER_REGISTRY_PATH="us-docker.pkg.dev/ps-msmt-published-artifacts/aggregation-service-container-artifacts"
printf "Aggregation server version: %s\n" "${VERSION}"

# remove existing symlinks
rm -f "${WORK_DIR}"/environments/demo/release_params.auto.tfvars

# generate release_params.auto.tfvars to use prebuilt and published AMI for
# aggregation-service release version and use downloaded versioned prebuilt jars
cat <<EOT >"${WORK_DIR}"/environments/shared/release_params.auto.tfvars
# Generated from release version - using prebuilt container image
# If you want use your self-built container image, follow the steps at
# https://github.com/privacysandbox/aggregation-service/blob/main/docs/gcp-aggregation-service.md#set-up-your-deployment-environment
# and change worker_image to your Google Artifact Registry path and tag
worker_image = "${CONTAINER_REGISTRY_PATH}/${CONTAINER_NAME_AND_TAG}"

frontend_service_jar = "../../jars/FrontendServiceHttpCloudFunction_${VERSION}.jar"
worker_scale_in_jar  = "../../jars/WorkerScaleInCloudFunction_${VERSION}.jar"

# Coordinator service accounts to impersonate for authorization and authentication
coordinator_a_impersonate_service_account = "a-opallowedusr@ps-msmt-coord-prd-g3p-svcacc.iam.gserviceaccount.com"
coordinator_b_impersonate_service_account = "b-opallowedusr@ps-prod-msmt-type2-e541.iam.gserviceaccount.com"

EOT

# symlink release_params.auto.tfvars into demo folder
ln -s ../shared/release_params.auto.tfvars "${WORK_DIR}"/environments/demo/
