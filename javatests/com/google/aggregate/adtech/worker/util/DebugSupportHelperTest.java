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

package com.google.aggregate.adtech.worker.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DebugSupportHelperTest {

  private Job ctx;

  @Test
  public void testGetDebugFileS3PathWithParent() {
    String filePath = "folder1/folder2/filename.avro";

    String debugPath = DebugSupportHelper.getDebugFilePrefix(filePath);

    assertThat(debugPath).isEqualTo("folder1/folder2/debug/filename.avro");
  }

  @Test
  public void testGetDebugFileS3PathWithoutParent() {
    String filePath = "filename.avro";

    String debugPath = DebugSupportHelper.getDebugFilePrefix(filePath);

    assertThat(debugPath).isEqualTo("debug/filename.avro");
  }

  @Test
  public void testGetDebugFileS3PathWithEmptyPath() {
    String filePath = "";

    // no action

    assertThrows(
        IllegalArgumentException.class, () -> DebugSupportHelper.getDebugFilePrefix(filePath));
  }

  @Test
  public void testGetDebugFileS3PathWithoutExtension() {
    String filePath = "filename";

    String debugPath = DebugSupportHelper.getDebugFilePrefix(filePath);

    assertThat(debugPath).isEqualTo("debug/filename");
  }

  @Test
  public void testIsDebugRunWithDebugJob() {
    ctx = FakeJobGenerator.generateBuilder("foo").build();
    RequestInfo requestInfo = ctx.requestInfo();

    RequestInfo requestInfoWithDebug =
        requestInfo.toBuilder()
            .clearJobParameters()
            .putAllJobParameters(ImmutableMap.of(DebugSupportHelper.JOB_PARAM_DEBUG_RUN, "true"))
            .build();
    ctx = ctx.toBuilder().setRequestInfo(requestInfoWithDebug).build();

    assertTrue(DebugSupportHelper.isDebugRun(ctx));
  }

  @Test
  public void testIsDebugRunWithNotDebugJob() {
    ctx = FakeJobGenerator.generateBuilder("foo").build();
    RequestInfo requestInfo = ctx.requestInfo();

    RequestInfo requestInfoWithDebug =
        requestInfo.toBuilder()
            .clearJobParameters()
            .putAllJobParameters(ImmutableMap.of(DebugSupportHelper.JOB_PARAM_DEBUG_RUN, "false"))
            .build();
    ctx = ctx.toBuilder().setRequestInfo(requestInfoWithDebug).build();

    assertFalse(DebugSupportHelper.isDebugRun(ctx));
  }

  @Test
  public void testIsDebugRunWithNoDebugJobParams() {
    ctx = FakeJobGenerator.generateBuilder("foo").build();
    RequestInfo requestInfo = ctx.requestInfo();

    RequestInfo requestInfoWithDebug =
        requestInfo.toBuilder().clearJobParameters().putAllJobParameters(ImmutableMap.of()).build();
    ctx = ctx.toBuilder().setRequestInfo(requestInfoWithDebug).build();

    assertFalse(DebugSupportHelper.isDebugRun(ctx));
  }
}
