# Aggregation Service API Documentation

## AWS Nitro Enclave based Aggregation Service

### createJob Endpoint

#### Endpoint

`https://<api-gateway>/stage/v1alpha/createJob`

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
  "input_data_blob_prefix": <string>,

  // Storage bucket for input data.
  "input_data_bucket_name": <string>,

  // The output data path in the bucket. Currently, single output file is
  // supported.
  "output_data_blob_prefix": <string>,

  // Storage bucket for output data.
  "output_data_bucket_name": <string>,

  // Parameters are required unless marked Optional.
  "job_parameters": {
    // For a single domain file, it's a file path in the bucket. For multiple
    // domain files, it's a prefix in the file path. For example, inputting
    // "folder1/shard" would include "folder1/shard/domain1.avro",
    // "folder1/shard_domain.avro" and "folder1/shard/folder2/domain.avro".
    "output_domain_blob_prefix": <string>,

    // Domain file bucket.
    "output_domain_bucket_name": <string>,

    // Reporting URL.
    "attribution_report_to": <string>,

    // [Optional] differential privacy epsilon value to be used
    // for this job. 0.0 < debug_privacy_epsilon <= 64.0. The
    // value can be varied so that tests with different epsilon
    // values can be performed during the origin trial.
    "debug_privacy_epsilon": <floating point, double>,
    // [Optional] The percentage of reports, if excluded from
    // aggregation due to an error, will fail the job.
    // Values can be from 0 to 100. If left empty, default value of 10%
    // will be used,
    "report_error_threshold_percentage": <double>
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

### getJob Endpoint

#### Endpoint

`https://<api-gateway>/stage/v1alpha/getJob`

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
    // [Optional] differential privacy epsilon value to be used
    // for this job. 0.0 < debug_privacy_epsilon <= 64.0. The
    // value can be varied so that tests with different epsilon
    // values can be performed during the origin trial. A greater
    // epsilon value results in less noise in the output. Default
    // value for epsilon is 10.
    "debug_privacy_epsilon": <floating point, double>,
    // [Optional] The percentage of reports, if excluded from
    // aggregation due to an error, will fail the job.
    // Values can be from 0 to 100. If left empty, default value of 10%
    // will be used.
    "report_error_threshold_percentage": <double>
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
[AggregationWorkerReturnCode.java](https://github.com/privacysandbox/aggregation-service/blob/main/java/com/google/aggregate/adtech/worker/AggregationWorkerReturnCode.java)
and `result_info.return_messages` will have the exception message followed by a few stack frames of
the exception stacktrace for debugging.

If an unexpected exception occurs, `result_info.error_summary.error_messages` will contain the error
messages.

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
