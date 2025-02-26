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
    name = "LocalJsonResultFileWriterTest",
    srcs = ["LocalJsonResultFileWriterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/json",
        "//java/com/google/aggregate/protocol/avro:avro_results_schema_supplier",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "LocalPrivacyBudgetExhaustedInfoWriterTest",
    srcs = ["LocalPrivacyBudgetExhaustedInfoWriterTest.java"],
    data = [
        "//javatests/com/google/aggregate/adtech/worker/writer/json/test:expected_privacy_budget_exhausted_debugging_info.json",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model:privacy_budget_debugging_model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/writer:privacy_budget_exhausted_info_writer",
        "//java/com/google/aggregate/adtech/worker/writer/json:local_privacy_budget_exhausted_writer",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:jimfs",
    ],
)
