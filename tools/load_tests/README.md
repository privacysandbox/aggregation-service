# Aggregation Service Load Tests

Perform load testing on Aggregation Service. This uses the AWS Step Function. Lambdas are fanned out
and each lambda makes some number of requests to the service. The result is finally compiled and
returned.

Here are user parameter options:

1.  `base_url`: **REQUIRED**. Base URL of the Aggregation Service.
2.  `numWorkers`: **OPTIONAL**. Number of lambdas to be invoked in parallel. Default: 1.
3.  `numRequests`: **OPTIONAL**. Number of requests per lambda. Default: 1.
4.  `timeBetweenRequests`: **OPTIONAL**. Sleep time in between requests. Default: 10 seconds.
5.  `access_key`: **REQUIRED**. AWS access key.
6.  `secret_key`: **REQUIRED**. AWS secret access key.
7.  `host`: **REQUIRED**. Aggregation server host.
8.  `region`: **REQUIRED**. Aggregation service deployed region.
9.  `service`: **REQUIRED**. Aggregation service service.
10. `input_data_bucket_name`: **REQUIRED**.
11. `input_data_blob_prefix`: **REQUIRED**.
12. `output_data_bucket_name`: **REQUIRED**.
13. `output_data_blob_prefix`: **REQUIRED**.
14. `attribution_report_to`: **REQUIRED**.
15. `output_domain_bucket_name`: **REQUIRED**.
16. `output_domain_blob_prefix`: **REQUIRED**.
17. `debug_run`: **REQUIRED**.

## Set up

### Install SAM

Follow instructions here:
<https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html>

### Configure AWS profile

Make sure to have aws credentials configured.

### Make changes

1.  You can allocate more resources by updating the `template.yaml` file.
2.  You can modify the state machine by updating the
    `statemachine/AggregationServiceLoadTests.asl.json`.
3.  You can update files in `lambda/functions` to update the lambda functions to be deployed.

### Test

Unit tests are in `functions/*_test.py`. Once you've made changes, please run tests to ensure your
changes work. Tests are run inside docker with bazel. **NOTE** These tests are not run in CI so you
should run them to ensure everything works.

```sh
docker build -t load-test-image <path>/test_image/
docker run -i --rm --entrypoint=/bin/bash --workdir /src/workspace --volume ${HOME}/.cache/bazel:/root/.cache/bazel --volume $(git rev-parse --show-toplevel)/tools/load_tests:/src/workspace load-test-image -c "
  mv SPACE WORKSPACE
  bazel test //functions:unit_tests
  mv WORKSPACE SPACE
"
```

### Build

```sh
sam build
```

### Invoke lambda locally

Make sure you have docker running locally.

```sh
sam local invoke <lambda-name> -e <event-file>

sam local invoke TriggerJobs -e event.json
```

where `event.json` is something like

```json
{
  "numWorkers": "1",
  "numRequests": "1",
  "timeBetweenRequests": "1",
  "base_url": "https://pvnx5wx9sg.execute-api.us-east-1.amazonaws.com",
  "access_key": "key",
  "secret_key": "secret",
  "host": "pvnx5wx9sg.execute-api.us-east-1.amazonaws.com",
  "region": "us-east-1",
  "service": "execute-api"
  "input_data_blob_prefix": "",
  "input_data_bucket_name": "",
  "output_data_blob_prefix": "",
  "output_data_bucket_name": "",
  "attribution_report_to": "",
  "output_domain_blob_prefix": "",
  "output_domain_bucket_name": "",
  "debug_run": ""
}
```

An `event.json` for the `CheckJobsStatus` function would be

```json
[
    {
        "job_request_ids": ["aabbcc4002"],
        "base_url": "https://pvnx5wx9sg.execute-api.us-east-1.amazonaws.com",
        "host": "pvnx5wx9sg.execute-api.us-east-1.amazonaws.com",
        "region": "us-east-1",
        "access_key": "key",
        "secret_key": "secret",
        "service": "execute-api"
    }
]
```

### Deploy

```sh
sam deploy --guided
```
