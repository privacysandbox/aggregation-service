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

package(default_visibility = ["//visibility:public"])

java_test(
    name = "AsgCapacityHandlerTest",
    srcs = ["AsgCapacityHandlerTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:AsgCapacityHandler",
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/aws:AutoScalingTasks",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:guava",
        "//java/external:mockito",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
    ],
)

java_test(
    name = "TerminatedInstanceHandlerTest",
    srcs = ["TerminatedInstanceHandlerTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:TerminatedInstanceHandler",
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/aws:AutoScalingTasks",
        "//java/external:aws_auto_scaling",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:guava",
        "//java/external:mockito",
    ],
)
