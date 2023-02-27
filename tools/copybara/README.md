# Sync Golden Aggregatable Reports from Chrome to Aggregate Service

The golden aggregatable reports are used by chrome browser tests to validate the aggregatable report
generation. They are located in
[chromium source](https://source.chromium.org/chromium/chromium/src/+/main:content/test/data/attribution_reporting/aggregatable_report_goldens/).
These reports can be used for server-side validation as well. The reports are updated when a newer
version of aggregatable reports are released.

To sync the golden aggregatable report directory, do the following

-   Setup Copybara. Follow instructions in go/copybara

-   You can do a dry-run

```sh
copybara copy.bara.sky chrome_to_gob --force --dry-run --git-destination-path $(mktemp -d)
```

-   From the `tools/copybara` directory, run

```sh
copybara copy.bara.sky chrome_to_gob --force
```

This will create a Gerrit CL and you should see the link to the CL from the terminal output. Go to
the CL and approve the change.
