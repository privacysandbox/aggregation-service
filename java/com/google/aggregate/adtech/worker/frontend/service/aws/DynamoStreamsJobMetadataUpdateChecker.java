/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.frontend.service.aws;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.transformers.v2.dynamodb.DynamodbRecordTransformer;
import com.google.inject.Inject;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.converter.AttributeValueMapToJobMetadataConverter;
import com.google.aggregate.adtech.worker.shared.model.BackendModelUtil;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.model.Record;

/** Simple utility to check for updated {@link JobMetadata} in a {@link DynamodbStreamRecord} */
public final class DynamoStreamsJobMetadataUpdateChecker {

  private final AttributeValueMapToJobMetadataConverter attributeValueMapConverter;

  /** Creates a new instance of the {@code DynamoStreamsJobMetadataUpdateChecker} class. */
  @Inject
  DynamoStreamsJobMetadataUpdateChecker(
      AttributeValueMapToJobMetadataConverter attributeValueMapConverter) {
    this.attributeValueMapConverter = attributeValueMapConverter;
  }

  /**
   * Checks if there is any update in the stream record provided and converts to the JobMetadata.
   * This method returns an empty optional if the old and new images are the same, ignoring the
   * record version, in order to prevent processing when no change is present.
   *
   * <p>NOTE: This is only able to detect changes between the old and new metadata if the stream
   * view type is set to "new and old images." If only the new image is provided it will think every
   * metadata entry in the stream is a change.
   *
   * @param dynamodbStreamRecord the stream record with new and old images (old is missing for
   *     inserts)
   * @return A present optional if the new image contains an update. Optional will be present when
   *     no old image is present (signaling an insert). Optional will be empty if the new image has
   *     no logical changes from the old one, ignoring updates to the recordVersion.
   */
  public Optional<JobMetadata> checkForUpdatedMetadata(DynamodbStreamRecord dynamodbStreamRecord) {
    Record sdkV2Record = DynamodbRecordTransformer.toRecordV2(dynamodbStreamRecord);

    if (!sdkV2Record.dynamodb().hasNewImage()) {
      return Optional.empty();
    }
    JobMetadata newJobMetadata =
        attributeValueMapConverter.convert(sdkV2Record.dynamodb().newImage());
    if (!sdkV2Record.dynamodb().hasOldImage()) {
      // Return the new image if it is present but the old one is missing
      return Optional.of(newJobMetadata);
    } else {
      // Return the new image if it is different from the old one (ignoring the record version),
      // otherwise return empty optional
      JobMetadata oldJobMetadata =
          attributeValueMapConverter.convert(sdkV2Record.dynamodb().oldImage());

      return Optional.of(newJobMetadata)
          .filter(
              newMetadata -> !BackendModelUtil.equalsIgnoreDbFields(newMetadata, oldJobMetadata));
    }
  }
}
