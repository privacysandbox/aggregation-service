#!/bin/sh
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

# Run this script with bash to download prebuilt artifacts published with every release
# bash download_prebuilt_dependencies.sh

SCRIPT_LOCATION=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Fetch terraform scripts from control-plane-shared-libraries repository at {VERSION}
# the current aggregation-service release depends on
source "${SCRIPT_LOCATION}/fetch_terraform.sh"

# Create folder where we download prebuilt jar artifacts to, ok if already existing
mkdir -p jars

# URL where prebuilt jar artifacts are published and publicly accessible
S3_URL="https://aggregation-service-published-artifacts.s3.amazonaws.com"

# Prebuilt jars to download - _{VERSION} will be postfix added
jars=(
  "AwsChangeHandlerLambda"
  "AwsApiGatewayFrontend"
  "AwsFrontendCleanupLambda"
  "AsgCapacityHandlerLambda"
  "TerminatedInstanceHandlerLambda"
  "LocalTestingTool"
)

# Download jars
for jar in "${jars[@]}"; do
  echo "Downloading ${jar}_${VERSION}.jar ..."
  curl -f -o jars/${jar}_$VERSION.jar ${S3_URL}/aggregation-service/${VERSION}/${jar}_${VERSION}.jar
done
