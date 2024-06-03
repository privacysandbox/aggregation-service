# Testing locally using Local Testing Tool

The local testing tool can be used to perform aggregation and generate a summary report with the
following types of **unencrypted** aggregatable debug reports-

1. [Attribution Reporting](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
2. [Protected Audience](https://github.com/patcg-individual-drafts/private-aggregation-api#reports)
3. [Shared Storage](https://github.com/patcg-individual-drafts/private-aggregation-api#reports)

[Aggregatable debug reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
that have debug_cleartext_payload are helpful for understanding the content of reports and
validating that registrations on the browser client or device are configured properly. Learn how to
setup debug reports for
[Chrome](https://developer.chrome.com/docs/privacy-sandbox/attribution-reporting-debugging) and
[Android](https://developer.android.com/design-for-safety/privacy-sandbox/attribution#attribution-success)
clients.

You can process
[aggregatable debug reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
locally and generate summary reports with the LocalTestingTool\_{VERSION}.jar.

**NOTE** : encrypted reports can **not** be processed with the local testing tool.

## Using the Local Testing tool

### Clone the repository

Clone the repository into a local folder `<repository_root>`:

```sh
git clone https://github.com/privacysandbox/aggregation-service;
cd aggregation-service
```

### Download Local Testing tool

Download the local testing tool with the below command.

```sh
VERSION=$(cat VERSION); curl -f -o LocalTestingTool_$VERSION.jar https://aggregation-service-published-artifacts.s3.amazonaws.com/aggregation-service/$VERSION/LocalTestingTool_$VERSION.jar
```

You'll need [Java JRE](https://adoptium.net/) installed to use the tool.

<!-- prettier-ignore-start -->
_The `SHA256` of the `LocalTestingTool_{version}.jar` can be found on the
[releases page](https://github.com/privacysandbox/aggregation-service/releases)._
<!-- prettier-ignore-end -->

### Generating a summary report

Using the local testing tool, you can generate a summary report.

Simply pass any of the 3 kinds of supported reports as `--input_data_avro_file` param.

Follow the instructions on how to
[collect and batch aggregatable reports](/docs/collecting.md#collecting-and-batching-aggregatable-reports).
Create an output domain file: `output_domain.avro`. For testing you can use our
[sample debug batch](/sampledata/output_debug_reports.avro) with the corresponding
[output domain avro](/sampledata/output_domain.avro).

To aggregate the resulting avro batch `output_debug_reports.avro` file into a summary report, run
the following command:

```sh
java -jar LocalTestingTool_{version}.jar \
--input_data_avro_file output_debug_reports.avro \
--domain_avro_file output_domain.avro \
--output_directory .
```

This will create a summary report as output.avro file in the same directory where you ran the tool.
You can use [avro tools](https://mvnrepository.com/artifact/org.apache.avro/avro-tools) to read avro
output as JSON.

You can also batch the `output_debug_reports.avro` file into a summary report without adding noise
to the summary report. You should expect to receive the value of `32768` and `4400` from the
[sample aggregatable report](collecting.md#aggregatable-report-sample)

```sh
java -jar LocalTestingTool_<version>.jar \
--input_data_avro_file output_debug_reports_<timestamp>.avro \
--domain_avro_file output_domain.avro \
--json_output \
--output_directory . \
--no_noising
```

The output of above tool execution will be in `output.json` with the following content

```json
[
    {
        "bucket": "<øg\u0090?»sì&Õ\u0018À\u0096\u008c)Ü",
        "metric": 32768
    },
    {
        "bucket": "$Reô2ñns&Õ\u0018À\u0096\u008c)Ü",
        "metric": 4400
    }
]
```

To see all supported flags for the local testing tool run
`java -jar LocalTestingTool_{version}.jar --help`, e.g. you can adjust the noising epsilon with the
`--epsilon` flag or disable noising all together with the `--no_noising` flag.

Note: The local testing tool also supports aggregation of
[Protected Audience](https://github.com/patcg-individual-drafts/private-aggregation-api#reports) and
[Shared Storage](https://github.com/patcg-individual-drafts/private-aggregation-api#reports)
reports. Simply pass the batch of FLEDGE or shared-storage unencrypted reports in the
--input_data_avro_file param.

## Local Testing tool flags and descriptions

```sh
$ java -jar LocalTestingTool_deploy.jar --help
Usage: Aggregation Library [options]
  Options:
    --input_data_avro_file
      Path to the local file which contains aggregate reports in avro format
      Default: <empty string>
    --domain_avro_file
      Path to the local file which contains the pre-listed keys in avro format
      Default: <empty string>
    --output_directory
      Path to the directory where the output would be written
      Default: <empty string>
    --epsilon
      Epsilon value for noise > 0 and <= 64
      Default: 10.0
    --print_licenses
      only prints licenses for all the dependencies.
      Default: false
    --help
      list all the parameters.
    --no_noising
      ignore noising and thresholding.
      Default: false
    --json_output
      output the result in json format.
      Default: false
```
