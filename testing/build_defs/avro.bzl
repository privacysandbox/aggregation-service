# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

def _generate_avro_impl(ctx):
    out = ctx.actions.declare_file("%s.avro" % ctx.label.name)
    domain = None
    if ctx.attr.generate_output_domain:
        domain = ctx.actions.declare_file("%s_domain.avro" % ctx.label.name)

    args = ctx.actions.args()
    args.add("--aggregatable_report_file_path", out)
    args.add("--num_reports", ctx.attr.num_reports)
    args.add("--distribution", "FILE")
    args.add("--distribution_file_path", ctx.file.human_readable_reports)
    args.add("--asymmetric_key_file_path", ctx.file.key)
    args.add("--scheduled_report_time", ctx.attr.scheduled_report_time)
    args.add("--domain_overlap", ctx.attr.domain_overlap)

    if ctx.attr.generate_output_domain:
        args.add("--generate_output_domain")
        args.add("--output_domain_path", domain)
        args.add("--output_domain_size", ctx.attr.output_domain_size)

    outputs_list = [out, domain] if ctx.attr.generate_output_domain else [out]

    ctx.actions.run(
        executable = ctx.executable._simulation,
        arguments = [args],
        inputs = [ctx.file.human_readable_reports, ctx.file.key],
        outputs = outputs_list,
    )

    return [
        DefaultInfo(
            files = depset(outputs_list),
            runfiles = ctx.runfiles(files = outputs_list),
        ),
        ReportsInfo(reports_path = out),
    ] + ([OutputDomainInfo(output_domain_path = domain)] if ctx.attr.generate_output_domain else [])

OutputDomainInfo = provider(
    doc = "Provides output domain",
    fields = {"output_domain_path": "Path to where the output domain is stored."},
)

ReportsInfo = provider(
    doc = "Provides AggregatableReports",
    fields = {"reports_path": "Path to where the output domain is stored."},
)

generate_avro = rule(
    implementation = _generate_avro_impl,
    attrs = {
        "domain_overlap": attr.string(
            doc = "Type of overlap domain keys should have with report keys.",
            default = "FULL",
        ),
        "generate_output_domain": attr.bool(
            doc = "If true, indicates that output domain of results should be generated.",
        ),
        "human_readable_reports": attr.label(
            doc = "Text file containing human-readable reports.",
            allow_single_file = True,
            mandatory = True,
        ),
        "key": attr.label(
            doc = "Key file for encryption the reports",
            allow_single_file = True,
            mandatory = True,
        ),
        # TODO: remove the attr below, the tool should figure out on its own how
        # many reports are there.
        "num_reports": attr.int(
            doc = "Number of reports that should be generated.",
            mandatory = True,
        ),
        "output_domain_size": attr.int(
            doc = "Number of buckets that should be present in the output domain.",
        ),
        "scheduled_report_time": attr.string(
            doc = "Scheduled time for the generated reports",
            default = "1970-01-01T00:00:00Z",
        ),
        "_simulation": attr.label(
            default = Label("//java/com/google/aggregate/simulation:SimulationRunner"),
            executable = True,
            cfg = "target",
        ),
    },
)

def _shard_avro_impl(ctx):
    if (ctx.attr.reports_path and ctx.attr.domain_path):
        print("Either reports path or domain path must be provided.")
        fail
    out = ctx.actions.declare_directory("%s_shards" % ctx.label.name)
    args = ctx.actions.args()
    args.add("--num_shards", ctx.attr.num_shards)
    args.add("--output_dir", out.path)

    if ctx.attr.reports_path:
        input_path = ctx.attr.reports_path[ReportsInfo].reports_path
    else:
        input_path = ctx.attr.domain_path[OutputDomainInfo].output_domain_path
        args.add("--domain")

    args.add("--input", input_path)
    ctx.actions.run(
        executable = ctx.executable._shard_tool,
        arguments = [args],
        inputs = [input_path],
        outputs = [out],
    )
    return [DefaultInfo(
        files = depset([out]),
        runfiles = ctx.runfiles(files = [out]),
    )]

shard_avro = rule(
    implementation = _shard_avro_impl,
    attrs = {
        "domain_path": attr.label(
            doc = "Path to domain reports file that need to be sharded.",
            mandatory = False,
        ),
        "num_shards": attr.int(
            doc = "Number of shards to be generated",
            mandatory = True,
        ),
        "reports_path": attr.label(
            doc = "Path to avro reports file that need to be sharded.",
            mandatory = False,
        ),
        "_shard_tool": attr.label(
            default = Label("//java/com/google/aggregate/tools/shard:AvroShard"),
            executable = True,
            cfg = "target",
        ),
    },
)
