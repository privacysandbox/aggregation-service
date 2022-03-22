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
  // characters !"#$%&'()*+,-./:;<=>?@[\]^_`{}~
  "job_request_id": <string>,
  // input file bucket and path in bucket, can be prefix for
  // sharded inputs
  "input_data_blob_prefix": <string>,
  "input_data_bucket_name": <string>,
  // output file bucket and path in bucket, can be prefix for
  // sharded outputs
  "output_data_blob_prefix": <string>,
  // output data bucket
  "output_data_bucket_name": <string>,
  "job_parameters": {
    // location of pre-listed aggregation buckets
    "output_domain_blob_prefix": <string>,
    "output_domain_bucket_name": <string>,
    // reporting URL
    "attribution_report_to": <string>,
    // [Optional] privacy budget limit to be enforced.
    // Used for debugging during the origin trial
    // 0 < debug_privacy_budget_limit < (2^31)-1
    "debug_privacy_budget_limit": <integer>,
    // [Optional] differential privacy epsilon value to be used
    // for this job. 0.0 < debug_privacy_epsilon <= 64.0. The
    // value can be varied so that tests with different epsilon
    // values can be performed during the origin trial.
    "debug_privacy_epsilon": <floating point, double>
  }
}
```

#### Response HTTP codes

```txt
Success: 202 Accepted
Bad request (malformed): 400 Bad Request
Duplicate job (job_request_id already taken): 409 Conflict
```

#### Success Response Payload

`{​} // Empty object in response body for success`

#### Error Response body

These match the [Google Cloud Error Model](https://cloud.google.com/apis/design/errors#error_model)

```jsonc
{
  "error": {
    "code": 3,
    // Corresponds to this
    "message": "detailed error message string",
    "status": "INVALID_ARGUMENT",
    "details": [{
      "reason": "API_KEY_INVALID",
      "domain": "foo.com",
      // might not be present
      "metadata": {
        // Map<String, String>, might not be present
        "service": "translate.googleapis.com"
      }
    }]
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
  // <RECEIVED, IN_PROGRESS, or FINISHED>,
  "job_status": <string>,
  // time req was received
  "request_received_at": <timestamp>,
  // last update time
  "request_updated_at": <timestamp>,
  "input_data_blob_prefix": <string>,
  "input_data_bucket_name": <string>,
  "output_data_blob_prefix": <string>,
  "output_data_bucket_name": <string>,
  // Only present when job is finished
  "result_info": {
    "return_code": <string>,
    // Debug information
    "return_message": <string>,
    "finished_at": <timestamp>,
    "error_summary": {
      "num_reports_with_errors": <int>,
      "error_counts": [
        {
          "category": <string>,
          "count": <int>
        },
        …
      ]
    }
  }
  "job_parameters": {
    // location of pre-listed aggregation buckets
    "output_domain_blob_prefix": <string>,
    "output_domain_bucket_name": <string>,
    // reporting URL
    "attribution_report_to" : <string>,
    // [Optional] privacy budget limit to be enforced.
     // Used for debugging during origin trial
    "debug_privacy_budget_limit": <integer>,
    // [Optional] differential privacy epsilon value to be used
    // for this job. 0.0 < debug_privacy_epsilon <= 64.0. The
    // value can be varied so that tests with different epsilon
    // values can be performed during the origin trial. A greater
    // epsilon value results in less noise in the output. Default
    // value for epsilon is 10.
    "debug_privacy_epsilon": <floating point, double>
  }
}
```

#### Error Response body

These match the [Google Cloud Error Model](https://cloud.google.com/apis/design/errors#error_model)

```jsonc
{
  "error": {
    "code": 3,
    // Corresponds to this
    "message": "detailed error message string",
    "status": "INVALID_ARGUMENT",
    "details": [{
      "reason": "API_KEY_INVALID",
      "domain": "foo.com",
      // might not be present
      "metadata": {
        // Map<String, String>, might not be present
        "service": "translate.googleapis.com"
      }
    }]
  }
}
```

## Local testing tool

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
