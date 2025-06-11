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
    name = "GetMessageCountTaskTest",
    srcs = [
        "GetMessageCountTaskTest.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/aws:AutoScalingTasks",
        "//java/external:aws_sqs",
        "//java/external:mockito",
    ],
)

java_test(
    name = "SetDesiredCapacityTaskTest",
    srcs = [
        "SetDesiredCapacityTaskTest.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/aws:AutoScalingTasks",
        "//java/external:aws_auto_scaling",
        "//java/external:mockito",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
    ],
)

java_test(
    name = "ManageTerminatedInstanceTaskTest",
    srcs = [
        "ManageTerminatedInstanceTaskTest.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/tasks/aws:AutoScalingTasks",
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/common",
        "//java/com/google/aggregate/adtech/worker/shared/testing",
        "//java/external:aws_auto_scaling",
        "//java/external:mockito",
    ],
)
