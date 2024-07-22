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

package com.google.aggregate.adtech.worker.util;

import com.google.common.net.InternetDomainName;
import java.net.MalformedURLException;
import java.net.URL;

/** Utility class providing methods for working with reporting origins URLs. */
public class ReportingOriginUtils {
  /**
   * Converts a reporting origin URL to a site URL by extracting the top private domain. The return
   * value is always prefixed with the https protocol.
   *
   * @param reportingOrigin the reporting origin URL
   * @return the site URL, prefixed with https:// (e.g., https://example.com)
   * @throws InvalidReportingOriginException if the reporting origin is malformed or not under a
   *     known public suffix
   */
  public static String convertReportingOriginToSite(String reportingOrigin)
      throws InvalidReportingOriginException {
    URL url;
    try {
      // Strip trailing slash.
      if (reportingOrigin.endsWith("/")) {
        reportingOrigin = reportingOrigin.substring(0, reportingOrigin.length() - 1);
      }
      url = new URL(reportingOrigin);
    } catch (MalformedURLException e) {
      throw new InvalidReportingOriginException(e);
    }
    // Remove the protocol and port (if any)
    String host = url.getHost();
    InternetDomainName domain = InternetDomainName.from(host);
    if (!domain.isUnderPublicSuffix()) {
      throw new InvalidReportingOriginException(
          "Reporting origin is not under a known public suffix.");
    }
    return "https://" + domain.topPrivateDomain();
  }

  /** Exception thrown when a reporting origin is invalid. */
  public static class InvalidReportingOriginException extends Exception {

    public InvalidReportingOriginException(Throwable e) {
      super(e);
    }

    public InvalidReportingOriginException(String message) {
      super(message);
    }
  }
}
