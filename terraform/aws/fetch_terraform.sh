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

WORK_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
readonly WORKSPACE_DIR="${WORK_DIR}"/../..
CONTROL_PLANE_SHARED_LIBRARIES_VERSION=$(grep -oP 'COORDINATOR_VERSION = "\Kv([0-9]+).([0-9]+).([0-9]+)' "${WORKSPACE_DIR}"/WORKSPACE)
VERSION="$(<"${WORKSPACE_DIR}"/VERSION)"
AMI_NAME="aggregation-service-enclave_${VERSION}"
AMI_OWNER="971056657085"
printf "Aggregation server version: %s\n" "${VERSION}"
printf "Control plane shared version: %s\n" "${CONTROL_PLANE_SHARED_LIBRARIES_VERSION}"

# fetch based on tag
readonly CONTROL_PLANE_REPO_RELDIR=control-plane-shared-libraries
readonly CONTROL_PLANE_REPO_DIR="${WORK_DIR}/${CONTROL_PLANE_REPO_RELDIR}"
git clone https://github.com/privacysandbox/control-plane-shared-libraries "${CONTROL_PLANE_REPO_DIR}" || true
git -C "${CONTROL_PLANE_REPO_DIR}" checkout "${CONTROL_PLANE_SHARED_LIBRARIES_VERSION}"
git -C "${CONTROL_PLANE_REPO_DIR}" clean -df

# remove existing symlinks
rm -f "${WORK_DIR}"/{applications,modules,environments/shared,environments/demo}

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
# If you want use your self-built AMI, follow the README.md and change
# ami_owners to ["self"]
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
