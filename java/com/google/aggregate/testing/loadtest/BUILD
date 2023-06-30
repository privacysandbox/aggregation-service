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

load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "loadtest_runner",
    classpath_resources = ["//java/com/google/aggregate/testing/loadtest:slf4jconfig"],
    main_class = "com.google.aggregate.testing.loadtest.LoadTestRunner",
    runtime_deps = [
        ":loadtest",
    ],
)

java_library(
    name = "loadtest",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/aggregate/privacy/budgeting/bridge:http_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/external:aws_auth",
        "//java/external:aws_regions",
        "//java/external:aws_sts",
        "//java/external:clients_configclient",
        "//java/external:clients_configclient_aws",
        "//java/external:clients_configclient_aws_operator",
        "//java/external:clients_configclient_operator",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:clients_distributed_privacy_budgeting_aws",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jcommander",
        "//java/external:scp_credsprovider",
        "//java/external:slf4j",
    ],
)

filegroup(
    name = "slf4jconfig",
    srcs = ["simplelogger.properties"],
)
