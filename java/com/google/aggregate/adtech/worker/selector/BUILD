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
    name = "selector",
    srcs = glob(["*.java"]),
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/external:clients_blobstorageclient_aws",
        "//java/external:clients_blobstorageclient_gcp",
        "//java/external:clients_configclient_aws",
        "//java/external:clients_configclient_aws_operator",
        "//java/external:clients_configclient_gcp",
        "//java/external:clients_configclient_gcp_operator",
        "//java/external:clients_configclient_model",
        "//java/external:clients_cryptoclient_aws",
        "//java/external:clients_cryptoclient_gcp",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:clients_distributed_privacy_budgeting_aws",
        "//java/external:clients_distributed_privacy_budgeting_aws_to_gcp",
        "//java/external:clients_distributed_privacy_budgeting_gcp",
        "//java/external:clients_distributed_privacy_budgeting_local",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_gcp",
        "//java/external:clients_jobclient_model",
        "//java/external:clients_lifecycleclient_aws",
        "//java/external:clients_lifecycleclient_gcp",
        "//java/external:clients_lifecycleclient_local",
        "//java/external:clients_metricclient_aws",
        "//java/external:clients_metricclient_gcp",
        "//java/external:guice",
    ],
)
