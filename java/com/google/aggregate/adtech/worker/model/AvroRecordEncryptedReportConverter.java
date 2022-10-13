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

package com.google.aggregate.adtech.worker.model;

import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.common.base.Converter;

/** Converts avro records to encrypted report. */
public final class AvroRecordEncryptedReportConverter
    extends Converter<AvroReportRecord, EncryptedReport> {

  @Override
  protected EncryptedReport doForward(AvroReportRecord avroRecord) {
    return EncryptedReport.builder()
        .setPayload(avroRecord.payload())
        .setKeyId(avroRecord.keyId())
        .setSharedInfo(avroRecord.sharedInfo())
        .build();
  }

  @Override
  protected AvroReportRecord doBackward(EncryptedReport encryptedReport) {
    return AvroReportRecord.create(
        encryptedReport.payload(), encryptedReport.keyId(), encryptedReport.sharedInfo());
  }
}
