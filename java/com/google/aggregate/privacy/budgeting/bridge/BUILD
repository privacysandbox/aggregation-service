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
    name = "privacy_budgeting_service_bridge",
    srcs = [
        "PrivacyBudgetingServiceBridge.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:guava",
    ],
)

java_library(
    name = "http_privacy_budgeting_service_bridge",
    srcs = [
        "HttpPrivacyBudgetingServiceBridge.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":privacy_budgeting_service_bridge",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:guava",
        "//java/external:javax_inject",
        "//java/external:model_privacy_budgeting",
    ],
)

java_library(
    name = "unlimited_privacy_budgeting_service_bridge",
    srcs = [
        "UnlimitedPrivacyBudgetingServiceBridge.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":privacy_budgeting_service_bridge",
        "//java/external:guava",
    ],
)

java_library(
    name = "fake_privacy_budgeting_service_bridge",
    testonly = True,
    srcs = [
        "FakePrivacyBudgetingServiceBridge.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":privacy_budgeting_service_bridge",
        "//java/external:guava",
    ],
)
