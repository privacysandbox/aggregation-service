# Aggregation Service for Measurement

Aggregation Service provides server-side mechanism for adtechs to create summary reports. A summary
report is the result of noisy aggregation applied to a batch of aggregatable reports. Summary
reports allow for greater flexibility and a richer data model than event-level reporting,
particularly for some use-cases like conversion values. Aggregation Service operates on aggregatable
reports that are encrypted reports sent from individual user devices which contain data about
individual events in the client software, such as a Chrome browser or an Android device.

## Build and deploy

This repository contains instructions and scripts to set up and test the Aggregation Service for
[Aggregatable Reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATION_SERVICE_TEE.md#aggregatable-reports)
locally and on Amazon Web Services
[Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/). If you want to learn more about
the [Privacy Sandbox](https://privacysandbox.com/) Aggregation Service for the Attribution Reporting
API and other use cases, read the
[Aggregation Service proposal](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATION_SERVICE_TEE.md#aggregatable-reports).

## Key documents

### Explainers

-   [Aggregation Service explainer](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md)

### Operating documentation

-   [Collect and batch aggregatable reports](/docs/collecting.md)
-   [Testing locally using Local Testing Tool](docs/local-testing-tool.md)
-   [Testing on AWS using encrypted reports](docs/aws-aggregation-service.md)
-   [Testing on GCP using encrypted reports](docs/gcp-aggregation-service.md)
-   [Aggregation Service API](docs/api.md)
-   [Generate debug summary reports](/docs/debugging.md)

## Support

You can reach out to us for support through creating issues on this repository or sending us an
email at aggregation-service-support\<at>google.com. This address is monitored and only visible to
selected support staff.

## License

Apache 2.0 - See [LICENSE](LICENSE) for more information.

## FAQ

### Where should I post feedback/questions, this repo or the API repo?

For feedback/questions encountered during using this aggregation service implementation, please use
the support channels provided by this repo. For feedback/requests related to the APIs in general,
please initiate discussions in the respective API repo eg.
[Attribution Reporting API repo](https://github.com/WICG/attribution-reporting-api).
