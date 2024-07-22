# Copyright 2024 Google LLC
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
    name = "extraction_utils",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/external:acai",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:avro",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_annotations",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:jackson_datatype_jsr310",
        "//java/external:javax_inject",
        "//java/external:jcommander",
        "//java/external:jimfs",
        "//java/external:slf4j",
        "//java/external:slf4j_simple",
    ],
)

java_library(
    name = "config",
    srcs = ["PrivacyBudgetUnitExtractionConfig.java"],
    deps = [
        "//java/external:guava",
        "//java/external:jcommander",
        "//java/external:slf4j",
    ],
)
