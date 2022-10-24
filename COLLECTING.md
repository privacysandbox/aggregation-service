# Collecting and Batching Aggregatable Reports

This document provides instructions and code snippets
on how to collect, transform and batch [Aggregatable Reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
produced by the [Attribution Reporting API](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md)

The Attribution Reporting API can generate 4 possible types of reports during the
[Privacy Sandbox Relevance and Measurement origin trials (OT)](https://developer.chrome.com/origintrials/#/view_trial/771241436187197441).
These reports are sent to predefined endpoints to the domain registered during
source registration (such as <https://adtech.localhost>).
See this [demo](https://goo.gle/attribution-reporting-demo) for examples.

1. Event-level report
    - Reporting URL: `http://adtech.localhost/.well-known/attribution-reporting/report-event-attribution`
1. Event-level debug report
    - Reporting URL: `http://adtech.localhost/.well-known/attribution-reporting/debug/report-event-attribution`
1. Aggregatable report
    - Reporting URL: `http://adtech.localhost/.well-known/attribution-reporting/report-aggregate-attribution`
1. Aggregatable debug report
    - Reporting URL: `http://adtech.localhost/.well-known/attribution-reporting/debug/report-aggregate-attribution`

*The `.well-known/…` paths are predefined paths which can not be customized.
To collect reports, you need to run an endpoint that can respond to POST requests
on the above paths.*

## Aggregatable report sample

This is a sample aggregatable report produced with the
[Attribution Reporting API Demo](https://goo.gle/attribution-reporting-demo)
with debugging enabled.

```json
{
  "aggregation_service_payloads": [
    {
      "debug_cleartext_payload": "omRkYXRhgaJldmFsdWVEAACAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAFWWlvcGVyYXRpb25paGlzdG9ncmFt",
      "key_id": "e101cca5-3dec-4d4f-9823-9c7984b0bafe",
      "payload": "26/oZSjHABFqsIxR4Gyh/DpmJLNA/fcp43Wdc1/sblss3eAkAPsqJLphnKjAC2eLFR2bQolMTOneOU5sMWuCfag2tmFlQKLjTkNv85Wq6HAmLg+Zq+YU0gxF573yzK38Cj2pWtb65lhnq9dl4Yiz"
    }
  ],
  "attribution_destination": "http://shoes.localhost",
  "shared_info": "{\"debug_mode\":\"enabled\",\"privacy_budget_key\":\"OtLi6K1k0yNpebFbh92gUh/Cf8HgVBVXLo/BU50SRag=\",\"report_id\":\"00cf2236-a4fa-40e5-a7aa-d2ceb33a4d9d\",\"reporting_origin\":\"http://adtech.localhost:3000\",\"scheduled_report_time\":\"1649652363\",\"version\":\"\"}",
  "source_debug_key": "531933890459023",
  "source_registration_time": "1649635200",
  "source_site": "http://news.localhost",
  "trigger_debug_key": "531933890459023"
}
```

The `debug_cleartext_payload` field contains the base64 encoded [CBOR](https://cbor.io/)
payload. The above CBOR payload decodes into the following data in JSON format
(Decoded with [CBOR Playground](https://cbor.me)). The bucket value is encoded
as a sequence of 'characters' representing the underlying bytes. While some
bytes may be represented as ASCII characters, others are unicode escaped.

```json
{
  "data": [
    {
      "value": "\u0000\u0000\x80\u0000",
      "bucket": "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0005Y"
    }
  ],
  "operation": "histogram"
}
```

## Convert the aggregatable report into Avro binary representation

The [sample report](#aggregatable-report-sample) lists a `debug_cleartext_payload`
field that is *not* encrypted and can be processed with the
[local testing tool](https://aggregation-service-published-artifacts.s3.amazonaws.com/aggregation-service/0.4.0/LocalTestingTool_0.4.0.jar).

When testing the aggregation service locally and on Amazon Web Services
[Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/),
an [Avro](https://avro.apache.org/) batch with the following record schema is
expected.

### `reports.avsc`

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

For local testing, the avro `payload` field expects a byte array of the
`debug_cleartext_payload` field (`base64` encoded). The `debug_cleartext_payload`
field is present in each aggregation service payload object in the
`aggregation_service_payloads` list of an aggregatable report with debugging
enabled.

For testing with encrypted reports on the Amazon Web Services
[Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/), the avro
`payload` field expects a byte array of the aggregatable report's
`aggregation_service_payloads` object's `payload` field.

## Collect, transform and batch reports

The following code snippets are in Golang, but can be adapted to other
programming languages.

### Listen on predefined endpoints

When debugging is enabled for the Attribution Reporting API, additional fields
are present in the reports, and a duplicate debug report is sent immediately.
The following 2 predefined endpoints are used:

1. `.well-known/attribution-reporting/report-aggregate-attribution` for regular,
scheduled (delayed) reports with encrypted payloads. If debugging is enabled,
these will contain additional fields: for example, a cleartext payload if both
debug keys are also set.
2. `.well-known/attribution-reporting/debug/report-aggregate-attribution` for
debug reports that are duplicates of the regular reports, but sent immediately
at generation time.

First, lets define all types we will work with:

- Aggregatable report generated from the Attribution Reporting API

    ```go
    // AggregatableReport contains the information generated by the Attribution
    // Reporting API in the browser
    type AggregatableReport struct {
      SourceSite             string `json:"source_site"`
      AttributionDestination string `json:"attribution_destination"`
      // SharedInfo is a JSON serialized instance of struct SharedInfo.
      // This exact string is used as authenticated data for decryption. The string
      // therefore must be forwarded to the aggregation service unmodified. The
      // reporting origin can parse the string to access the encoded fields.
      // https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports
      SharedInfo                 string                       `json:"shared_info"`
      AggregationServicePayloads []*AggregationServicePayload `json:"aggregation_service_payloads"`

      SourceDebugKey  uint64 `json:"source_debug_key,string"`
      TriggerDebugKey uint64 `json:"trigger_debug_key,string"`
    }

    // AggregationServicePayload contains the payload for the aggregation server.
    type AggregationServicePayload struct {
      // Payload is a encrypted CBOR serialized instance of struct Payload, which is base64 encoded.
      Payload               string `json:"payload"`
      KeyID                 string `json:"key_id"`
      DebugCleartextPayload string `json:"debug_cleartext_payload,omitempty"`
    }
    ```

- Aggregatable report in Avro format, as expected by the aggregation service (you'll
need to import [gopkg.in/avro.v0](https://pkg.go.dev/gopkg.in/avro.v0))

    ```go
    // AvroAggregatableReport format expected by aggregation service and local testing tool
    type AvroAggregatableReport struct {
      Payload    []byte `avro:"payload"`
      KeyID      string `avro:"key_id"`
      SharedInfo string `avro:"shared_info"`
    }
    ```

Now let's register request handlers and start an http server:

  ```go
  func main() {
    http.HandleFunc("/.well-known/attribution-reporting/report-aggregate-attribution", collectEndpoint)
    http.HandleFunc("/.well-known/attribution-reporting/debug/report-aggregate-attribution", collectEndpoint)
    var address = ":3001"
    log.Printf("Starting Collector on address %v", address)
    log.Fatal(http.ListenAndServe(address, nil))
  }
  ```

And here is how we handle incoming reports in our `HandlerFunc` implementation:

```go
func collectEndpoint(w http.ResponseWriter, r *http.Request) {
 var timeStr = time.Now().Format(time.RFC3339)
 if r.Method == "POST" {
  var endpoint = "regular"
  // check if report was an immediately sent one to the debug endpoint
  if strings.Contains(r.URL.Path, ".well-known/attribution-reporting/debug/report-aggregate-attribution") {
   endpoint = "debug"
  }

  log.Printf("Received Aggregatable Report on %s endpoint", endpoint)
  report := &AggregatableReport{}
  buf := new(bytes.Buffer)
  buf.ReadFrom(r.Body)
  log.Print(buf.String())
  if err := json.Unmarshal(buf.Bytes(), report); err != nil {
   errMsg := "Failed in decoding aggregation report"
   http.Error(w, errMsg, http.StatusBadRequest)
   log.Printf(errMsg+" %v", err)
   return
  }
  schema, err := avro.ParseSchema(reports_avsc)
  check(err)

  f, err := os.Create(fmt.Sprintf("output_%s_reports_%s.avro", endpoint, timeStr))
  check(err)
  defer f.Close()

  w := bufio.NewWriter(f)
  writer, err := avro.NewDataFileWriter(w, schema, avro.NewSpecificDatumWriter())
  check(err)

  var dwriter *avro.DataFileWriter
  var dw *bufio.Writer
  if (len(report.AggregationServicePayloads) > 0 && len(report.AggregationServicePayloads[0].DebugCleartextPayload) > 0) {
   df, err := os.Create(fmt.Sprintf("output_%s_clear_text_reports_%s.avro", endpoint, timeStr))
   check(err)
   defer df.Close()

   dw = bufio.NewWriter(df)
   dwriter, err = avro.NewDataFileWriter(dw, schema, avro.NewSpecificDatumWriter())
   check(err)
  }

  for _, payload := range report.AggregationServicePayloads {
   var payload_cbor []byte
   var err error

   payload_cbor, err = b64.StdEncoding.DecodeString(payload.Payload)
   check(err)
   avroReport := &AvroAggregatableReport{
    Payload:    []byte(payload_cbor),
    KeyID:      payload.KeyID,
    SharedInfo: report.SharedInfo,
   }

   if err := writer.Write(avroReport); err != nil {
    log.Fatal(err) // i/o errors OR encoding errors
   }

   if len(payload.DebugCleartextPayload) > 0 {
    payload_debug_cbor, err := b64.StdEncoding.DecodeString(payload.DebugCleartextPayload)
    check(err)
    avroDReport := &AvroAggregatableReport{
     Payload:    []byte(payload_debug_cbor),
     KeyID:      payload.KeyID,
     SharedInfo: report.SharedInfo,
    }
    if err := dwriter.Write(avroDReport); err != nil {
     log.Fatal(err) // i/o errors OR encoding errors
    }
   }
  }
  writer.Flush()
  w.Flush()
  if dwriter != nil {
   dwriter.Flush()
   dw.Flush()
  }

 } else {
  http.Error(w, "Invalid request method.", http.StatusMethodNotAllowed)
  log.Print("Invalid request received.")
 }
```

Once an aggregatable report has been collected, it'll be stored in the
`output_regular_reports_<timestamp>.avro` and `output_regular_clear_text_reports_<timestamp>.avro`
for report received on the `.well-known/attribution-reporting/report-aggregate-attribution`
endpoint and `output_debug_reports_<timestamp>.avro` and `output_debug_clear_text_reports_<timestamp>.avro`
for report received on the `.well-known/attribution-reporting/debug/report-aggregate-attribution`
endpoint respectively.

## Process Avro batch files

To process the above Avro files, you must specify the expected bucket keys
in a domain file `output_domain.avro` with the following Avro schema.

### `output_domain.avsc`

```avro
{
  "type": "record",
  "name": "AggregationBucket",
  "fields": [
    {
      "name": "bucket",
      "type": "bytes",
      "doc": "A single bucket that appears in the aggregation service output. 128-bit integer encoded as a 16-byte big-endian bytestring."
    }
  ]
}
```

### Generate a output domain Avro file

You can use the [Avro Tools](https://www.apache.org/dyn/closer.cgi/avro/) to
generate a `output_domain.avro` from a JSON input file.

You can download the Avro Tools jar 1.11.0 [here](https://dlcdn.apache.org/avro/avro-1.11.0/java/avro-tools-1.11.0.jar)

We use the following `output_domain.json` input file to generate our
`output_domain.avro` file. This uses the bucket from the above
[sample aggregatable report](#aggregatable-report-sample). The below sample uses
unicode escaped "characters" to encode the byte array bucket value.

```json
{
  "bucket": "\u0005Y"
}
```

To generate the `output_domain.avro` file use the above JSON file and domain schema file:

```sh
java -jar avro-tools-1.11.0.jar fromjson \
--schema-file output_domain.avsc output_domain.json > output_domain.avro
```

### Produce a summary report locally

Using the [local testing tool](https://aggregation-service-published-artifacts.s3.amazonaws.com/aggregation-service/0.4.0/LocalTestingTool_0.4.0.jar),
you now can generate a summary report. [See all flags and descriptions](./API.md#local-testing-tool)

*Note: The `SHA256` of the `LocalTestingTool_{version}.jar` is `{LocalTestingTool_{version}.jar-SHA}`
obtained with `openssl sha256 <jar>`.*

We will run the tool, without adding noise to the summary report, to receive the
expected value of `32768` from the [sample aggregatable report](#aggregatable-report-sample).

```sh
java -jar LocalRunner_deploy.jar \
--input_data_avro_file output_debug_reports_<timestamp>.avro \
--domain_avro_file output_domain.avro \
--json_output \
--no_noising
```

The output of above tool execution will be in `output.json` with the following
content

```json
[
  {
    "bucket" : "\u0005Y",
    "value" : 32768
  }
]
```
