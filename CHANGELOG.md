# Changelog

## [2.7.0](https://github.com/privacysandbox/aggregation-service/compare/v2.6.0...v2.7.0) (2024-08-01)

-   Added support for aggregating reports belonging to multiple reporting origins under the same
    reporting site in a single aggregation job.
-   [GCP Only] Updated coordinator endpoints to new Google/Third-Party coordinator pair.

## [2.6.0](https://github.com/privacysandbox/aggregation-service/compare/v2.5.0...v2.6.0) (2024-07-19)

-   Enabled support for
    [Aggregate Debug Reporting API](https://github.com/WICG/attribution-reporting-api/blob/main/aggregate_debug_reporting.md).
-   Reduced memory usage by making `AggregatedFacts` mutable and removing redundant object creation.
-   Updated dependencies to address security vulnerabilities.
-   Upgraded control plane shared library dependency to
    [v1.9.0-rc03](https://github.com/privacysandbox/coordinator-services-and-shared-libraries/releases/tag/v1.9.0-rc03)

## [2.5.1](https://github.com/privacysandbox/aggregation-service/compare/v2.5.0...v2.5.1) (2024-07-19)

-   Updated dependencies to address security vulnerabilities.

## [2.5.0](https://github.com/privacysandbox/aggregation-service/compare/v2.4.2...v2.5.0) (2024-05-22)

### Changes

-   Deprecated single party key support.
-   Enabled exception caching on private key service and set TTL for exception cache to reduce the
    load and make aggregation jobs run/fail faster.
-   Enabled non-zero filtering of labeled contributions.
-   Enabled OpenTelemetry for metric and trace collection.
-   Fixed enclave worker AMI
    [build issue](https://github.com/privacysandbox/aggregation-service/issues/40) by pinning Docker
    version to 24.0.5.
-   Fixed GCP cloud build by adding -y option to the install commands with Docker.
-   Fixed job failure with `ClassCastException` error.
-   Fixed noised facts mismatch when DebugRun and DomainOptional are both set.
-   Limited Otel memory reporting max to 90%.
-   Made aggregate fact noising parallel.
-   Made the job fail early on reaching a report error threshold.
-   Refactored blob reading logic to handle zero-sized blobs gracefully.
-   Removed redundant SingleFactAggregation class to reduce memory consumption.
-   Updated dependencies to address security vulnerabilities.
-   Updated documentation for batching strategies, aggregatable reports and OpenTelemetry usage.
-   Upgraded control plane shared library dependency to
    [v1.8.0-rc01](https://github.com/privacysandbox/coordinator-services-and-shared-libraries/releases/tag/v1.8.0-rc01)

## [2.4.4](https://github.com/privacysandbox/aggregation-service/compare/v2.4.3...v2.4.4) (2024-07-19)

### Changes

-   Updated dependencies to address security vulnerabilities.

## [2.4.3](https://github.com/privacysandbox/aggregation-service/compare/v2.4.2...v2.4.3) (2024-05-20)

### Changes

-   Fixed GCP cloud build by adding -y option to the install commands with Docker.
-   Updated dependencies to address security vulnerabilities.

## [2.4.2](https://github.com/privacysandbox/aggregation-service/compare/v2.4.1...v2.4.2) (2024-04-09)

### Changes

-   Fixed job failure with `ClassCastException` error.
-   Fixed noised facts mismatch when DebugRun and DomainOptional are both set.
-   Updated dependencies to address security vulnerabilities.

## [2.4.1](https://github.com/privacysandbox/aggregation-service/compare/v2.4.0...v2.4.1) (2024-03-11)

### Changes

#### [AWS only]

-   Fixed enclave worker AMI
    [build issue](https://github.com/privacysandbox/aggregation-service/issues/40) by pinning Docker
    version to 24.0.5.

## [2.4.0](https://github.com/privacysandbox/aggregation-service/compare/v2.3.0...v2.4.0) (2024-02-12)

### Changes

-   Added support for
    [Labeled Privacy Budget keys](https://github.com/patcg-individual-drafts/private-aggregation-api/blob/main/flexible_filtering.md)
    for default labels.
-   Used RxJava for domain reading, reducing overall job execution time and memory consumption.
-   Upgraded Bazel version to 6.5.0.

## [2.3.4](https://github.com/privacysandbox/aggregation-service/compare/v2.3.3...v2.3.4) (2024-07-19)

### Changes

-   Updated dependencies to address security vulnerabilities.

## [2.3.3](https://github.com/privacysandbox/aggregation-service/compare/v2.3.2...v2.3.3) (2024-05-20)

### Changes

-   Fixed GCP cloud build by adding -y option to the install commands with Docker.
-   Updated dependencies to address security vulnerabilities.

## [2.3.2](https://github.com/privacysandbox/aggregation-service/compare/v2.3.1...v2.3.2) (2024-04-09)

### Changes

-   Fixed job failure with `ClassCastException` error.
-   Updated dependencies to address security vulnerabilities.

## [2.3.1](https://github.com/privacysandbox/aggregation-service/compare/v2.3.0...v2.3.1) (2024-03-11)

### Changes

#### [AWS only]

-   Fixed enclave worker AMI
    [build issue](https://github.com/privacysandbox/aggregation-service/issues/40) by pinning Docker
    version to 24.0.5.

## [2.3.0](https://github.com/privacysandbox/aggregation-service/compare/v2.2.0...v2.3.0) (2024-01-12)

### Changes

-   Upgraded control plane shared library dependency to
    [v1.5.1](https://github.com/privacysandbox/coordinator-services-and-shared-libraries/releases/tag/v1.5.1)
-   Updated dependencies to address security vulnerabilities.

## [2.2.3](https://github.com/privacysandbox/aggregation-service/compare/v2.2.2...v2.2.3) (2024-05-20)

### Changes

-   Fixed GCP cloud build by adding -y option to the install commands with Docker.
-   Updated dependencies to address security vulnerabilities.

## [2.2.2](https://github.com/privacysandbox/aggregation-service/compare/v2.2.1...v2.2.2) (2024-04-09)

### Changes

-   Fixed job failure with `ClassCastException` error.
-   Updated dependencies to address security vulnerabilities.

## [2.2.1](https://github.com/privacysandbox/aggregation-service/compare/v2.2.0...v2.2.1) (2024-03-11)

### Changes

#### [AWS only]

-   Fixed enclave worker AMI
    [build issue](https://github.com/privacysandbox/aggregation-service/issues/40) by pinning Docker
    version to 24.0.5.

## [2.2.0](https://github.com/privacysandbox/aggregation-service/compare/v2.1.0...v2.2.0) (2023-12-07)

### Changes

-   Added additional shared_info version validation in case of version greater than LATEST_VERSION
    and updated [documentation](./docs/api.md).
-   Upgraded control plane shared library dependency to
    [v1.5.0](https://github.com/privacysandbox/coordinator-services-and-shared-libraries/releases/tag/v1.5.0)
-   Updated [operating documentation references](./README.md#operating-documentation).
-   Pinned maven install rule to prevent unexpected image pcr0/hash change.

#### [GCP Only]

-   Enabled parallel upload to cloud storage of the sharded summary reports.

## [2.1.4](https://github.com/privacysandbox/aggregation-service/compare/v2.1.3...v2.1.4) (2024-05-20)

### Changes

-   Fixed GCP cloud build by adding -y option to the install commands with Docker.
-   Updated dependencies to address security vulnerabilities.

## [2.1.3](https://github.com/privacysandbox/aggregation-service/compare/v2.1.2...v2.1.3) (2024-04-09)

### Changes

-   Fixed job failure with `ClassCastException` error.
-   Updated dependencies to address security vulnerabilities.

## [2.1.2](https://github.com/privacysandbox/aggregation-service/compare/v2.1.1...v2.1.2) (2024-03-11)

### Changes

#### [AWS only]

-   Fixed enclave worker AMI
    [build issue](https://github.com/privacysandbox/aggregation-service/issues/40) by pinning Docker
    version to 24.0.5.

## [2.1.1](https://github.com/privacysandbox/aggregation-service/compare/v2.1.0...v2.1.1) (2023-12-05)

### Changes

-   Fixed document links in [REAMDE.md](./README.md).
-   Pinned maven install rule to prevent unexpected image pcr0/hash change.
-   Pinned the OTel dependencies to a specific version.

#### [GCP Only]

-   Enabled parallel upload to cloud storage of the sharded summary reports.

## [2.1.0](https://github.com/privacysandbox/aggregation-service/compare/v2.0.0...v2.1.0) (2023-11-01)

### Changes

-   Added support for aggregation service on Google Cloud Platform (GCP) Follow our
    [instructions](./docs/gcp-aggregation-service.md) to get started.
-   Added check for source registration time being present in Attribution Reporting API (ARA) report
    for calculation of Attribution Report Accounting Key for privacy budgeting.
-   Added report shared_info version validations.

#### [AWS only]

-   Enable uploading sharded summary report to cloud storage in parallel.
-   Stabilized script fetch_terraform.sh by cleaning up existing files.

## [2.0.4](https://github.com/privacysandbox/aggregation-service/compare/v2.0.3...v2.0.4) (2024-05-20)

### Changes

-   Updated dependencies to address security vulnerabilities.

## [2.0.3](https://github.com/privacysandbox/aggregation-service/compare/v2.0.2...v2.0.3) (2024-04-09)

### Changes

-   Fixed job failure with `ClassCastException` error.
-   Updated dependencies to address security vulnerabilities.

## [2.0.2](https://github.com/privacysandbox/aggregation-service/compare/v2.0.1...v2.0.2) (2024-03-11)

### Changes

#### [AWS only]

-   Fixed enclave worker AMI
    [build issue](https://github.com/privacysandbox/aggregation-service/issues/40) by pinning Docker
    version to 24.0.5.

## [2.0.1](https://github.com/privacysandbox/aggregation-service/compare/v2.0.0...v2.0.1) (2023-12-05)

### Changes

-   Fixed document links in [REAMDE.md](./README.md).
-   Stabilized script fetch_terraform.sh by cleaning up existing files.
-   Pinned maven install rule to prevent unexpected image pcr0/hash change.

## [2.0.0](https://github.com/privacysandbox/aggregation-service/compare/v1.0.3...v2.0.0) (2023-09-20)

### Changes

-   Updated coordinator endpoints to new Google/Third-Party coordinator pair.
-   Upgraded control plane shared library dependency to
    [v1.2.0](https://github.com/privacysandbox/coordinator-services-and-shared-libraries/releases/tag/v1.2.0).
-   Added documentation for various job validations. It can be found [here](./docs/api.md).
-   Changes to handle null reports by filtering out the null facts before aggregation.
-   Added certain validations around fields in SharedInfo used for privacy budget key generation.

## [1.0.0](https://github.com/privacysandbox/aggregation-service/compare/v0.12.0...v1.0.0) (2023-07-11)

### Changes

-   Updated document links for aws-aggregation-service.md.
-   Updated comments in AMI BUILD file.

## [0.12.0](https://github.com/privacysandbox/aggregation-service/compare/v0.11.0...v0.12.0) (2023-06-28)

### Changes

-   Added output file sharding feature.
-   Fixed links in documents.
-   Added input bucket encoding details in collecting.md
-   Fixed fetch_terraform.sh script.
-   Improved AWS credentials provider to include prefetching and caching of credentials.

## [0.11.0](https://github.com/privacysandbox/aggregation-service/compare/v0.10.0...v0.11.0) (2023-06-21)

### Changes

-   Upgraded control plane shared library dependency to
    [v0.51.15](https://github.com/privacysandbox/control-plane-shared-libraries/tree/v0.51.15)
-   Updated Privacy Budget Client to retry on certain retryable http status codes in addition to
    IOExceptions.
-   Custom return messages for each PrivacyBudget related error for debug jobs.
-   Added stacktrace to return messages shown to adtechs in case of unhandled/unexpected exceptions.
-   Use of RxJava to read report files. Memory optimization to add back pressure.
-   Added 2 new error codes around report decryption - DECRYPTION_KEY_NOT_FOUND,
    DECRYPTION_KEY_FETCH_ERROR, SERVICE_ERROR.
-   Sample build script fixes that address issues
    [#16](https://github.com/privacysandbox/aggregation-service/issues/16) and
    [#17](https://github.com/privacysandbox/aggregation-service/issues/17)
-   Fail the job early when report error count exceeds threshold. More details
    [here](./docs/api.md).
-   Updated return message for PRIVACY_BUDGET_EXHAUSTED return code.
-   Add support for reading files larger than 2.5 GB.
-   Fix Base64 encoding for bucket in JSON format results.

## [0.10.0](https://github.com/privacysandbox/aggregation-service/compare/v0.9.0...v0.10.0) (2023-05-10)

### Changes

-   Upgraded control plane shared library dependency to
    [v0.51.13](https://github.com/privacysandbox/control-plane-shared-libraries/tree/v0.51.13)

## [0.9.0](https://github.com/privacysandbox/aggregation-service/compare/v0.8.0...v0.9.0) (2023-05-08)

### Changes

-   Update enclave base container to `java17-debian11`
-   Fixing LocalTestingTool json output
-   Add stacktrace to ResultInfo.ReturnMessage for handled exceptions
-   Add a return code "SUCCESS_WITH_ERRORS" for jobs that complete successfully but with some errors
    in some reports
-   Add Description to ErrorCount categories
-   Avro file load memory handling
-   null report support
-   Adding PBS results to debug runs

## [0.8.0](https://github.com/privacysandbox/aggregation-service/compare/v0.7.0...v0.8.0) (2023-03-15)

### Changes

-   Fix Local testing tool when using --skip_domain flag to skip specifying domain input file.
-   Fix to exit Aggregation worker when going out of memory. Enclave gets restarted automatically
    and continues to pick new jobs.
-   Fix to mitigate enclave restart failure issue. EC2 instance will get marked unhealthy if not
    able to start the enclave. Autoscaling group will replace the instance accordingly.

## [0.7.0](https://github.com/privacysandbox/aggregation-service/compare/v0.6.0...v0.7.0) (2023-03-07)

### Changes

-   Added error messages in Job result info returned as part of getJob API when uncaught exceptions
    happen and trigger job retries.

## [0.6.0](https://github.com/privacysandbox/aggregation-service/compare/v0.5.0...v0.6.0) (2023-02-23)

### Changes

-   Added Private Aggregation support for
    [FLEDGE](https://github.com/patcg-individual-drafts/private-aggregation-api#reports) and
    [Shared-storage](https://github.com/patcg-individual-drafts/private-aggregation-api#reports)
    reports in
    [local testing tool](https://github.com/privacysandbox/aggregation-service/blob/v0.6.0/docs/API.md#local-testing-tool).

-   Aggregation worker Java heap memory limits increased to 75% of container allocated memory.

-   Miscellaneous Aggregate API documentation updates.

## [0.5.0](https://github.com/privacysandbox/aggregation-service/compare/v0.4.0...v0.5.0) (2022-11-30)

### Changes

-   Upgraded control plane shared library dependency to
    [v0.39.0](https://github.com/privacysandbox/control-plane-shared-libraries/tree/v0.39.0), which
    requires 6 Elastic IP addresses for the subnets of the created VPC. With the default
    [AWS VPC EIPs quota](https://docs.aws.amazon.com/vpc/latest/userguide/amazon-vpc-limits.html)
    set to 5 per region, users may need to request for more quota when deploying the Aggregate
    Service or decrease the number of subnets created for the aggregation service VPC. To stay
    within the default quota you can decrease the number of subnets, by setting
    `vpc_availability_zones = ["a","b","c","d","e"]` in your `<name>.auto.tfvars`

-   Addressed both
    [security issues](https://github.com/privacysandbox/aggregation-service/blob/v0.4.0/README.md#general-security-notes)
    by uprading the control plane shared library dependency.

## [0.4.0](https://github.com/privacysandbox/aggregation-service/compare/v0.3.0...v0.4.0) (2022-10-24)

### Changes

-   Multi-party coordinator support added and made the default.
-   Aggregation Service enforces the
    [no-duplicate](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#no-duplicates-rule)
    rule. We recommend users design their systems keeping the no-duplicate rule in consideration. We
    suggest reading the [debugging](./DEBUGGING.md) document for debug aggregation runs.

## [0.3.0](https://github.com/privacysandbox/aggregation-service/compare/v0.2.0...v0.3.0) (2022-06-30)

### Changes

-   Support for updated Attribution Reporting API
    [Aggregatable Reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATION_SERVICE_TEE.md#aggregatable-reports)
    format.

## [0.2.0](https://github.com/privacysandbox/aggregation-service/compare/v0.1.2...v0.2.0) (2022-06-09)

### Changes

-   The
    [`no-duplicate`](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#no-duplicates-rule)
    rule for privacy budget is not enforced anymore but will be enforced in the future. We recommend
    you design your systems with the `no-duplicate` rule in consideration.
-   Added support for relative paths in LocalTestingTool binary
-   Fixed issue with unexpected error messages in LocalTestingTool binary
