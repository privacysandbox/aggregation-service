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
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/gcp",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/gcp",
        "//java/external:gcp_cloud_function",
        "//java/external:gcp_compute",
        "//java/external:gcp_spanner",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:slf4j",
        "//java/external:slf4j_simple",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/gcp/util",
    ],
)

java_binary(
    name = "WorkerScaleInCloudFunction",
    resources = [
        "//licenses/THIRD_PARTY_NOTICES/gcp_worker_scale_in_cloud_function_release:licenses",
    ],
    runtime_deps = [
        ":gcp",
    ],
)

alias(
    name = "WorkerScaleInCloudFunctionDeploy",
    actual = ":WorkerScaleInCloudFunction_deploy.jar",
)

pkg_zip(
    name = "WorkerScaleInCloudFunctionDeploy_zip",
    srcs = [
        ":WorkerScaleInCloudFunction_deploy.jar",
    ],
)
