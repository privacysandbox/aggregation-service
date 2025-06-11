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

package com.google.aggregate.adtech.worker.frontend.injection.modules.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.google.common.base.Converter;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.aggregate.adtech.worker.frontend.injection.modules.BaseFrontendModule;
import com.google.aggregate.adtech.worker.frontend.serialization.JsonSerializerFacade;
import com.google.aggregate.adtech.worker.frontend.serialization.ObjectMapperSerializerFacade;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.adtech.worker.frontend.service.FrontendServiceImpl;
import com.google.aggregate.adtech.worker.frontend.service.converter.CreateJobRequestToRequestInfoConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.ErrorCountConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.ErrorSummaryConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.GetJobResponseConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.JobStatusConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.ResultInfoConverter;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTask;
import com.google.aggregate.adtech.worker.frontend.tasks.testing.FakeCreateJobTask;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.adtech.worker.frontend.testing.FakeRequestInfoValidator;
import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.ErrorCountProto;
import com.google.aggregate.protos.frontend.api.v1.ErrorSummaryProto;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto;
import com.google.aggregate.protos.frontend.api.v1.JobStatusProto;
import com.google.aggregate.protos.frontend.api.v1.ResultInfoProto;
import com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.testing.FakeJobQueue;
import com.google.scp.shared.mapper.TimeObjectMapper;

/** Contains all the bindings used for unit testing the frontend components. */
@AutoService(BaseFrontendModule.class)
public class FakeFrontendModule extends BaseFrontendModule {

  private static final FakeRequestInfoValidator fakeRequestInfoValidator;

  static {
    fakeRequestInfoValidator = new FakeRequestInfoValidator();
  }

  /** Provides an instance of the {@code FakeRequestInfoValidator} class. */
  @Provides
  @Singleton
  public static FakeRequestInfoValidator getFakeRequestInfoValidator() {
    return fakeRequestInfoValidator;
  }

  @Override
  public Class<? extends JsonSerializerFacade> getJsonSerializerImplementation() {
    return ObjectMapperSerializerFacade.class;
  }

  @Override
  public Class<? extends FrontendService> getFrontendServiceImplementation() {
    return FrontendServiceImpl.class;
  }

  @Override
  public Class<? extends Converter<JobMetadata, GetJobResponseProto.GetJobResponse>>
      getJobResponseConverterImplementation() {
    return GetJobResponseConverter.class;
  }

  @Override
  public Class<? extends Converter<CreateJobRequest, RequestInfo>>
      getCreateJobRequestToRequestInfoConverterImplementation() {
    return CreateJobRequestToRequestInfoConverter.class;
  }

  @Override
  public Class<? extends Converter<ResultInfo, ResultInfoProto.ResultInfo>>
      getResultInfoConverterImplementation() {
    return ResultInfoConverter.class;
  }

  @Override
  public Class<? extends Converter<ErrorSummary, ErrorSummaryProto.ErrorSummary>>
      getErrorSummaryConverterImplementation() {
    return ErrorSummaryConverter.class;
  }

  @Override
  public Class<
          ? extends
              Converter<
                  com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
                  JobStatusProto.JobStatus>>
      getJobStatusConverterImplementation() {
    return JobStatusConverter.class;
  }

  @Override
  public Class<? extends Converter<ErrorCount, ErrorCountProto.ErrorCount>>
      getErrorCountConverterImplementation() {
    return ErrorCountConverter.class;
  }

  @Override
  protected void configureModule() {
    bind(ObjectMapper.class).to(TimeObjectMapper.class);
    bind(JobQueue.class).to(FakeJobQueue.class);
    bindCreateJobTaskModule();
    Multibinder<RequestInfoValidator> requestInfoValidatorMultibinder =
        Multibinder.newSetBinder(binder(), RequestInfoValidator.class);
    requestInfoValidatorMultibinder.addBinding().toInstance(fakeRequestInfoValidator);
  }

  public void bindCreateJobTaskModule() {
    bind(CreateJobTask.class).to(FakeCreateJobTask.class);
  }
}
