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

load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "factories",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/injection/modules",
        "//java/com/google/aggregate/adtech/worker/frontend/serialization",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws:aws_helpers",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/external:guava",
        "//java/external:guice",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//shared/protos/api/v1:java_proto",
    ],
)
