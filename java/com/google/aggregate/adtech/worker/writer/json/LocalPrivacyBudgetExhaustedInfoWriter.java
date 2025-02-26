/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.adtech.worker.writer.json;

import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.adtech.worker.model.Views;
import com.google.aggregate.adtech.worker.model.serdes.PrivacyBudgetExhaustedInfoSerdes;
import com.google.aggregate.adtech.worker.writer.PrivacyBudgetExhaustedInfoWriter;
import com.google.aggregate.adtech.worker.writer.PrivacyBudgetExhaustedInfoWriter.FileWriteException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Writes PrivacyBudgetExhaustedInfo to local filesystem. This written file will be uploaded to
 * cloud storage for adtech to access.
 */
public final class LocalPrivacyBudgetExhaustedInfoWriter
    implements PrivacyBudgetExhaustedInfoWriter {
  private final PrivacyBudgetExhaustedInfoSerdes privacyBudgetExhaustedInfoSerdes;

  @Inject
  LocalPrivacyBudgetExhaustedInfoWriter(
      PrivacyBudgetExhaustedInfoSerdes privacyBudgetExhaustedInfoSerdes) {
    this.privacyBudgetExhaustedInfoSerdes = privacyBudgetExhaustedInfoSerdes;
  }

  @Override
  public void writePrivacyBudgetExhaustedInfo(
      PrivacyBudgetExhaustedInfo privacyBudgetExhaustedInfo, Path resultFilePath)
      throws FileWriteException {
    try {
      String serializedPrivacyBudgetExhaustedInfo =
          privacyBudgetExhaustedInfoSerdes.doBackwardWithView(
              Optional.of(privacyBudgetExhaustedInfo), Views.UsedInPrivacyBudgeting.class);
      Files.writeString(resultFilePath, serializedPrivacyBudgetExhaustedInfo);
    } catch (IOException e) {
      throw new FileWriteException("Failed to write local PrivacyBudgetExhaustedInfo JSON file", e);
    }
  }
}
