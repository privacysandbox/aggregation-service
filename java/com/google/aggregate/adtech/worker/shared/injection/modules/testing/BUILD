# Copyright 2025 Google LLC
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
    name = "fake_data_module",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:fake_metadata_db",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/com/google/aggregate/adtech/worker/shared/testing",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:guava",
        "//java/external:guice",
    ],
)
