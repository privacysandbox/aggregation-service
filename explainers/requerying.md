# Aggregation Service: Requerying

_This document is an explainer for the upcoming Requerying feature in Aggregation Service._

## Intended audience

This explainer is primarily aimed at developers who use Aggregation Service. Since Aggregation
Service supports aggregatable reports produced by the
[Attribution Reporting API](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md)
(ARA) and
[Private Aggregation API](https://github.com/patcg-individual-drafts/private-aggregation-api#reports),
this explainer is applicable to developers who use either of those APIs.

## Introduction

Aggregation Service currently implements the
["No duplicates" rule](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#no-duplicates-rule):

-   No aggregatable report can appear more than once within a job request.
-   No Shared ID can appear in more than one job request or contribute to more than one summary
    report.

This explainer proposes removing the second restriction. In other words, this proposal allows a
report to be reused in multiple job requests (aka "queries") - we are calling this capability
"Requerying." Note that the first restriction will still be enforced (i.e. within a query, a report
can only appear once).

In order to adhere to the privacy considerations of the APIs that use Aggregation Service, we
enforce that 1) each job request must specify how much
[Aggregatable Report Accounting Budget](https://privacysandbox.google.com/private-advertising/aggregation-service/availability-and-use-cases#key-terms-and-concepts)
to consume, and 2) for each
[Shared ID](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#disjoint-batches),
the sum of all of its budget consumptions must be less than or equal to a system-level max budget.
This gives adtechs the flexibility to choose how many times a report can be requeried, while meeting
the existing differential privacy principles.

For example, suppose the max Aggregatable Report Accounting Budget is epsilon = 64. An adtech may
decide that each Shared ID needs to be queried 4 times - in that case, the adtech would use an
Aggregatable Report Accounting Budget of epsilon = 16 for each query. Alternatively, an adtech could
decide to use epsilon = 47 for one query, then use the remaining 17 epsilon for a second query.

### Terminology

**"Query"**: In this explainer, we will use the term "query" to refer to a single time that a report
is processed in an Aggregation Service job request. For example, if a report has been processed as
part of 3 different jobs, we would say it "has been queried 3 times" or "has had 3 queries".

**"Budget"**: For sake of brevity, the rest of this explainer will refer to the
[Aggregatable Report Accounting Budget](https://privacysandbox.google.com/private-advertising/aggregation-service/availability-and-use-cases#key-terms-and-concepts)
simply as "budget."

This should not be confused with the
[Contribution Budget](https://developers.google.com/privacy-sandbox/private-advertising/attribution-reporting/contribution-budget):

-   Aggregatable Report Accounting Budget (or simply "budget" in this doc): used by Aggregation
    Service, this limits the number of times each report can be processed in an aggregation query.
    **This is what we will mainly be referring to in this explainer.**
-   Contribution Budget (a.k.a. the L1 max): used by Attribution Reporting and Private Aggregation
    APIs, this limits how much each individual user can contribute in a privacy unit. **This
    explainer won't refer to this much - if it does, the explainer will call out the full name
    "Contribution Budget."**

# Motivating Use Cases

-   **"Initial estimate" query**: Requerying lets adtechs get initial "rough" summary of the data
    quickly, while still allowing them to reprocess later for "richer" comprehensive data once all
    reports in a Shared ID have been received
    ([issue #732](https://github.com/WICG/attribution-reporting-api/issues/732)). This could be
    useful in various scenarios such as handling delayed reports or quick spam detection.
-   **Daily/weekly/monthly processing**: Requerying enables adtechs to reuse reports to process data
    across different windows of time. For example, adtechs can process data for each day and each
    week of a month, as well as an end-of-month query, by reusing the same reports.
-   **Rolling window queries**: Similar to the previous use case, Requerying enables adtechs to
    reuse reports when querying rolling windows. For example, if the adtech wants to query
    "conversions over the last 7 days" every single day, then each query will reuse 6 days of
    reports from the previous query.

# Privacy Considerations

## Laplace mechanism

Aggregation Service already uses the
[Laplace mechanism](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#added-noise-to-summary-reports)
for adding noise. Adtechs control the privacy budget consumption as well as the noise level with a
parameter called epsilon (the current API uses the "debug_privacy_epsilon" job parameter). Today, we
allow up to epsilon = 64 worth of budget consumption for each Shared ID, and re-querying will not
change that. The difference with Requerying is that adtechs will be able to split the epsilon budget
across multiple queries.

Example

-   Query 1: epsilon = 16
-   Query 2: epsilon = 32
-   Query 3: epsilon = 16
-   Total: epsilon = 16 + 32 + 16 = 64

From a differential privacy perspective, running multiple queries with different levels of epsilon
has the exact same level of privacy as running a single query with the sum of all those epsilons. No
matter how the adtech splits their budget across queries, the total privacy level will be no worse
than total epsilon (see Theorem 1 in this
[paper](https://link.springer.com/chapter/10.1007/11681878_14)).

## Impact on noise

Adtechs can split up the budget however they want. However, be aware that making epsilon smaller for
each query will also result in larger noise for that query.

In other words, although we won't impose hard limits on the number of times a report can be queried,
in practice, querying a report a large number of times will result in impractically large noise. For
example, if an Adtech wants to query a report 100 times, they might allocate 0.64 epsilon for each
query, which would result in a much larger standard deviation.

**If you need support for a large number of queries (> 40), please file a Github issue to describe
your use case. Depending on the demand, we may explore enhancements to the Requerying feature.**

# Proposal

We propose the following changes to the
[CreateJobRequest](https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md) JSON
request:

```jsonc
{
  // ... existing fields

  // Specifies parameters for noising and budget consumption
  // If this field is unset AND the debug_privacy_epsilon job_parameter is unset, default
  // to laplace_dp_params.job_epsilon = 10.
  "privacy_params": {

    // Noising with the Laplace mechanism.
    "laplace_dp_params": {
      // The epsilon for this job. This determines noise levels and budget
      // consumption for just this job. Must be at most 64.
      //
      // Floating point numbers are allowed, but must be limited to 2 decimal places,
      // for performance reasons. If more than 2 decimal places are used, the request
      // will fail.
      //
      // If laplace_dp_params is set but job_epsilon is unset, the request will fail.
      "job_epsilon": <double>
    },
  }

  // Mark "debug_privacy_epsilon" as deprecated in job_parameters
}
```

A few notable things to call out:

-   For backwards compatibility, if neither the new privacy_params field nor the legacy
    debug_privacy_epsilon job_parameter are set, Aggregation Service will default to using Laplace
    DP with epsilon = 10. This matches the [existing behavior of the API](#backwards-compatibility).
-   For performance reasons, we enforce job_epsilon cannot have more than 2 decimal places.
-   The debug_privacy_epsilon job parameter will be deprecated. Going forward, adtechs should use
    the privacy_params field to configure epsilon instead. See the
    [Backwards compatibility](#backwards-compatibility) section for details.

## Example CreateJob requests

Example of Laplace, splitting budget into 4 jobs equally:

```jsonc
{
    // ... other fields
    "privacy_params": {
        "laplace_dp_params": {
            "job_epsilon": 16 // 64 / 4 = 16
        }
    }
}
```

Example of Laplace, choosing to use all budget in one job (i.e. not using Requerying)

```jsonc
{
    // ... other fields
    "privacy_params": {
        "laplace_dp_params": {
            "job_epsilon": 64
        }
    }
}
```

Example of using the default options: Laplace with 10 epsilon. Since there is 54 epsilon left over,
adtechs can either choose to process the reports again in a future job, or choose not to process the
reports again if they don't want to use Requerying.

```jsonc
{
    // ... "privacy_params" is unset.
}
```

Example of invalid requests:

```jsonc
// No job_epsilon is specified
{
  // ... other fields
  "privacy_params": {
    "laplace_dp_params": {}
  }
}

// Too many decimal places in job_epsilon. No more than 2 decimal places are allowed.
{
  // ... other fields
  "privacy_params": {
    "laplace_dp_params": {
      "job_epsilon": 10.12345,
    }
  }
}
```

## Budget Exhaustion

Today, Aggregation Service keeps track of budget consumption for each Shared ID. If a job causes one
of its Shared IDs to go over budget, that job fails with a BUDGET_EXHAUSTED return code. No budget
is consumed for any of the Shared IDs in that failed job.

Once Requerying is available, keep in mind that even though a Shared ID might still have budget
available right now, if the job would cause the Shared ID to go over budget, the job will fail. For
example, if a Shared ID has consumed 48 epsilon so far, but the job wants to consume 20 epsilon,
that would put the Shared ID at 48 + 20 = 68 > 64. Aggregation Service will fail that job with
BUDGET_EXHAUSTED, and the Shared ID's total budget consumption would remain at 48.

## Backwards compatibility

The existing debug_privacy_epsilon job_parameter will be deprecated. Adtechs are encouraged to use
the new privacy_params field instead. However, to maintain backwards compatibility, we will continue
to support the debug_privacy_epsilon job parameter for the time being.

See this table for the interaction between the privacy_params field and the debug_privacy_epsilon
job parameter:

|                      | debug_privacy_epsilon set                                 | debug_privacy_epsilon unset            |
| -------------------- | --------------------------------------------------------- | -------------------------------------- |
| privacy_params set   | Request will fail INVALID_JOB return code                 | use the privacy_params                 |
| privacy_params unset | treat as Laplace with job_epsilon = debug_privacy_epsilon | treat as Laplace with job_epsilon = 10 |

## Relation with Filtering IDs

When an adtech creates an aggregatable report, they can tag each bucket with a
[Filtering ID](https://github.com/patcg-individual-drafts/private-aggregation-api/blob/main/flexible_filtering.md).
Later, when submitting a job to Aggregation Service, the adtech can specify one or more filtering
IDs in the job request - buckets that are tagged with the desired filtering IDs will be kept in the
summary report, while buckets that aren't tagged will be excluded from the summary report.

Filtering IDs are part of the Shared ID - conceptually, each aggregatable report can be split into
multiple partitions (one for each filtering ID that is tagged), and each partition corresponds to a
different Shared ID. Because of this, filtering IDs effectively allow adtechs to reuse aggregatable
reports in more than one job.

This might sound similar to Requerying - however, each distinct partition in an aggregatable report
can still only be processed once. In other words, each contribution in a report can only contribute
to a single summary report. In contrast, Requerying is useful when an adtech wants to include a
single contribution in multiple summary reports.

To illustrate the difference, here is an example use case that could be solved with either Filtering
IDs or Requerying. Suppose an adtech wants to reuse a contribution in 3 different jobs, each
representing a different time window (daily, weekly, and monthly).

-   With Requerying, the adtech records 1 contribution via ARA or Private Aggregation API. Then in
    Aggregation Service, the adtech can submit 3 jobs that include the report. The adtech can choose
    to divide the total Aggregatable Report Accounting Budget across those jobs, so that each job
    uses epsilon = 64 / 3 = 21.33 to generate noise.
-   With Filtering IDs, the adtech records 3 contributions: "xxx_daily", "xxx_weekly", and
    "xxx_monthly" - each one also needs to be tagged with its corresponding filtering ID. The
    Contribution Budget would be divided across these contributions, 2^16 / 3 = 21,845.33. Then in
    Aggregation Service, the adtech can submit 3 jobs that include the report, with each job
    specifying the desired filtering ID (daily, weekly, monthly). Each job would use epsilon = 64 to
    generate noise, since each filtering ID gets its own Shared ID.

In terms of noise levels, both options would result in the same signal-to-noise ratio. However, for
this use case, we recommend Requerying:

-   Requerying is more flexible: with Filtering IDs, adtechs need to decide on the Filtering IDs
    upfront - if the adtech wants to add or change one of the time windows later, existing reports
    cannot be used. On the other hand, Requerying allows adtechs to change the time windows even
    after the aggregatable reports have already been generated.
-   Requerying is more intuitive: Filtering IDs requires the adtech to create 3 copies of the same
    contribution, which may be less efficient and error prone. Requerying allows adtechs to just
    record the contribution once and split budget during aggregation to generate multiple summary
    reports.

Filtering IDs are more suitable when the goal is to split metrics into further subcategories, such
as different campaign IDs. This enables more efficient processing in Aggregation Service by
selecting only the desired Filtering IDs. In contrast, to achieve the same thing with Requerying,
adtechs would have to 1) embed the campaign IDs in their contribution keys, then post-process the
summary report to include only the desired campaign IDs.

In other use cases, it's possible to use both Filtering IDs **and** Requerying at the same time. For
example, an adtech may still want to split up their contributions by campaign (using Filtering IDs),
but still process each contribution multiple times to compute daily, weekly, monthly summary reports
(using Requerying).

# Potential future changes

For Adtechs who need support for a large number of queries (> 40), please file a Github issue to
describe your use case. Depending on Adtech demand, we may explore enhancements to the Requerying
feature.

In the future, we may make a change to allow adtechs to configure their own maximum epsilon. The
adtech-configured maximums would still be required to be less than epsilon = 64.

We may also consider changing the max epsilon to improve privacy. Any changes to the max budgets
would be accompanied with advance notice to adtechs with migration instructions, as needed.
