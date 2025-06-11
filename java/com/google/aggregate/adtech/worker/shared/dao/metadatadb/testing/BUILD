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
    name = "fake_metadata_db",
    srcs = ["FakeMetadataDb.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//protos/shared/backend/metadatadb:java_proto",
    ],
)

java_library(
    name = "fake_asg_instances_dao",
    srcs = ["FakeAsgInstancesDao.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/common",
        "//protos/shared/backend/asginstance:java_proto",
    ],
)

java_library(
    name = "job_generator",
    srcs = ["JobGenerator.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)
