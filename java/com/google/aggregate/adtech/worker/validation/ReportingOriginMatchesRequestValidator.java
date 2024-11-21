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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MALFORMED;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.REPORTING_SITE_MISMATCH;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;
import com.google.aggregate.adtech.worker.util.ReportingOriginUtils;
import com.google.aggregate.adtech.worker.util.ReportingOriginUtils.InvalidReportingOriginException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Validates that the report's reportingOrigin is the same as the attributionReportTo provided in
 * the Aggregation Request.
 */
public final class ReportingOriginMatchesRequestValidator implements ReportValidator {

  private static final int MAX_CACHE_SIZE = 100;
  private static final long CACHE_ENTRY_TTL_SEC = 3600;
  private final LoadingCache<String, String> originToSiteMap =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_CACHE_SIZE)
          .expireAfterWrite(CACHE_ENTRY_TTL_SEC, TimeUnit.SECONDS)
          .concurrencyLevel(Runtime.getRuntime().availableProcessors())
          .build(
              new CacheLoader<>() {
                @Override
                public String load(final String reportingOrigin)
                    throws InvalidReportingOriginException {
                  return ReportingOriginUtils.convertReportingOriginToSite(reportingOrigin);
                }
              });

  @Override
  public Optional<ErrorMessage> validate(Report report, Job ctx) {
    Optional<String> optionalSiteValue =
        Optional.ofNullable(ctx.requestInfo().getJobParametersMap().get("reporting_site"));
    if (optionalSiteValue.isPresent()) {
      try {
        String reportingSiteParameterValue = optionalSiteValue.get();
        String siteForReportingOrigin = originToSiteMap.get(report.sharedInfo().reportingOrigin());
        if (!reportingSiteParameterValue.equals(siteForReportingOrigin)) {
          return createErrorMessage(REPORTING_SITE_MISMATCH);
        }
        return Optional.empty();
      } catch (ExecutionException e) {
        return createErrorMessage(ATTRIBUTION_REPORT_TO_MALFORMED);
      }
    } else {
      String attributionReportTo =
          ctx.requestInfo().getJobParametersMap().get("attribution_report_to");
      if (report.sharedInfo().reportingOrigin().equals(attributionReportTo)) {
        return Optional.empty();
      }

      return createErrorMessage(ATTRIBUTION_REPORT_TO_MISMATCH);
    }
  }
}
