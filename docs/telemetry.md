# Telemetry in Aggregation Service

Aggregation Service exports the following metrics and traces through
[OpenTelemetry](https://opentelemetry.io/): CPU usage, memory, total execution time. These
metrics/traces can be helpful during the debugging process or when deciding on the appropriate cloud
[instance size](./sizing-guidance.md).

Metrics/traces:

-   CPU usage: measured in percentage and exported with an interval of one minute. CPU percentage is
    rounded to the nearest integer (e.g. 12.34% rounded to 12%).
-   Memory: measured in percentage and exported with an interval of one minute. Memory percentage is
    rounded to the nearest 10th (e.g. 12% rounded to 10%) and reported max to 90%.
-   Total execution time (in seconds): time spent in worker processing from the time job is picked
    for processing to its completion. This is generated per job.

Memory and CPU usage are tracked for each environment. For debugging purposes only, we recommend
setting the following in your terraform variables file (`{name}.auto.tfvars`) to debug issues with a
single cloud instance.

E.g. In terraform variables file (`{name}.auto.tfvars`):

-   For AWS, set `max_capacity_ec2_instances = "1"`
-   For GCP, set `max_worker_instances = "1"`

## How to enable metrics/traces collection

Please note that enabling metrics or traces may add extra cost to your cloud billing. We recommend
referring to cloud provider website for cost details.

The metrics and traces collection is disabled by default. To enable it, please add the metrics you
want to export in your terraform variables file (`{name}.auto.tfvars`) as shown here:

```sh
allowed_otel_metrics = ["cpu_usage", "memory", "total_execution_time"]
```

In this case, "cpu_usage", "memory" and "total_execution_time" would be exported.

## Where to find the metrics/traces:

The env_name here is the same as what was set in your Aggregation Service deployment terraform.

-   AWS
    -   "cpu_usage" and "memory" graphs can be found in Cloudwatch > all metrics > {env_name} >
        OTelLib.
    -   "total_execution_time" is exported to "Traces" in Cloudwatch. You can run a query with
        `annotation.job_id = {job_id}` to get traces for a specific job.
-   GCP
    -   "cpu_usage" and "memory" graphs can be found in Monitoring > Metric explorer > Generic
        Node > Process. You can put `custom_namespace={env_name}` in the filter to see the metrics
        from a specific environment.
    -   "total_execution_time" is exported to "Trace Explorer". You can set `job-id: {job_id}` in
        the filter to get traces for a specific job.
