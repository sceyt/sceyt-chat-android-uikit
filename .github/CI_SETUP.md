# Chat Android UI Kit CI

Tracked under [WAAF-3507](https://waafi.atlassian.net/browse/WAAF-3507).

The Android CI workflow runs on pushes and pull requests targeting `dev` and
`main`. It cancels superseded runs and runs these checks:

- **Unit tests**: the existing JVM test suite (`test`) and release
  AAR builds for `SceytChatUiKit` and `SceytChatConnection`. Uploads test reports
  and both release libraries.
- **Instrumented tests**: the existing connected test suite on an API 35 emulator,
  including benchmark smoke checks in dry-run mode. Uploads device test reports
  and benchmark reports.
- **All Tests**: passes only when both jobs succeed. Use this as the required
  branch-protection check.

Both jobs install Android SDK 37.0 to match the project's `compileSdk`.
CI builds the libraries without publishing Maven releases.

`FindOrCreateChanelByUriTests` fetches tokens and creates channels on the staging
backend. The normal CI run excludes this class using the runner's `notClass`
argument. Run it separately against a configured test backend.

## Waafi configuration

Under repository Settings → Secrets and variables → Actions, configure:

| Setting | Type | Purpose |
| --- | --- | --- |
| `WAAFI_PLATFORM_API_KEY` | Secret | Send messages and add the sender to the configured channel. |
| `WAAFI_CHANNEL_ID` | Variable | Set to `869416152613912576` for this repository. |

The sender posts as `37400000000`. On HTTP 404, it adds that user to the configured
channel as a participant with access to prior history, then retries the message
once. Other HTTP errors and network failures fail the notification job without
retrying.

Success and failure alerts include the repository, PR or branch, commit, actual
passed and failed job lists, and links to the PR and run reports. An empty passed
list shows `None`; an empty failed list is omitted. The notification job and
duplicate `All Tests` summaries are excluded. Unavailable job results are reported
as unavailable. Closed PRs, superseded PR commits, cancelled runs, and fork PRs do
not send alerts. Fork PRs can still run the build and test jobs.

Push both workflow files together. A new GitHub run must verify Linux builds,
emulator execution, repository variables, and live Waafi delivery. Rerunning an
older run uses that run's older workflow definition.

## Local verification

```sh
./gradlew --no-daemon --stacktrace test :SceytChatUiKit:assembleRelease :SceytChatConnection:assembleRelease

ANDROID_SERIAL=<device-serial> ./gradlew --no-daemon --stacktrace connectedCheck \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel.FindOrCreateChanelByUriTests
```
