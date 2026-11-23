# Chat Android UI Kit CI

Tracked under [WAAF-3507](https://waafi.atlassian.net/browse/WAAF-3507).

On pull requests, **Kotlin Formatting** runs Spotless once. After it passes, the JVM and emulator jobs run in parallel. If the preflight fails, the test suites are skipped and **Unit tests** reports the failed prerequisite. On pushes, the test jobs run in parallel.

The Android CI workflow runs on pushes and pull requests targeting `dev` and
`main`. It cancels superseded runs and runs these checks:

- **Unit tests**: the existing JVM test suite (`test`) and release
  AAR builds for `SceytChatUiKit` and `SceytChatConnection`, Android lint, and ABI checks. Pull requests also run changed-line coverage. The job uploads the reports and both release libraries.
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

## Slack configuration

The GitHub workflow posts to `#sceyt-ci-android` using the `SLACK_WEBHOOK_URL_CI_ANDROID` organization Actions secret. Give this repository access to the secret in the organization's Actions settings.

Alerts name the failed step, include the underlying CI checks, and link to the run and pull request. The workflow skips notifications for forks, cancelled runs, closed pull requests, and superseded pull request commits.

## Local verification

```sh
./gradlew --no-daemon --stacktrace test :SceytChatUiKit:assembleRelease :SceytChatConnection:assembleRelease

ANDROID_SERIAL=<device-serial> ./gradlew --no-daemon --stacktrace connectedCheck \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel.FindOrCreateChanelByUriTests
```
