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
        "NumericConversions.java",
        "OutputShardFileHelper.java",
        "ReportingOriginUtils.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker:return_code",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/external:clients_jobclient_model",
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
        "//java/external:javax_inject",
        "//java/external:operator_protos",
        "//java/external:scp_shared_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/jobclient:model",
        "@shared_libraries//java/com/google/scp/operator/shared/model",
    ],
)
