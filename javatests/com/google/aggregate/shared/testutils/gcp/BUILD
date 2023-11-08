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
    name = "pubsub_emulator",
    srcs = [
        "PubSubEmulatorContainer.java",
        "PubSubEmulatorContainerTestModule.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/external:gcp_api_gax",
        "//java/external:gcp_gax_grpc",
        "//java/external:gcp_pubsub",
        "//java/external:gcp_pubsub_v1",
        "//java/external:grpc_api",
        "//java/external:guice",
        "//java/external:testcontainers",
    ],
)

java_library(
    name = "spanner_emulator",
    srcs = [
        "SpannerEmulatorContainer.java",
        "SpannerEmulatorContainerTestModule.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/external:gcp_core",
        "//java/external:gcp_spanner",
        "//java/external:guice",
        "//java/external:testcontainers",
    ],
)
