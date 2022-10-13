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

package(default_visibility = ["//visibility:public"])

java_library(
    name = "util",
    srcs = [
        "DebugSupportHelper.java",
        "NumericConversions.java",
    ],
    deps = [
        "//java/external:clients_jobclient_model",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/cpio/jobclient:model",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/shared/model",
    ],
)