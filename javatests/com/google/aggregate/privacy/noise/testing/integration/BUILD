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

package(default_visibility = ["//visibility:public"])

java_test(
    name = "NoisedSamplesGeneratorTest",
    srcs = ["NoisedSamplesGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/com/google/aggregate/privacy/noise/testing:noise_tester_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing/integration:dp_noise_tester_params_supplier",
        "//java/com/google/aggregate/privacy/noise/testing/integration:noised_samples_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:test_parameter_injector",
    ],
)

java_test(
    name = "DpNoiseTesterTest",
    srcs = ["DpNoiseTesterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/com/google/aggregate/privacy/noise/testing:noise_tester_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing/integration:dp_noise_tester_params_supplier",
        "//java/com/google/aggregate/privacy/noise/testing/integration:noise_tester",
        "//java/com/google/aggregate/privacy/noise/testing/integration:noised_samples_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:test_parameter_injector",
    ],
)
