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

# get aggregation service release version
VERSION=$(cat ../../VERSION)

# build jar artifacts with release version suffix and publish to S3
# using ENV variables JARS_PUBLISH_BUCKET and JARS_PUBLISH_BUCKET_PATH for publish destination
bazel run //terraform/aws:aws_change_handler_lambda_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION
bazel run //terraform/aws:asg_capacity_handler_lambda_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION
bazel run //terraform/aws:terminated_instance_handler_lambda_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION
bazel run //terraform/aws:aws_api_gateway_frontend_handler_lambda_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION
bazel run //terraform/aws:aws_frontend_cleanup_handler_lambda_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION
bazel run //terraform/aws:local_testing_tool_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION

bazel run //terraform/aws:privacy_budget_unit_extraction_tool_release \
--//terraform/aws:bucket_flag=$JARS_PUBLISH_BUCKET --//terraform/aws:bucket_path_flag=$JARS_PUBLISH_BUCKET_PATH \
-- --version=$VERSION
