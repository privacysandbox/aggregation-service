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

CONTROL_PLANE_SHARED_LIBRARIES_VERSION=$(grep -oP 'COORDINATOR_VERSION = "\Kv([0-9]+).([0-9]+).([0-9]+)' ../../WORKSPACE)
VERSION=$(cat ../../VERSION)
WORK_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# fetch based on tag
cd $WORK_DIR && git clone https://github.com/privacysandbox/control-plane-shared-libraries || true
cd control-plane-shared-libraries && git checkout ${CONTROL_PLANE_SHARED_LIBRARIES_VERSION} && git clean -df
cd $WORK_DIR

# remove existing symlinks
rm $WORK_DIR/applications $WORK_DIR/modules \
  $WORK_DIR/environments/shared $WORK_DIR/environments/demo || true

# symlink terraform folders
mkdir -p $WORK_DIR/environments
ln -s control-plane-shared-libraries/operator/terraform/aws/applications $WORK_DIR/applications
ln -s control-plane-shared-libraries/operator/terraform/aws/modules $WORK_DIR/modules
ln -s ../control-plane-shared-libraries/operator/terraform/aws/environments/shared $WORK_DIR/environments/shared
ln -s ../control-plane-shared-libraries/operator/terraform/aws/environments/demo $WORK_DIR/environments/demo

# generate release_params.auto.tfvars to use prebuilt and published AMI for
# aggregation-service release version and use downloaded versioned prebuilt jars
cat <<EOT >> environments/shared/release_params.auto.tfvars
# Generated from release version - using prebuilt AMI
# If you want use your self-built AMI, follow the README.md and change
# ami_owners to ["self"]
ami_name = "aggregation-service-enclave_$VERSION"
ami_owners = ["971056657085"]

change_handler_lambda = "../../jars/AwsChangeHandlerLambda_${VERSION}.jar"
frontend_lambda = "../../jars/AwsApiGatewayFrontend_${VERSION}.jar"
sqs_write_failure_cleanup_lambda = "../../jars/AwsFrontendCleanupLambda_${VERSION}.jar"
asg_capacity_handler_lambda = "../../jars/AsgCapacityHandlerLambda_${VERSION}.jar"
terminated_instance_handler_lambda = "../../jars/TerminatedInstanceHandlerLambda_${VERSION}.jar"

EOT

# symlink release_params.auto.tfvars into demo folder
ln -s ../shared/release_params.auto.tfvars environments/demo/
