/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.util.ReportingOriginUtils.InvalidReportingOriginException;
import org.junit.Test;

public class ReportingOriginUtilsTest {

  @Test
  public void convertToSite_singlePartTld() throws InvalidReportingOriginException {
    assertThat(ReportingOriginUtils.convertReportingOriginToSite("https://dummyOrigin.foo.com"))
        .isEqualTo("https://foo.com");
  }

  @Test
  public void convertToSite_multipartTld() throws InvalidReportingOriginException {
    assertThat(ReportingOriginUtils.convertReportingOriginToSite("https://dummyOrigin.foo.co.uk"))
        .isEqualTo("https://foo.co.uk");
  }

  @Test
  public void convertToSite_whenSiteProvided() throws InvalidReportingOriginException {
    assertThat(ReportingOriginUtils.convertReportingOriginToSite("https://foo.co.uk"))
        .isEqualTo("https://foo.co.uk");
  }

  @Test
  public void convertToSite_whenHttpUrlProvided() throws InvalidReportingOriginException {
    assertThat(ReportingOriginUtils.convertReportingOriginToSite("http://about.foo.blogspot.com"))
        .isEqualTo("https://foo.blogspot.com");
  }

  @Test
  public void convertToSite_whenUrlWithTrailingSlashProvided()
      throws InvalidReportingOriginException {
    assertThat(ReportingOriginUtils.convertReportingOriginToSite("http://about.foo.blogspot.com/"))
        .isEqualTo("https://foo.blogspot.com");
  }

  @Test
  public void convertToSite_whenUrlWithPortProvided()
          throws InvalidReportingOriginException {
    assertThat(ReportingOriginUtils.convertReportingOriginToSite("http://about.foo.blogspot.com:8443/bar"))
            .isEqualTo("https://foo.blogspot.com");
  }

  @Test
  public void convertToSite_onlyPublicSuffixProvided_throwsException() {
    InvalidReportingOriginException ex =
        assertThrows(
            InvalidReportingOriginException.class,
            () -> ReportingOriginUtils.convertReportingOriginToSite("https://blogspot.com"));
    assertThat(ex.getMessage()).contains("not under a known public suffix");
  }

  @Test
  public void convertToSite_underUnknownPublicSuffix_throwsException() {

    InvalidReportingOriginException ex =
        assertThrows(
            InvalidReportingOriginException.class,
            () ->
                ReportingOriginUtils.convertReportingOriginToSite(
                    "https://dummyOrigin.coordinator.test"));
    assertThat(ex.getMessage()).contains("not under a known public suffix");
  }

  @Test
  public void convertToSite_invalidDomainNoProtocol_throwsException() {

    InvalidReportingOriginException ex =
        assertThrows(
            InvalidReportingOriginException.class,
            () ->
                ReportingOriginUtils.convertReportingOriginToSite("dummyOrigin.coordinator.test"));
    assertThat(ex.getMessage()).contains("no protocol");
  }

  @Test
  public void convertToSite_malformedUrl_throwsException() {
    InvalidReportingOriginException ex =
        assertThrows(
            InvalidReportingOriginException.class,
            () -> ReportingOriginUtils.convertReportingOriginToSite("some invalid URL"));
    assertThat(ex.getMessage()).contains("no protocol");
  }

  @Test
  public void convertToSite_emptyInput_throwsException() {
    InvalidReportingOriginException ex =
        assertThrows(
            InvalidReportingOriginException.class,
            () -> ReportingOriginUtils.convertReportingOriginToSite(""));
    assertThat(ex.getMessage()).contains("no protocol");
  }
}
