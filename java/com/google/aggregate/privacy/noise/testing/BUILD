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

load("@rules_java//java:defs.bzl", "java_library", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "noise_tester_proto",
    srcs = ["noise_tester.proto"],
    deps = ["//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_proto"],
)

java_proto_library(
    name = "noise_tester_java_proto",
    deps = [
        ":noise_tester_proto",
    ],
)

java_library(
    name = "testing",
    srcs = [
        "ConstantNoiseModule.java",
        "FakeNoiseApplierSupplier.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":noise_tester_java_proto",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)
