# Changelog

## [0.3.0](https://github.com/google/trusted-execution-aggregation-service/compare/v0.2.0...v0.3.0) (2022-06-30)

### Changes

* Support for updated Attribution Reporting API [Aggregatable Reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATION_SERVICE_TEE.md#aggregatable-reports) format.

## [0.2.0](https://github.com/google/trusted-execution-aggregation-service/compare/v0.1.2...v0.2.0) (2022-06-09)

### Changes

* The [`no-duplicate`](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#no-duplicates-rule) rule for privacy budget is not enforced anymore but will be enforced in the future. We recommend you design your systems with the `no-duplicate` rule in consideration.
* Added support for relative paths in LocalTestingTool binary
* Fixed issue with unexpected error messages in LocalTestingTool binary
