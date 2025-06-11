# Copyright 2025 Google LLC
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

load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@rules_pkg//:pkg.bzl", "pkg_zip")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "gcp",
    srcs = glob([
        "*.java",
    ]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/frontend/service/model",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/gcp",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/gcp",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/gcp",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:gcp_cloud_function",
        "//java/external:google_api_client",
        "//java/external:google_http_client_gson",
        "//java/external:google_oauth_client",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:protobuf_java",
        "//java/external:protobuf_java_util",
        "//java/external:slf4j",
        "//java/external:slf4j_simple",
        "//protos/frontend/api/v1:java_proto",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//cc/public/cpio/proto/job_service/v1:job_service_java_proto",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/api/util",
        "@shared_libraries//java/com/google/scp/shared/gcp/util",
        "@shared_libraries//java/com/google/scp/shared/mapper",
    ],
)

java_binary(
    name = "FrontendServiceHttpCloudFunction",
    resources = [
        "//licenses/THIRD_PARTY_NOTICES/gcp_frontend_service_http_cloud_function_release:licenses",
    ],
    runtime_deps = [
        ":gcp",
    ],
)

alias(
    name = "FrontendServiceHttpCloudFunctionDeploy",
    actual = ":FrontendServiceHttpCloudFunction_deploy.jar",
)

pkg_zip(
    name = "FrontendServiceHttpCloudFunctionDeploy_zip",
    srcs = [
        ":FrontendServiceHttpCloudFunction_deploy.jar",
    ],
)
