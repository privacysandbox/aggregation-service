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
    name = "noised_samples_generator",
    srcs = ["NoisedSamplesGenerator.java"],
    deps = [
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/testing:noise_tester_java_proto",
        "//java/external:guice",
    ],
)

java_library(
    name = "noise_tester",
    srcs = [
        "DpNoiseTester.java",
    ],
    deps = [
        ":noised_samples_generator",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing:noise_tester_java_proto",
        "//java/external:differential_privacy",
        "//java/external:differential_privacy_statistical_utils",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_library(
    name = "dp_noise_tester_params_supplier",
    srcs = ["DpNoiseTesterParamsSupplier.java"],
    deps = ["//java/com/google/aggregate/privacy/noise/testing:noise_tester_java_proto"],
)
