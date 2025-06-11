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
    name = "validation",
    srcs = glob(["*.java"]),
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker:return_code",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/util:numerical_conversions_utils",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
    ],
)

java_library(
    name = "privacy_budget_key_validator",
    srcs = glob([
        "PrivacyBudgetKeyValidator.java",
        "ValidatorHelper.java",
    ]),
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
    ],
)
