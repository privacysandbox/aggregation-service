# cbor

Contains tests and test input files for cbor deserialization & serialization

`report.cbor` is a CBOR serialized report, generated from the contents
of `report.json` using the `json2cbor.rb` utility
from https://github.com/cabo/cbor-diag. `report.json` is a JSON representation
of the reports that Chrome produces.

Spec is defined
here: https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregate-attribution-reports

If needed, the file can be regenerated with this command:

```
 cat report.json | json2cbor.rb > report.cbor
```
