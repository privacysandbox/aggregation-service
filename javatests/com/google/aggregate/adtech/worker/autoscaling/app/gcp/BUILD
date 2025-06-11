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

load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "WorkerScaleInHttpFunctionTest",
    srcs = [
        "WorkerScaleInHttpFunctionTest.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/gcp",
        "//java/external:acai",
        "//java/external:gcp_cloud_function",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:mockito",
        "@shared_libraries//java/com/google/scp/shared/api/model",
    ],
)

java_test(
    name = "WorkerScaleInRequestHandlerTest",
    srcs = [
        "WorkerScaleInRequestHandlerTest.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/gcp",
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/gcp",
        "//java/external:acai",
        "//java/external:gcp_cloud_function",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:mockito",
        "@shared_libraries//java/com/google/scp/shared/api/model",
    ],
)
