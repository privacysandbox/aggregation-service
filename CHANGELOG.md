# Changelog

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
