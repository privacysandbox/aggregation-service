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

load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "util",
    srcs = [
        "DebugSupportHelper.java",
        "JobResultHelper.java",
        "JobUtils.java",
        "OutputShardFileHelper.java",
        "ReportingOriginUtils.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":numerical_conversions_utils",
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker:return_code",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
        "//java/external:javax_inject",
        "//java/external:scp_shared_proto",
        "//java/external:slf4j",
        "//protos/shared/backend:java_proto",
    ],
)

java_library(
    name = "numerical_conversions_utils",
    srcs = [
        "NumericConversions.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
    ],
)
