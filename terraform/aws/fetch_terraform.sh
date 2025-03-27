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
VERSION="$(<"${WORKSPACE_DIR}"/VERSION)"
AMI_NAME="aggregation-service-enclave_${VERSION}"
AMI_OWNER="971056657085"
XCC_PRIMARY_GCP_PROJECT_NUMBER="44970611175"
XCC_PRIMARY_GCP_PROJECT_ID="ps-aws-msmt-xcc-prd-g3p-svcacc"
XCC_PRIMARY_AWS_ACCOUNT="886436932340"
XCC_PRIMARY_ENV="a"
XCC_SECONDARY_GCP_PROJECT_NUMBER="926383725127"
XCC_SECONDARY_GCP_PROJECT_ID="prod-t2-agg-api-d799"
XCC_SECONDARY_AWS_ACCOUNT="717279703219"
XCC_SECONDARY_ENV="prod-b"
printf "Aggregation server version: %s\n" "${VERSION}"

# remove existing symlinks
rm -f "${WORK_DIR}"/environments/demo/release_params.auto.tfvars

# generate release_params.auto.tfvars to use prebuilt and published AMI for
# aggregation-service release version and use downloaded versioned prebuilt jars
cat <<EOT >"${WORK_DIR}"/environments/shared/release_params.auto.tfvars
# Generated from release version - using prebuilt AMI
# If you want use your self-built AMI, follow the steps at
# https://github.com/privacysandbox/aggregation-service/blob/main/docs/aws-aggregation-service.md#set-up-your-deployment-environment
# and change ami_owners to ["self"]
ami_name = "${AMI_NAME}"
ami_owners = ["${AMI_OWNER}"]

coordinator_configs = {
  "COORDINATOR_A" = {
    gcp_project_number = "${XCC_PRIMARY_GCP_PROJECT_NUMBER}"
    gcp_project_id     = "${XCC_PRIMARY_GCP_PROJECT_ID}"
    aws_account_id     = "${XCC_PRIMARY_AWS_ACCOUNT}"
    env_name           = "${XCC_PRIMARY_ENV}"
  },
  "COORDINATOR_B" = {
    gcp_project_number = "${XCC_SECONDARY_GCP_PROJECT_NUMBER}"
    gcp_project_id     = "${XCC_SECONDARY_GCP_PROJECT_ID}"
    aws_account_id     = "${XCC_SECONDARY_AWS_ACCOUNT}"
    env_name           = "${XCC_SECONDARY_ENV}"
  }
}

change_handler_lambda = "../../jars/AwsChangeHandlerLambda_${VERSION}.jar"
frontend_lambda = "../../jars/AwsApiGatewayFrontend_${VERSION}.jar"
sqs_write_failure_cleanup_lambda = "../../jars/AwsFrontendCleanupLambda_${VERSION}.jar"
asg_capacity_handler_lambda = "../../jars/AsgCapacityHandlerLambda_${VERSION}.jar"
terminated_instance_handler_lambda = "../../jars/TerminatedInstanceHandlerLambda_${VERSION}.jar"

EOT

# symlink release_params.auto.tfvars into demo folder
ln -s ../shared/release_params.auto.tfvars "${WORK_DIR}"/environments/demo/
