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

load("@bazel_skylib//rules:common_settings.bzl", "string_flag")
load("@rules_pkg//:mappings.bzl", "pkg_files")
load("@rules_pkg//:pkg.bzl", "pkg_tar")
load("//build_defs:release.bzl", "gcs_jar_release")

package(default_visibility = ["//visibility:public"])

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

string_flag(
    name = "licenses_flag",
    build_setting_default = "aggregation-service",
    visibility = ["//visibility:public"],
)

pkg_files(
    name = "dist_jars",
    srcs = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/gcp:WorkerScaleInCloudFunctionDeploy",
        "//java/com/google/aggregate/adtech/worker/frontend/service/gcp:FrontendServiceHttpCloudFunctionDeploy",
    ],
    prefix = "jars",
)

gcs_jar_release(
    name = "worker_scale_in_cloud_function_release",
    artifact_base_name = "WorkerScaleInCloudFunction_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/autoscaling/app/gcp:WorkerScaleInCloudFunctionDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/gcp_worker_scale_in_cloud_function_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

gcs_jar_release(
    name = "frontend_service_http_cloud_function_release",
    artifact_base_name = "FrontendServiceHttpCloudFunction_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/frontend/service/gcp:FrontendServiceHttpCloudFunctionDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/gcp_frontend_service_http_cloud_function_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

gcs_jar_release(
    name = "local_testing_tool_release",
    artifact_base_name = "LocalTestingTool_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker:LocalTestingToolDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/gcp_local_testing_tool_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

gcs_jar_release(
    name = "privacy_budget_unit_extraction_tool_release",
    artifact_base_name = "PrivacyBudgetUnitExtractionTool_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/tools/privacybudgetutil/gcp:GcpPrivacyBudgetUnitExtractionDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/gcp_privacy_budget_unit_extraction_tool_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

pkg_tar(
    name = "operator_tar",
    srcs = [
        ":dist_jars",
        "//terraform/gcp/applications:applications_pkg",
        "//terraform/gcp/environments:operator_tar_environments",
        "//terraform/gcp/modules:modules_pkg",
        "//terraform/gcp/modules/shared:modules_shared_pkg",
        "//terraform/gcp/util_scripts:util_scripts_pkg",
    ],
    extension = "tgz",
    strip_prefix = "/",
)
