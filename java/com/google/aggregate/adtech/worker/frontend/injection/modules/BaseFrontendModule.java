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

package com.google.aggregate.adtech.worker.frontend.injection.modules;

import com.google.common.base.Converter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.aggregate.adtech.worker.frontend.serialization.JsonSerializerFacade;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.ErrorCountProto;
import com.google.aggregate.protos.frontend.api.v1.ErrorSummaryProto;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto;
import com.google.aggregate.protos.frontend.api.v1.JobStatusProto;
import com.google.aggregate.protos.frontend.api.v1.ResultInfoProto;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.injection.factories.ModuleFactory;
import com.google.aggregate.adtech.worker.shared.injection.modules.BaseDataModule;
import java.util.logging.Logger;

/** Allows swappable/discoverable frontend modules when an inheritor's package is referenced. */
public abstract class BaseFrontendModule extends AbstractModule {

  /** Returns a {@code Class} object for the JsonSerializer. */
  public abstract Class<? extends JsonSerializerFacade> getJsonSerializerImplementation();

  /** Returns a {@code Class} object for the FrontendService. */
  public abstract Class<? extends FrontendService> getFrontendServiceImplementation();

  /**
   * Returns a type that converts from {@link JobMetadata} and {@link
   * GetJobResponseProto.GetJobResponse} *
   */
  public abstract Class<? extends Converter<JobMetadata, GetJobResponseProto.GetJobResponse>>
      getJobResponseConverterImplementation();

  /** Returns a type that converts from {@link CreateJobRequest} and {@link RequestInfo} */
  public abstract Class<? extends Converter<CreateJobRequest, RequestInfo>>
      getCreateJobRequestToRequestInfoConverterImplementation();

  /**
   * Returns a type that converts from {@link import
   * com.google.aggregate.protos.frontend.api.v1.ResultInfoProto.ResultInfo} and {@link
   * com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo} *
   */
  public abstract Class<
          ? extends
              Converter<
                  com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo,
                  ResultInfoProto.ResultInfo>>
      getResultInfoConverterImplementation();

  /**
   * Returns a type that converts from {@link
   * ErrorSummaryProto.ErrorSummary} and {@link
   * com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary} *
   */
  public abstract Class<
          ? extends
              Converter<
                  com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary,
                  ErrorSummaryProto.ErrorSummary>>
      getErrorSummaryConverterImplementation();

  /**
   * Returns a type that converts from {@link
   * JobStatusProto.JobStatus} and {@link
   * com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus} *
   */
  public abstract Class<
          ? extends
              Converter<
                  com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
                  JobStatusProto.JobStatus>>
      getJobStatusConverterImplementation();

  /**
   * Returns a type that converts from {@link
   * ErrorCountProto.ErrorCount} and {@link
   * com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount} *
   */
  public abstract Class<
          ? extends
              Converter<
                  com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount,
                  ErrorCountProto.ErrorCount>>
      getErrorCountConverterImplementation();

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies
   * that are specific to that implementation.
   */
  protected void configureModule() {}

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    Logger.getLogger("").info("Configuring base front end module");
    // Bind all classes needed for front end related services
    bind(JsonSerializerFacade.class).to(getJsonSerializerImplementation()).in(Singleton.class);
    bind(FrontendService.class).to(getFrontendServiceImplementation());
    bind(new TypeLiteral<Converter<JobMetadata, GetJobResponseProto.GetJobResponse>>() {})
        .to(getJobResponseConverterImplementation());
    bind(new TypeLiteral<
            Converter<
                CreateJobRequest,
                RequestInfo>>() {})
        .to(getCreateJobRequestToRequestInfoConverterImplementation());
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo,
                ResultInfoProto.ResultInfo>>() {})
        .to(getResultInfoConverterImplementation());
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary,
                ErrorSummaryProto.ErrorSummary>>() {})
        .to(getErrorSummaryConverterImplementation());
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
                JobStatusProto.JobStatus>>() {})
        .to(getJobStatusConverterImplementation());
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount,
                ErrorCountProto.ErrorCount>>() {})
        .to(getErrorCountConverterImplementation());

    install(ModuleFactory.getModule(BaseDataModule.class));
    configureModule();
  }

  /** Allows binding for non-generic classes, and adds additional logging. */
  protected @Override <T> AnnotatedBindingBuilder<T> bind(Class<T> bind) {
    Logger.getLogger("").info("Binding - " + bind.getName());
    return super.bind(bind);
  }

  /** Allows binding for generic classes, and adds additional logging. */
  protected @Override <T> AnnotatedBindingBuilder<T> bind(TypeLiteral<T> bind) {
    Logger.getLogger("").info("Binding - " + bind.toString());
    return super.bind(bind);
  }
}
