## Example use of DiffRunner

Use case: Check if there is any diff after a new code change.

1. Build the runner on HEAD

```shell
bazel build java/com/google/aggregate/tools/diff:DiffRunner
```

2. Create the test input if you don't have one

```shell
bazel build worker/testing/data:diff_1k
```

3. Create the golden as a pre-change truth

```shell
bazel-bin/java/com/google/aggregate/tools/diff/DiffRunner \
--test_input bazel-bin/worker/testing/data/diff_1k.avro \
--test_key worker/testing/data/encryption_key.pb \
--test_golden /tmp/diff_1k_golden.avro \
--update_golden
```

Example output:

```shell
...
New golden file is written to: /tmp/diff_1k_golden.avro
```

`update_golden` will make the **DiffRunner** create a new golden file with the
test inputs.

4. Do some changes on the Aggregation worker code and recompile **DiffRunner**
   to capture worker changes

```shell
bazel build java/com/google/aggregate/tools/diff:DiffRunner
```

5. Run diff between the pre-change truth and post-change result

```shell
bazel-bin/java/com/google/aggregate/tools/diff/DiffRunner \
--test_input bazel-bin/worker/testing/data/diff_1k.avro \
--test_key worker/testing/data/encryption_key.pb \
--test_golden /tmp/diff_1k_golden.avro
```

Example output:

```shell
Found diffs between left(test) and right(golden).
not equal: only on left={f=AggregatedFact{key=f, count=41, value=195}}: only on right={C=AggregatedFact{key=C, count=7, value=496}}
```

Note: The **DiffRunner** is
using `java/com/google/aggregate/adtech/worker/testing/LocalAggregationWorkerRunner.java`
as worker runner. If you want to run diff checks with different worker flags,
you may have to make a local change on that class.