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
    name = "in_memory_privacy_budget_manager",
    testonly = True,
    srcs = ["InMemoryPrivacyBudgetManager.java"],
    deps = [
        "//java/com/google/aggregate/privacy/budgeting:privacy_budget_manager",
        "//java/com/google/aggregate/privacy/budgeting/model:privacy_budgeting_model",
        "//java/external:guava",
    ],
)
