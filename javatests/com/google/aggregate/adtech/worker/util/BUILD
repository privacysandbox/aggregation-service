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

java_test(
    name = "NumericConversionsTest",
    srcs = ["NumericConversionsTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:google_truth",
    ],
)

java_test(
    name = "DebugSupportHelperTest",
    srcs = ["DebugSupportHelperTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:operator_protos",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/cpio/jobclient:model",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/shared/dao/metadatadb/model",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/shared/model",
    ],
)