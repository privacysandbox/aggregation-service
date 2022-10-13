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

java_library(
    name = "privacy_budget_converters",
    srcs = [
        "ConsumedPrivacyBudgetConverter.java",
        "PrivacyBudgetConverter.java",
        "PrivacyBudgetKeyConverter.java",
        "WireStorageConsumedPrivacyBudgetConverter.java",
        "WireStoragePrivacyBudgetConverter.java",
        "WireStoragePrivacyBudgetKeyConverter.java",
    ],
    deps = [
        "//java/com/google/aggregate/privacy/budgeting/model:privacy_budgeting_model",
        "//java/com/google/aggregate/privacy/budgeting/storage:privacy_budgeting_storage",
        "//java/external:guava",
    ],
)
