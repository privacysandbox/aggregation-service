# Private Aggregation Report Goldens

This directory contains a set of golden files (JSON reports) that are valid
aggregatable reports generated by the Chrome client for server
interoperability testing.

The subdirectory latest/ contains files matching the current implementation,
and files matching old versions' implementations will be archived in
version_{x}/ where {x} represents a specific version string.

Each subdirectory includes a private/public key pair which are base64-encoded
and can be used to encrypt/decrypt the data. See
//third_party/boringssl/src/include/openssl/hpke.h for Hybrid Public Key
Encryption (HPKE).

## Keep files up to date

//content/browser/private_aggregation/private_aggregation_report_golden_unittest.cc
contains tests that run on the files for latest/ to ensure that the golden
report format matches the format produced by the current implementation
(ignoring randomness like the exact encrypted string).

## Version history

The history of report versions is listed below, with links to past commits of
documentation.

| Version string | Spec | Payload encryption details | Changes |
| --- | --- | --- | --- |
| "`0.1`" | [link](https://patcg-individual-drafts.github.io/private-aggregation-api/pr-preview/refs/pull/128/merge/index.html) | [link](https://chromium.googlesource.com/chromium/src/+/57a65e032513965829e3ed1c1cd20b39d63d2224/content/browser/aggregation_service/payload_encryption.md) | n/a (initial release).
| "`1.0`" | [link](https://github.com/patcg-individual-drafts/private-aggregation-api) | [link](https://chromium.googlesource.com/chromium/src/+/main/content/browser/aggregation_service/payload_encryption.md) | Adds [filtering IDs](https://github.com/patcg-individual-drafts/private-aggregation-api/blob/main/flexible_filtering.md)


## Golden Report Descriptions

| ID | API                | Num Contributions | Max Contributions | Null? | Debug? | Extreme key? | Filtering ID | Max bytes |
|---:|--------------------|------------------:|------------------:|-------|--------|--------------|-------------:|----------:|
|  1 | Protected Audience |                 1 |                20 |       | Yes    |              |              |           |
|  2 | Protected Audience |                 1 |                20 |       |        |              |              |           |
|  3 | Shared Storage     |                 2 |                20 |       | Yes    |              |              |           |
|  4 | Shared Storage     |                 2 |                20 |       |        |              |              |           |
|  5 | Protected Audience |                 1 |                20 |       | Yes    | Yes          |              |           |
|  6 | Protected Audience |                 1 |                20 |       |        | Yes          |              |           |
|  7 | Shared Storage     |                 1 |                20 | Yes   |        |              |              |           |
|  8 | Protected Audience |                 1 |                20 |       | Yes    |              |            3 |           |
|  9 | Protected Audience |                 1 |                20 |       |        |              |            3 |           |
| 10 | Protected Audience |                 1 |                20 |       | Yes    |              |     2^64 - 1 |         8 |
| 11 | Protected Audience |                 1 |               100 |       | Yes    |              |              |           |
| 12 | Protected Audience |                 1 |               100 |       |        |              |              |           |
| 13 | Protected Audience |                99 |               100 |       | Yes    |              |              |           |
| 14 | Protected Audience |               100 |               100 |       | Yes    |              |              |           |
| 15 | Protected Audience |               100 |               100 |       | Yes    |              |            3 |           |
| 16 | Protected Audience |               100 |               100 |       | Yes    |              |     2^64 - 1 |         8 |
| 17 | Protected Audience |                 1 |               100 |       | Yes    |              |     2^64 - 1 |         8 |
| 18 | Protected Audience |                 1 |              1000 |       | Yes    |              |            3 |           |
| 19 | Protected Audience |                 1 |              1000 |       |        |              |            3 |           |
