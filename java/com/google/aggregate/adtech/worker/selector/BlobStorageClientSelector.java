/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.selector;

import com.google.inject.Module;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule;
import com.google.scp.operator.cpio.blobstorageclient.gcp.GcsBlobStorageClientModule;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;

/** CLI enum to select the data handler client implementation */
public enum BlobStorageClientSelector {
  AWS_S3_CLIENT(new S3BlobStorageClientModule()),
  GCP_CS_CLIENT(new GcsBlobStorageClientModule()),
  LOCAL_FS_CLIENT(new FSBlobStorageClientModule());

  private final Module blobStorageClientModule;

  BlobStorageClientSelector(Module blobStorageClientModule) {
    this.blobStorageClientModule = blobStorageClientModule;
  }

  public Module getBlobStorageClientSelectorModule() {
    return blobStorageClientModule;
  }
}
