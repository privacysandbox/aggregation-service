# README.md

This directory contains multiple unencrypted input aggregatable reports and their corresponding
aggregated outputs.

## Data Sets

The following are attribution-reporting type aggregatable report input data sets:

1. `input_set_attribution_1`
2. `input_set_attribution_2`
3. `input_set_attribution_3`
4. `input_set_attribution_4`

The following are fledge type aggregatable report input data sets:

1. `input_set_fledge_1`
2. `input_set_fledge_2`

Each of the above directory can contain one or many of these files:

1. `batch.avro` &mdash; input avro file containing aggregatable reports made of unencrypted payload
   and shared-infos.
2. `domain.avro` &mdash; input domain avro file.
3. `text_batch_data.txt` &mdash; text file containing facts (bucket and values tuples) used in
   batch.avro
4. `domain.txt` &mdash; input file used to create domain.avro.
5. `flags.txt` &mdash; configuration used during aggregation for given data set like - no_noise,
   skip_domain, no_overlap_keys, total_keys_in_domain etc.

`expected_output_set_i` corresponds to `input_set_<API_TYPE>_i` ie. `expected_output_set_1`is the
aggregation output for both `input_set_fledge_1` and `input_set_attribution_1` and so on. The reason
behind this is that both the fledge and attribution-reporting aggregatable reports were generated
using the same input `text_batch_data.txt`.

## Data Generation

### Attribution Reporting Data

`input_set_attribution_i` data sets were generated using `SampleDataGenerator`.

### Fledge Data

`input_set_fledge_1` was generated with `FULL` domain overlap using `SimulationRunner` -

```sh
bazel run java/com/google/aggregate/simulation:SimulationRunner -- \
  --aggregatable_report_file_path $DIR1/batch.avro \
  --num_reports 10 \
  --no_encryption \
  --distribution FILE \
  --distribution_file_path $DIR1/text_batch_data.txt \
  --aggregatable_report_provider FLEDGE \
  --num_unique_privacy_budget_keys 3 \
  --domain_overlap FULL \
  --generate_output_domain \
  --output_domain_path $DIR1/domain.avro
```

where `DIR1=<project-root>/aggregate-service/worker/testing/data/library/input_set_fledge_1`

`input_set_fledge_2` was generated with `NONE` domain overlap using `SimulationRunner`:

```sh
bazel run java/com/google/aggregate/simulation:SimulationRunner -- \
  --aggregatable_report_file_path $DIR2/batch.avro \
  --num_reports 10 \
  --no_encryption \
  --distribution FILE \
  --distribution_file_path $DIR2/text_batch_data.txt \
  --aggregatable_report_provider FLEDGE \
  --num_unique_privacy_budget_keys 3 \
  --domain_overlap NONE \
  --generate_output_domain \
  --output_domain_path $DIR2/domain.avro
```

where `DIR2=<project-root>/aggregate-service/worker/testing/data/library/input_set_fledge_2`
