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
    name = "localblob_client",
    srcs = [
        "LocalBlobStorageClient.java",
        "LocalBlobStorageClientModule.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/lifecycleclient:aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:aws_regions",
        "//java/external:aws_s3",
        "//java/external:clients_blobstorageclient_aws",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_configclient_aws",
        "//java/external:clients_configclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:clients_cryptoclient_aws",
        "//java/external:clients_metricclient_aws",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_databind",
        "//java/external:javax_inject",
    ],
)
