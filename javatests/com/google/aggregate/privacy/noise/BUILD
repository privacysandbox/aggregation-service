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

load("@rules_java//java:defs.bzl", "java_test")

package(default_visibility = ["//visibility:public"])

java_test(
    name = "NoisedAggregationRunnerImplTest",
    srcs = ["NoisedAggregationRunnerImplTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/model",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_test(
    name = "DpNoiseApplierTest",
    srcs = ["DpNoiseApplierTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/com/google/aggregate/privacy/noise/testing:noise_tester_java_proto",
        "//java/external:acai",
        "//java/external:differential_privacy",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_test(
    name = "DpNoiseParamsFactoryTest",
    srcs = ["DpNoiseParamsFactoryTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_test(
    name = "ThresholdSupplierTest",
    srcs = ["ThresholdSupplierTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
    ],
)
