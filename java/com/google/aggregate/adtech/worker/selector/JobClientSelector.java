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
import com.google.aggregate.adtech.worker.jobclient.aws.AwsJobHandlerModule;
import com.google.aggregate.adtech.worker.jobclient.gcp.GcpJobHandlerModule;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule;

/** CLI enum to select the job puller implementation */
public enum JobClientSelector {
  LOCAL_FILE(new LocalFileJobHandlerModule()),
  AWS(new AwsJobHandlerModule()),
  GCP(new GcpJobHandlerModule());

  private final Module pullerGuiceModule;

  JobClientSelector(Module pullerGuiceModule) {
    this.pullerGuiceModule = pullerGuiceModule;
  }

  public Module getPullerGuiceModule() {
    return pullerGuiceModule;
  }
}
