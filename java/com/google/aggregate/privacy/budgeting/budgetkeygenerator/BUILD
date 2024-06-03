# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "privacy_budget_key_generator_module",
    srcs = [
        "PrivacyBudgetKeyGenerator.java",
        "PrivacyBudgetKeyGeneratorUtil.java",
        "VersionedPrivacyBudgetKeyGeneratorProvider.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_library(
    name = "privacy_budget_key_generator",
    srcs = [
        "PrivacyBudgetKeyGenerator.java",
        "PrivacyBudgetKeyGeneratorFactory.java",
        "PrivacyBudgetKeyGeneratorModule.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":privacy_budget_key_generator_module",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/attributionreporting:pbk_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/attributionreportingdebug:pbk_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/protectedaudience:pbk_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/sharedstorage:pbk_generator",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)
