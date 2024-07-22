# cbor

Contains tests and test input files for cbor deserialization & serialization

Chrome Json reports are picked from
[here](https://source.chromium.org/chromium/chromium/src/+/main:content/test/data/attribution_reporting/aggregatable_report_goldens/latest/).

`report_x_cleartext_payloads.json` has base64 encoded cleartest payload that is passed through
base64 decoder to get cbor, for example:

```sh
  base64 -d report_1_cleartext_payloads.json > report1.cbor
```

Alternative, to ignore garbage while decoding use the following command:

```sh
  base64 -di report_1_cleartext_payloads.json > report1.cbor
```

CborPayloadSerdesTest reads the cbor files from reportx.cbor files, deserializes the cbor payload
and compares it with manually constructed test payload.
