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

package com.google.aggregate.adtech.worker.jobclient.testing;

import com.google.aggregate.adtech.worker.jobclient.JobClient.JobClientException;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import com.google.aggregate.adtech.worker.jobclient.testing.ConstantJobClient;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobResultGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

@RunWith(JUnit4.class)
public class ConstantJobClientTest {

  private com.google.aggregate.adtech.worker.jobclient.testing.ConstantJobClient jobClient;

  @Before
  public void setUp() {
    jobClient = new ConstantJobClient();
  }

  @Test
  public void pullDefaultEmpty() throws Exception {
    Optional<Job> jobPulled = jobClient.getJob();

    assertThat(jobPulled).isEmpty();
  }

  @Test
  public void pullConstantAndExhaust() throws Exception {
    Job job = com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator.generate("foo");
    jobClient.setReturnConstant(job);

    Optional<Job> jobPulled = jobClient.getJob();

    assertThat(jobPulled).isPresent();
    assertThat(jobPulled.get()).isEqualTo(job);

    Optional<Job> exhaustedItem = jobClient.getJob();

    assertThat(exhaustedItem).isEmpty();
  }

  @Test
  public void pullEmptyExplicit() throws Exception {
    Job job = com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator.generate("foo");
    jobClient.setReturnConstant(job);
    jobClient.setReturnEmpty();

    Optional<Job> jobPulled = jobClient.getJob();

    assertThat(jobPulled).isEmpty();
  }

  @Test
  public void testMarkJobCompleted() throws Exception {
    Job job = com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator.generate("foo");
    JobResult jobResult = com.google.aggregate.adtech.worker.jobclient.testing.FakeJobResultGenerator.fromJob(job);

    jobClient.markJobCompleted(jobResult);

    assertThat(jobClient.getLastJobResultCompleted()).isEqualTo(jobResult);
  }

  /** Test that getJob throws an exception when set to but does not throw on successive calls */
  @Test
  public void testGetJobThrowsWhenSetTo() throws Exception {
    Job job = com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator.generate("fizz");
    jobClient.setShouldThrowOnGetJob(true);
    jobClient.setReturnConstant(job);

    // Should throw on first call, but not the second one
    assertThrows(JobClientException.class, () -> jobClient.getJob());
    Optional<Job> jobPulled = jobClient.getJob();

    assertThat(jobPulled).hasValue(job);
  }

  @Test
  public void testMarkJobCompletedThrowsWhenSetTo() throws Exception {
    Job job = FakeJobGenerator.generate("foo");
    JobResult jobResult = FakeJobResultGenerator.fromJob(job);
    jobClient.setShouldThrowOnMarkJobCompleted(true);

    assertThrows(JobClientException.class, () -> jobClient.markJobCompleted(jobResult));
  }
}
