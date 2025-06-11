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
    name = "JobRequestIdLengthValidatorTest",
    srcs = ["JobRequestIdLengthValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:google_truth",
        "//java/external:guava",
        "//protos/shared/backend:java_proto",
    ],
)

java_test(
    name = "JobRequestIdCharactersValidatorTest",
    srcs = ["JobRequestIdCharactersValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:google_truth",
        "//java/external:guava",
        # Needs snakeyaml dependency here to choose the right snakeyaml version.
        "//java/external:snakeyaml",
        "//java/external:test_parameter_injector",
        "//protos/shared/backend:java_proto",
    ],
)
