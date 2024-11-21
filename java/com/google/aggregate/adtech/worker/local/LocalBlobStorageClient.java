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

package com.google.aggregate.adtech.worker.local;

import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClient;
import java.nio.file.FileSystem;
import javax.inject.Inject;

/**
 * BlobStorageClient which list a single shard. Short term fix for Standalone library to expect a
 * single file for a avro batch rather than directory.
 */
public class LocalBlobStorageClient extends FSBlobStorageClient {

  private final FileSystem fileSystem;

  @Inject
  public LocalBlobStorageClient(FileSystem fileSystem) {
    super(fileSystem);
    this.fileSystem = fileSystem;
  }

  @Override
  public ImmutableList<String> listBlobs(DataLocation location) throws BlobStorageClientException {
    BlobStoreDataLocation blobLocation = location.blobStoreDataLocation();
    return ImmutableList.of(blobLocation.key());
  }
}
