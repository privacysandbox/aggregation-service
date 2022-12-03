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

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NoopJobProcessorTest {

  private NoopJobProcessor processor;

  @Before
  public void setUp() {
    processor = new NoopJobProcessor();
  }

  @Test
  public void returnEmptyIfNothingIsSet() {
    Optional<Job> lastProcessed = processor.getLastProcessed();

    assertThat(lastProcessed).isEmpty();
  }

  @Test
  public void returnLastProcessedItemAndJobResultToReturn() throws Exception {
    Job item = FakeJobGenerator.generate("foo");
    JobResult jobResult = FakeJobResultGenerator.fromJob(item);
    processor.setJobResultToReturn(jobResult);

    JobResult jobResultReturned = processor.process(item);

    assertThat(processor.getLastProcessed()).isPresent();
    assertThat(processor.getLastProcessed().get()).isEqualTo(item);
    assertThat(jobResultReturned).isEqualTo(jobResult);
  }

  @Test
  public void throwsWhenSetTo() throws Exception {
    Job item = FakeJobGenerator.generate("foo");
    processor.setShouldThrowException(true);

    assertThrows(IllegalStateException.class, () -> processor.process(item));
  }
}
