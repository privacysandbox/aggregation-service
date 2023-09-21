#!/bin/bash
# Copyright 2022 Google LLC
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
AMI_NAME="aggregation-service-enclave_${VERSION}"
AMI_OWNER="971056657085"
printf "Aggregation server version: %s\n" "${VERSION}"
printf "Control plane shared version: %s\n" "${CONTROL_PLANE_SHARED_LIBRARIES_VERSION}"

# fetch based on tag
readonly CONTROL_PLANE_REPO_RELDIR=coordinator-services-and-shared-libraries
readonly CONTROL_PLANE_REPO_DIR="${WORK_DIR}/${CONTROL_PLANE_REPO_RELDIR}"
if [[ -d ${CONTROL_PLANE_REPO_DIR} ]] && ! [[ -r ${CONTROL_PLANE_REPO_DIR}/.git/config ]]; then
  rm -rf ${CONTROL_PLANE_REPO_DIR}
fi
# cleanup old control plane shared libraries repo
if [[ -d control-plane-shared-libraries ]]; then
  rm -rf control-plane-shared-libraries
fi
if ! [[ -d ${CONTROL_PLANE_REPO_DIR} ]]; then
  git clone https://github.com/privacysandbox/coordinator-services-and-shared-libraries "${CONTROL_PLANE_REPO_DIR}" || true
fi
git -C "${CONTROL_PLANE_REPO_DIR}" fetch origin "${CONTROL_PLANE_SHARED_LIBRARIES_VERSION}"
git -C "${CONTROL_PLANE_REPO_DIR}" checkout FETCH_HEAD
git -C "${CONTROL_PLANE_REPO_DIR}" clean -df

# remove existing symlinks
rm -rf "${WORK_DIR}"/{applications,modules,environments/shared,environments/demo}

# symlink terraform folders
mkdir -p "${WORK_DIR}"/environments
ln -s "${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/aws/applications "${WORK_DIR}"/applications
ln -s "${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/aws/modules "${WORK_DIR}"/modules
ln -s ../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/aws/environments/shared "${WORK_DIR}"/environments/shared
ln -s ../"${CONTROL_PLANE_REPO_RELDIR}"/operator/terraform/aws/environments/demo "${WORK_DIR}"/environments/demo

# generate release_params.auto.tfvars to use prebuilt and published AMI for
# aggregation-service release version and use downloaded versioned prebuilt jars
cat <<EOT >>"${WORK_DIR}"/environments/shared/release_params.auto.tfvars
# Generated from release version - using prebuilt AMI
# If you want use your self-built AMI, follow the steps at
# https://github.com/privacysandbox/aggregation-service/blob/main/docs/aws-aggregation-service.md#set-up-your-deployment-environment
# and change ami_owners to ["self"]
ami_name = "${AMI_NAME}"
ami_owners = ["${AMI_OWNER}"]

change_handler_lambda = "../../jars/AwsChangeHandlerLambda_${VERSION}.jar"
frontend_lambda = "../../jars/AwsApiGatewayFrontend_${VERSION}.jar"
sqs_write_failure_cleanup_lambda = "../../jars/AwsFrontendCleanupLambda_${VERSION}.jar"
asg_capacity_handler_lambda = "../../jars/AsgCapacityHandlerLambda_${VERSION}.jar"
terminated_instance_handler_lambda = "../../jars/TerminatedInstanceHandlerLambda_${VERSION}.jar"

EOT

# symlink release_params.auto.tfvars into demo folder
ln -s ../shared/release_params.auto.tfvars "${WORK_DIR}"/environments/demo/
