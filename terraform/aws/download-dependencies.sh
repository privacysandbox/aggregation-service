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


mkdir -p jars

gcs_url="https://storage.googleapis.com/trusted-execution-aggregation-service-public-artifacts"

jars=(
  "AwsChangeHandlerLambda"
  "aws_apigateway_frontend"
  "AwsFrontendCleanupLambda"
  "AsgCapacityHandlerLambda"
  "TerminatedInstanceHandlerLambda"
)
version=`cat ../../VERSION`
for jar in "${jars[@]}"; do
  echo "Downloading ${jar}_${version}.jar ..."
  curl -o jars/${jar}_$version.jar ${gcs_url}/${version}/${jar}_${version}.jar
done