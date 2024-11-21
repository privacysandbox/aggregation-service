# Copyright 2023 Google LLC
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
    name = "otel_prod",
    srcs = glob(["*.java"]),
    javacopts = ["-Xep:Var:ERROR"],
    deps = [
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:opentelemetry_api",
        "//java/external:opentelemetry_context",
        "//java/external:opentelemetry_contrib_aws_xray",
        "//java/external:opentelemetry_exporter_logging",
        "//java/external:opentelemetry_exporter_logging_otlp",
        "//java/external:opentelemetry_exporter_otlp",
        "//java/external:opentelemetry_sdk",
        "//java/external:opentelemetry_sdk_common",
        "//java/external:opentelemetry_sdk_metrics",
        "//java/external:opentelemetry_sdk_testing",
        "//java/external:opentelemetry_sdk_trace",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)
