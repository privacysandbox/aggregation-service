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
    name = "AttributionReportingPrivacyBudgetKeyGeneratorTest",
    srcs = ["AttributionReportingPrivacyBudgetKeyGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator_module",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/attributionreporting:pbk_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_test(
    name = "AttributionReportingDebugPrivacyBudgetKeyGeneratorTest",
    srcs = ["AttributionReportingDebugPrivacyBudgetKeyGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator_module",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/attributionreportingdebug:pbk_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_test(
    name = "ProtectedAudiencePrivacyBudgetKeyGeneratorTest",
    srcs = ["ProtectedAudiencePrivacyBudgetKeyGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator_module",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/protectedaudience:pbk_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_test(
    name = "SharedStoragePrivacyBudgetKeyGeneratorTest",
    srcs = ["SharedStoragePrivacyBudgetKeyGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator_module",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/sharedstorage:pbk_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_test(
    name = "PrivacyBudgetKeyGeneratorFactoryTest",
    srcs = ["PrivacyBudgetKeyGeneratorFactoryTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator_module",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/attributionreporting:pbk_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/attributionreportingdebug:pbk_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/protectedaudience:pbk_generator",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator/sharedstorage:pbk_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_test(
    name = "PrivacyBudgetKeyGeneratorUtilTest",
    srcs = ["PrivacyBudgetKeyGeneratorUtilTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator_module",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)
