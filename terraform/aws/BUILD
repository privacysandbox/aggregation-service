load("@bazel_skylib//rules:common_settings.bzl", "string_flag")

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
load("//build_defs:release.bzl", "s3_jar_release")

string_flag(
    name = "bucket_flag",
    build_setting_default = "aggregation-service-published-artifacts",
    visibility = ["//visibility:public"],
)

string_flag(
    name = "bucket_path_flag",
    build_setting_default = "aggregation-service",
    visibility = ["//visibility:public"],
)

s3_jar_release(
    name = "aws_change_handler_lambda_release",
    artifact_base_name = "AwsChangeHandlerLambda_{VERSION}.jar",
    jar_target = "@shared_libraries//java/com/google/scp/operator/frontend/service/aws:AwsChangeHandlerLambdaDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "asg_capacity_handler_lambda_release",
    artifact_base_name = "AsgCapacityHandlerLambda_{VERSION}.jar",
    jar_target = "@shared_libraries//java/com/google/scp/operator/autoscaling/app/aws:AsgCapacityHandlerLambdaDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "terminated_instance_handler_lambda_release",
    artifact_base_name = "TerminatedInstanceHandlerLambda_{VERSION}.jar",
    jar_target = "@shared_libraries//java/com/google/scp/operator/autoscaling/app/aws:TerminatedInstanceHandlerLambdaDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "aws_api_gateway_frontend_handler_lambda_release",
    artifact_base_name = "AwsApiGatewayFrontend_{VERSION}.jar",
    jar_target = "@shared_libraries//java/com/google/scp/operator/frontend:AwsApiGatewayFrontendDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "aws_frontend_cleanup_handler_lambda_release",
    artifact_base_name = "AwsFrontendCleanupLambda_{VERSION}.jar",
    jar_target = "@shared_libraries//java/com/google/scp/operator/frontend/service/aws:AwsFrontendCleanupLambdaDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "local_testing_tool_release",
    artifact_base_name = "LocalTestingTool_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker:LocalTestingToolDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "privacy_budget_unit_extraction_tool_release",
    artifact_base_name = "PrivacyBudgetUnitExtraction_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/tools/privacybudgetutil/aws:AwsPrivacyBudgetUnitExtractionDeploy",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)
