# Aggregation Service API Documentation

### createJob Endpoint

#### Endpoint

AWS: `https://<api-gateway>/stage/v1alpha/createJob`

GCP: `https://<cloud-run-endpoint>/v1alpha/createJob`

#### Protocol

HTTPS

#### Method

POST

#### Payload

```jsonc
{
  // Unique identifier. Length must be 128 characters or less.
  // Legal characters are ASCII letters (a-z, A-Z), ASCII
  // numbers (0-9), and the following ASCII punctuation
  // characters !"#$%&'()*+,-./:;<=>?@[\]^_`{}~.
  "job_request_id": <string>,

  // For a single file, it's a file path in the bucket. For multiple input
  // files, it's a prefix in the file path. For example, inputting
  // "folder1/shard" would take all files with paths starting with
  // "folder1/shard" such as "folder1/shard1.avro", "folder1/shard/test1.avro"
  // and "folder1/shard1/folder2/test1.avro".
  // It is recommended to keep the number of shards between the number of CPUs
  // available to the enclave and 1000.
  "input_data_blob_prefix": <string>,

  // Storage bucket for input data.
  "input_data_bucket_name": <string>,

  // The output data path in the bucket.
  // The output file will be named as follows:
  //     [OutputDataBlobPrefix]-[ShardId]-of-[TotalShards]
  // In the case of a single shard, the output file will still apply the
  // shard suffix information as "[OutputDataBlobPrefix]-1-of-1".
  // If "output_data_blob_prefix" includes the Avro file extension (.avro),
  // the output shard names will also include the Avro file extension at
  // the end.
  "output_data_blob_prefix": <string>,

  // Storage bucket for output data.
  "output_data_bucket_name": <string>,

  // Parameters are required unless marked Optional.
  "job_parameters": {
    // For a single domain file, it's a file path in the bucket. For multiple
    // domain files, it's a prefix in the file path. For example, inputting
    // "folder1/shard" would include "folder1/shard/domain1.avro",
    // "folder1/shard_domain.avro" and "folder1/shard/folder2/domain.avro".
    // It is recommended to keep the number of shards between the number of CPUs
    // available to the enclave and 1000.
    "output_domain_blob_prefix": <string>,

    // Domain file bucket.
    "output_domain_bucket_name": <string>,

    // Reporting URL.
    // This should be same as the reporting_origin present in the reports' shared_info.
    "attribution_report_to": <string>,

    // [Optional] Reporting Site.
    // This should be the reporting site that is onboared to aggregation service.
    // Note: All reports in the request should have reporting origins which
    // belong to the reporting site mentioned in this parameter. This parameter
    // and the "attribution_report_to" parameter are mutually exclusive, exactly
    // one of the two parameters should be provided in the request.
    "reporting_site": "<string>"

    // [Optional] Differential privacy epsilon value to be used
    // for this job. 0.0 < debug_privacy_epsilon <= 64.0. The
    // value can be varied so that tests with different epsilon
    // values can be performed during the origin trial.
    "debug_privacy_epsilon": <double value represented as string>,

    // [Optional] The percentage of reports, if excluded from
    // aggregation due to an error, will fail the job.
    // Values can be from 0 to 100. If left empty, default value of 10%
    // will be used,
    "report_error_threshold_percentage": <double value represented as string>,

    // [Optional] Total number of reports provided as input data for this job.
    // This value, in conjunction with "report_error_threshold_percentage" will
    // enable early failure of the job when reports are excluded due to errors.
    "input_report_count": <long value represented as string>,

    // [Optional] A list of unsigned filtering IDs separated by comma. All the
    // contribtions other than the matching filtering ID will be filtered out.
    // e.g. "filtering_ids":"12345,34455,12". Default value is "0".
    "filtering_ids":<string>,

    // [Optional] When executing a debug run, noised and unnoised debug summary
    // report and annotations are added to indicate which keys are present in the
    // domain input and/or reports. Additionally, duplicates across batches are
    // also not enforced. Note that the debug run only considers reports that have the flag
    // "debug_mode": "enabled". Read /docs/debugging.md for details.
    "debug_run": <boolean value represented as string>
  }
}
```

Please see [Appendix](/docs/api.md#appendix) for input and output file format details.

#### Response HTTP codes

```txt
Success: 202 Accepted
Bad request (malformed): 400 Bad Request
Duplicate job (job_request_id already taken): 409 Conflict
```

#### Success Response Payload

`{} // Empty object in response body for success`

#### Error Response body

These match the [Google Cloud Error Model](https://cloud.google.com/apis/design/errors#error_model)

```jsonc
{
    "error": {
        "code": 3,
        // Corresponds to this
        "message": "detailed error message string",
        "status": "INVALID_ARGUMENT",
        "details": [
            {
                "reason": "API_KEY_INVALID",
                "domain": "foo.com",
                // might not be present
                "metadata": {
                    // Map<String, String>, might not be present
                    "service": "translate.googleapis.com"
                }
            }
        ]
    }
}
```

# Job Request Validations

These are the validations that are done before the aggregation begins.

1. **Job request is valid**\
   i. Job request is not empty.\
   ii. Job should have a job_request_id.

2. **Job parameters are valid**\
   All required `request_info.job_parameters` map entries must be set with valid values. \
   Please see the createJob request parameter documentation above for more details.
3. Job request's `job_parameters.attribution_report_to` value should match Aggregatable Report's
   `shared_info.reporting_origin`. Reports that fail this validation are counted in
   ATTRIBUTION_REPORT_TO_MISMATCH error counter. Aggregatable report validations and error counters
   can be found in the
   [Input Aggregatable Report Validations](#input-aggregatable-report-validations) below
4. Job request's `job_parameters` should contain exactly one of `attribution_report_to` and
   `reporting_site`.
5. If `job_parameters.reporting_site` is provided, `shared_info.reporting_origin` of all
   aggregatable reports should belong to this reporting site.

Return code:
[INVALID_JOB](java/com/google/aggregate/adtech/worker/AggregationWorkerReturnCode.java#L38)

### getJob Endpoint

#### Endpoint

AWS: `https://<api-gateway>/stage/v1alpha/getJob`

GCP: `https://<cloud-run-endpoint>/v1alpha/getJob`

#### Protocol

HTTPS

#### Method

GET

#### Request params

`"job_request_id": <string>`

#### Response HTTP code

```txt
Found: 200 Ok
Not found: 404 Not Found
```

#### Response Body

```jsonc
{
  // Unique identifier
  "job_request_id" : "UNIQUEID12313",
  // <RECEIVED, IN_PROGRESS, or FINISHED>
  "job_status": <string>,
  // Time request was received
  "request_received_at": <timestamp>,
  // Last update time
  "request_updated_at": <timestamp>,
  // Location of input reports
  "input_data_blob_prefix": <string>,
  "input_data_bucket_name": <string>,
  // Location of output summary report
  "output_data_blob_prefix": <string>,
  "output_data_bucket_name": <string>,
  // Only present when job is finished
  "result_info": {
    "return_code": <string>,
    // Debug information
    "return_message": <string>,
    "finished_at": <timestamp>,
    "error_summary": {
      "error_counts": [
        {
          "category": <string>,
          "count": <int>
        },
        ...
      ]
    }
  },
  "job_parameters": {
    // Location of pre-listed aggregation buckets
    "output_domain_blob_prefix": <string>,
    "output_domain_bucket_name": <string>,
    // Reporting URL
    "attribution_report_to" : <string>,
    // [Optional] Reporting site value from the CreateJob request, if provided.
    "reporting_site": <string>
    // [Optional] differential privacy epsilon value to be used
    // for this job. 0.0 < debug_privacy_epsilon <= 64.0. The
    // value can be varied so that tests with different epsilon
    // values can be performed during the origin trial. A greater
    // epsilon value results in less noise in the output. Default
    // value for epsilon is 10.
    "debug_privacy_epsilon": <double value represented as string>,
    // [Optional] The percentage of reports, if excluded from
    // aggregation due to an error, will fail the job.
    // Values can be from 0 to 100. If left empty, default value of 10%
    // will be used.
    "report_error_threshold_percentage": <double value represented as string>,
    // [Optional] Total number of reports provided as input data for this job.
    // This value, in conjunction with "report_error_threshold_percentage" will
    // enable early failure of the job when reports are excluded due to errors.
    "input_report_count": <long value represented as string>,
    // [Optional] A list of unsigned filtering IDs separated by comma. All the
    // contribtions other than the matching filtering ID will be filtered out.
    // e.g. "filtering_ids":"12345,34455,12". Default value is "0".
    "filtering_ids":<string>,
  },
  // The time when worker starts processing request in the latest processing
  // attempt
  // If the job_status is set to `FINISHED`, one can calculate the request's
  // processing time in worker (excludes the time spent waiting in job queue)
  // as `request_updated_at` minus `request_processing_started_at`.
  "request_processing_started_at": <timestamp>
}
```

#### Error Response codes

In case of errors in successfully completing the job, the below error response codes would be
present in the `result_info` section of the GetJob API response body

-   RETRIES_EXHAUSTED: The aggregation request failed because it exhausted the number of retries
    attempted. This error is transient and the job can be retried.

If the job fails due to a handled exception, then `result_info.return_code` will have the
corresponding error code in
[AggregationWorkerReturnCode.java](../java/com/google/aggregate/adtech/worker/AggregationWorkerReturnCode.java)
and `result_info.return_messages` will have the exception message followed by a few stack frames of
the exception stacktrace for debugging.

If an unexpected exception occurs, `result_info.error_summary.error_messages` will contain the error
messages.

#### Error Counts - error_counts

The count of reports that are excluded from aggregation due to errors can be found in the getJob
response `result_info.error_summary.error_counts` with the `category` field giving the error code
and `count` giving the number of reports in that category.

[Error categories and description](../java/com/google/aggregate/adtech/worker/model/ErrorCounter.java)

If the total number of reports with errors exceeds 10% of the total report count, then the job will
fail early with return code
[REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD](../java/com/google/aggregate/adtech/worker/AggregationWorkerReturnCode.java#L88)
before the privacy budget is consumed. The threshold percentage can be set in
job.request_info.job_parameters for the key "report_error_threshold_percentage".

#### Error Response body

These match the [Google Cloud Error Model](https://cloud.google.com/apis/design/errors#error_model)

```jsonc
{
    "error": {
        "code": 3,
        // Corresponds to this
        "message": "detailed error message string",
        "status": "INVALID_ARGUMENT",
        "details": [
            {
                "reason": "API_KEY_INVALID",
                "domain": "foo.com",
                // might not be present
                "metadata": {
                    // Map<String, String>, might not be present
                    "service": "translate.googleapis.com"
                }
            }
        ]
    }
}
```

### Appendix

#### Input File format

Aggregation service expects input aggregatable reports batched in the
[Avro](https://avro.apache.org/) format given below. Please see
[Collecting and Batching Aggregatable Reports](/docs/collecting.md) for how to generate avro
aggregatable reports using collected data.

#### [reports.avsc](/protocol/avro/reports.avsc)

```avro
{
  "type": "record",
  "name": "AggregatableReport",
  "fields": [
    {
      "name": "payload",
      "type": "bytes"
    },
    {
      "name": "key_id",
      "type": "string"
    },
    {
      "name": "shared_info",
      "type": "string"
    }
  ]
}
```

Additionally a domain file is needed to declare all expected aggregation keys for aggregating the
aggregatable reports (keys not listed in the domain file won't be aggregated)

#### [output_domain.avsc](/protocol/avro/output_domain.avsc)

```avro
{
 "type": "record",
 "name": "AggregationBucket",
 "fields": [
   {
     "name": "bucket",
     "type": "bytes"
     /* A single bucket that appears in
     the aggregation service output.
     128-bit integer encoded as a
     16-byte big-endian byte string. */
   }
 ]
}
```

#### Output File format

Summary report is the output file generated by the Aggregation Service and is also in avro format
with the following schema -

#### [results.avsc](/protocol/avro/results.avsc)

```avro
{
  "type": "record",
  "name": "AggregatedFact",
  "fields": [
    {
      "name": "bucket",
      "type": "bytes",
      "doc": "Histogram bucket used in aggregation. 128-bit integer encoded as a 16-byte big-endian bytestring. Leading 0-bits will be left out."
    },
    {
      "name": "metric",
      "type": "long",
      "doc": "Metric associated with the bucket"
    }
  ]
}
```

#### Input Aggregatable Report Validations

Input aggregatable report is required to have a valid and/or supported value for the following
report shared_info fields: `api`, `report_id`, `reporting_origin` and `scheduled_report_time`.
Invalid aggregatable reports will not be included in the aggregation. getJob response
`result_info.error_summary.error_counts` will have error counters for these invalid reports along
with the error code, reason and count of reports in each category.

If the invalid reports in a job exceed the `report_error_threshold_percentage` (see the
[createJob](#createjob-endpoint) request job parameters above), the job will fail with
REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD error.

A summary of all report error counters along with their mitigations can be found in
[Aggregation Service Report Error Codes and Mitigations](/docs/error-codes-and-mitigation.md#aggregation-service-report-error-codes-and-mitigations).

If report `shared_info.version` is higher than supported major version, the aggregation job will
fail without consuming privacy budget with `result_info.return_code` UNSUPPORTED_REPORT_VERSION.
