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

package com.google.aggregate.protocol.avro;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData.EnumSymbol;
import org.apache.avro.generic.GenericRecord;

/** Reader that provides {@code Record} from an Avro file following the defined schema. */
public final class AvroDebugResultsReader extends AvroRecordReader<AvroDebugResultsRecord> {

  /** Creates a reader based on the given records. */
  AvroDebugResultsReader(DataFileStream<GenericRecord> streamReader) {
    super(streamReader);
  }

  AvroDebugResultsRecord deserializeRecordFromGeneric(GenericRecord record) {
    byte[] bucketBytes = ((ByteBuffer) record.get("bucket")).array();

    BigInteger bucket = NumericConversions.uInt128FromBytes(bucketBytes);
    long unnoisedMetric = ((long) record.get("unnoised_metric"));
    long noise = ((long) record.get("noise"));
    List<EnumSymbol> annotations = (List<EnumSymbol>) record.get("annotations");

    List<DebugBucketAnnotation> annotationList =
        annotations.stream()
            .map(
                annotation ->
                    DebugBucketAnnotation.valueOf(annotation.toString().toUpperCase(Locale.ROOT)))
            .collect(toImmutableList());

    return AvroDebugResultsRecord.create(
        bucket, unnoisedMetric + noise, unnoisedMetric, annotationList);
  }
}
