# Collecting and Batching Aggregatable Reports

This document provides instructions and code snippets on how to collect, transform and batch
[Aggregatable Reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
produced by the
[Attribution Reporting API](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md)
and
[Private Aggregation API](https://github.com/patcg-individual-drafts/private-aggregation-api#reports).

The Attribution Reporting API can generate 4 possible types of reports during the
[Privacy Sandbox Relevance and Measurement origin trials (OT)](https://developer.chrome.com/origintrials/#/view_trial/771241436187197441).
These reports are sent to predefined endpoints to the domain registered during source registration
(such as <https://adtech.localhost>). See this [demo](https://goo.gle/attribution-reporting-demo)
for examples.

1. Event-level report
    - Reporting URL:
      `http://adtech.localhost/.well-known/attribution-reporting/report-event-attribution`
1. Event-level debug report
    - Reporting URL:
      `http://adtech.localhost/.well-known/attribution-reporting/debug/report-event-attribution`
1. Aggregatable report
    - Reporting URL:
      `http://adtech.localhost/.well-known/attribution-reporting/report-aggregate-attribution`
1. Aggregatable debug report
    - Reporting URL:
      `http://adtech.localhost/.well-known/attribution-reporting/debug/report-aggregate-attribution`

_The `.well-known/...` paths are predefined paths which can not be customized. To collect reports,
you need to run an endpoint that can respond to POST requests on the above paths._

## Aggregatable report sample

This is a sample aggregatable report produced with the
[Attribution Reporting API Demo](https://goo.gle/attribution-reporting-demo) with debugging enabled.

```json
{
    "aggregation_coordinator_origin": "https://publickeyservice.msmt.aws.privacysandboxservices.com",
    "aggregation_service_payloads": [
        {
            "debug_cleartext_payload": "omRkYXRhlKJldmFsdWVEAACAAGZidWNrZXRQPPhnkD+7c+wm1RjAlowp3KJldmFsdWVEAAARMGZidWNrZXRQJFJl9DLxbnMm1RjAlowp3KJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAKJldmFsdWVEAAAAAGZidWNrZXRQAAAAAAAAAAAAAAAAAAAAAGlvcGVyYXRpb25paGlzdG9ncmFt",
            "key_id": "27c22e1b-dc77-4fc0-aee7-4d291262071c",
            "payload": "vxia0shX3KjvsgdNWZdznk2IUWtOi8+fZCTGFniGeRTzAv5LgSeGLd3PhDANDyazjDNSm8GxjtsPmoG08rd5nL8+qc6NwKJfwnAQUVgg/iCS8alpCcrD9aTSTyjeUdrJBmfwvjjydpm6NA6o+RVbpv1N7v+Z3arSvUzqw3k3UfGy12n10+TkaH/ymCo5Ke/9mbSwxy68aJfAGutEt91fo7qHtvvrTAULBIgQTgNsy9LkSeCTQ2YeEr7wyxBssZLrPEanjh6LGtzj2gXRiz3OiVAqXxnSn7eW8K9V9TosFsIVHijk9o5oz6+9LhkDx5SHHvx048m5OqyFe48KSBAlMcHHSSk+GCwQtHcHWgSVb8TmHDE9UnTc7+tSR60IK/UK4351my7aEaKaacF3q28pYjjoy92idwzcw0IWUUMAb4c1z1RetxZ602txSnjOsFLUcW02ReeUJcgynbi5M/DIbpltqTlg2FeSsMzLvf0yGvP6NDUF6jqgaD9kfHcLgwLR61G/SnjXVtdUU31VdS5bPMyrfBthesHytmNFKQQtBqLZ32uyf60NKed9xZku70IW0r/z06hPlvRG+ME+FqPEgbWC/YsBXv/Ua3wPjQ1Q3/BvQWHIPuaEyA2aeDilE0h3wfHMLsGbHxq/5Tw+pqwR12g069Qf5jiYyCiBdo96Vd40iKbZL4DVt85QHevpHFQYTmANBHYqW09Gl3TWnay8BgbIE+38IX2O9lwR59tiRCXFNwLmE6nUtGjfTBFpz1VCIVRos9K14tE4yGfxyyNVt8dHy6CFABG5wYtnT/+izKMhPzZCVA65wqaKEehk26+inHa4GzXCJfWvX5QNV1FxEaSrktM22/91NWsQXyMC2fT7NtK7MEKsXSmkqL0VdlxNR4b6WCz2yd5hVufSKZA8e4Wfljr43Lc22om54o2Y6qtteoIsj9FhJbHmnke/NbrbmEPf9Jk37d/KxGu96X0E46ACA4WQa18hBxKTir+LKI0g+rXJlfwUn4vvNZz1vInX7VUHelZi0mpVylFD5XP09O0PX4oUPx+WVCCf"
        }
    ],
    "shared_info": "{\"api\":\"attribution-reporting\",\"attribution_destination\":\"https://arapi-advertiser.web.app\",\"debug_mode\":\"enabled\",\"report_id\":\"6334058b-301d-40c8-be58-3f63eed454a1\",\"reporting_origin\":\"https://arapi-adtech.web.app\",\"scheduled_report_time\":\"1700089276\",\"source_registration_time\":\"0\",\"version\":\"0.1\"}",
    "source_debug_key": "685645209142579",
    "trigger_debug_key": "685645209142579"
}
```

The `debug_cleartext_payload` field contains the base64 encoded [CBOR](https://cbor.io/) payload.
The above CBOR payload decodes into the following data in JSON format (Decoded with
[CBOR Playground](https://cbor.me)). The bucket value is encoded as a sequence of 'characters'
representing the underlying bytes. While some bytes may be represented as ASCII characters, others
are unicode escaped.

Using CBOR, you will get the bucket and value in hex format. You can convert the value into decimal
while the bucket can be converted into an escaped unicode format by converting the characters into
ASCII or using the JavaScript code below.

```json
{
    "data": [
        {
            "value": h'00008000',
            "bucket": h'3CF867903FBB73EC26D518C0968C29DC'
        },
        {
            "value": h'00001130',
            "bucket": h'245265F432F16E7326D518C0968C29DC'
        },
        {
            "value": h'00000000',
            "bucket": h'00000000000000000000000000000000'
        },
        ...
    ],
    "operation": "histogram"
}
```

```javascript
function hexToAscii(hexString) {
    if (hexString.length % 2 != 0) {
        hexString = '0' + hexString;
    }
    let asciiStr = '';
    for (let i = 0; i < hexString.length; i += 2) {
        asciiStr += String.fromCharCode(parseInt(hexString.substr(i, 2), 16));
    }
    return asciiStr;
}
```

## Convert the aggregatable report into Avro binary representation

Both the local testing tool and the aggregation service running on AWS Nitro Enclave expect
aggregatable reports batched in the [Avro](https://avro.apache.org/) format given below.

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

The [sample report](#aggregatable-report-sample) lists a `debug_cleartext_payload` field that is
_not_ encrypted and can be processed with the [local testing tool](/docs/local-testing-tool.md).

For local testing, the avro `payload` field expects a byte array of the `debug_cleartext_payload`
field (`base64` encoded). The `debug_cleartext_payload` field is present in each aggregation service
payload object in the `aggregation_service_payloads` list of an aggregatable report with debugging
enabled.

For testing with encrypted reports on the Amazon Web Services
[Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/), the avro `payload` field expects
a byte array of the aggregatable report's `aggregation_service_payloads` object's `payload` field.

## Collect, transform and batch reports

The following code snippets are in Golang, but can be adapted to other programming languages.

### Listen on predefined endpoints

When debugging is enabled for the Attribution Reporting API, additional fields are present in the
reports, and a duplicate debug report is sent immediately. The following 2 predefined endpoints are
used:

1. `.well-known/attribution-reporting/report-aggregate-attribution` for regular, scheduled (delayed)
   reports with encrypted payloads. If debugging is enabled, these will contain additional fields:
   for example, a cleartext payload if both debug keys are also set.
2. `.well-known/attribution-reporting/debug/report-aggregate-attribution` for debug reports that are
   duplicates of the regular reports, but sent immediately at generation time.

First, lets define all types we will work with:

-   Aggregatable report generated from the Attribution Reporting API

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

-   Aggregatable report in Avro format, as expected by the aggregation service (you'll need to
    import [gopkg.in/avro.v0](https://pkg.go.dev/gopkg.in/avro.v0))

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
for report received on the `.well-known/attribution-reporting/report-aggregate-attribution` endpoint
and `output_debug_reports_<timestamp>.avro` and `output_debug_clear_text_reports_<timestamp>.avro`
for report received on the `.well-known/attribution-reporting/debug/report-aggregate-attribution`
endpoint respectively.

## Process Avro batch files

To process the above Avro files, you must specify the expected bucket keys in a domain file. The
bucket values are 128-bit integer encoded as a 16-byte big-endian bytestring. `output_domain.avro`
with the following Avro schema.

The schema for output domain file is provided below -

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

You can use the [Avro Tools](https://www.apache.org/dyn/closer.cgi/avro/) to generate a
`output_domain.avro` from a JSON input file.

You can download the Avro Tools jar 1.11.1
[here](http://archive.apache.org/dist/avro/avro-1.11.1/java/avro-tools-1.11.1.jar)

We use the following `output_domain.json` input file to generate our `output_domain.avro` file. This
uses the bucket from the above [sample aggregatable report](#aggregatable-report-sample). The below
sample uses unicode escaped "characters" to encode the byte array bucket value.

```json
{
    "bucket": "<øg\u0090?»sì&Õ\u0018À\u0096\u008c)Ü"
}
```

To generate the `output_domain.avro` file use the above JSON file and domain schema file:

```sh
java -jar avro-tools-1.11.1.jar fromjson \
--schema-file output_domain.avsc output_domain.json > output_domain.avro
```

Another sample of a valid output domain json file -

```json
{
    "bucket": "\u003c\u00f8\u0067\u0090\u003f\u00bb\u0073\u00ec\u0026\u00d5\u0018\u00c0\u0096\u008c\u0029\u00dc"
}
```
