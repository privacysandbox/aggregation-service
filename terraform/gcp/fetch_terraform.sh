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

function get_coordinator_version() {
  local -r workspace_file="$1"
  local -r ver_assign="$(grep -o '^COORDINATOR_VERSION = "v.*"' "${workspace_file}")"
  local -r ver="${ver_assign#*\"}"
  printf "%s\n" "${ver%\"}"
}

WORK_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
readonly WORKSPACE_DIR="${WORK_DIR}"/../..
CONTROL_PLANE_SHARED_LIBRARIES_VERSION=$(get_coordinator_version "${WORKSPACE_DIR}"/WORKSPACE)
VERSION="$(<"${WORKSPACE_DIR}"/VERSION)"
CONTAINER_NAME_AND_TAG="worker_mp_gcp_prod:${VERSION}"
CONTAINER_REGISTRY_PATH="us-docker.pkg.dev/ps-msmt-published-artifacts/aggregation-service-container-artifacts"
printf "Aggregation server version: %s\n" "${VERSION}"
printf "Control plane shared version: %s\n" "${CONTROL_PLANE_SHARED_LIBRARIES_VERSION}"

# fetch based on tag
readonly CONTROL_PLANE_REPO_RELDIR=coordinator-services-and-shared-libraries
readonly CONTROL_PLANE_REPO_DIR="${WORK_DIR}/${CONTROL_PLANE_REPO_RELDIR}"
if [[ -d ${CONTROL_PLANE_REPO_DIR} ]] && ! [[ -r ${CONTROL_PLANE_REPO_DIR}/.git/config ]]; then
  rm -rf ${CONTROL_PLANE_REPO_DIR}
fi
if ! [[ -d ${CONTROL_PLANE_REPO_DIR} ]]; then
  git clone https://github.com/privacysandbox/coordinator-services-and-shared-libraries "${CONTROL_PLANE_REPO_DIR}" || true
fi
git -C "${CONTROL_PLANE_REPO_DIR}" checkout "${CONTROL_PLANE_SHARED_LIBRARIES_VERSION}"
git -C "${CONTROL_PLANE_REPO_DIR}" clean -df
git -C "${CONTROL_PLANE_REPO_DIR}" reset --hard HEAD
for patch in ${WORKSPACE_DIR}/build_defs/shared_libraries/*.patch; do
  git -C "${CONTROL_PLANE_REPO_DIR}" apply --reject --whitespace=fix "${patch}"
done

# remove existing symlinks
rm -rf "${WORK_DIR}"/{applications,modules,environments/shared,environments/demo,environments/adtech_setup/*.*_sample,environments/adtech_setup/adtech_setup.tf,environments/adtech_setup/adtech_setup_output.tf,environments/adtech_setup/adtech_setup_variables.tf}

# symlink terraform folders
mkdir -p "${WORK_DIR}"/environments
ln -s "${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/applications "${WORK_DIR}"/applications
ln -s "${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/modules "${WORK_DIR}"/modules
ln -s ../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/shared "${WORK_DIR}"/environments/shared
ln -s ../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/demo "${WORK_DIR}"/environments/demo
mkdir -p "${WORK_DIR}"/environments/adtech_setup
ln -s ../../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/adtech_setup/adtech_setup_output.tf "${WORK_DIR}"/environments/adtech_setup/adtech_setup_output.tf
ln -s ../../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/adtech_setup/adtech_setup_variables.tf "${WORK_DIR}"/environments/adtech_setup/adtech_setup_variables.tf
ln -s ../../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/adtech_setup/adtech_setup.auto.tfvars_sample "${WORK_DIR}"/environments/adtech_setup/adtech_setup.auto.tfvars_sample
ln -s ../../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/adtech_setup/adtech_setup.tf "${WORK_DIR}"/environments/adtech_setup/adtech_setup.tf
ln -s ../../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/gcp/environments/adtech_setup/main.tf_sample "${WORK_DIR}"/environments/adtech_setup/main.tf_sample
# generate release_params.auto.tfvars to use prebuilt and published AMI for
# aggregation-service release version and use downloaded versioned prebuilt jars
cat <<EOT >>"${WORK_DIR}"/environments/shared/release_params.auto.tfvars
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
