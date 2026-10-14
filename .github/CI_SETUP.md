# Chat Android UI Kit CI

Tracked under [WAAF-3507](https://waafi.atlassian.net/browse/WAAF-3507).

The Android CI workflow runs on pushes and pull requests targeting `dev`,
`main`, `publish/staging`, and `publish/prod`. It cancels superseded development
and PR runs; running publishing pushes are allowed to finish. It runs these checks:

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

The unit-test job also runs the release-note helper's Node tests.

`FindOrCreateChanelByUriTests` fetches tokens and creates channels on the staging
backend. The normal CI run excludes this class using the runner's `notClass`
argument. Run it separately against a configured test backend.

## Firebase App Distribution

After **All Tests** passes, a push to a publishing branch builds and distributes
the demo APK. PR runs and pushes to `dev` or `main` do not distribute builds.

| Branch | Variant | GitHub environment |
| --- | --- | --- |
| `publish/staging` | `stagingRelease` | `firebase-staging` |
| `publish/prod` | `prodRelease` | `firebase-prod` |

Create both GitHub environments under Settings → Environments. Restrict each to
its matching publishing branch. Require PRs and the **All Tests** check for both
branches. Promote from `dev` using **merge commits**, preserving feature history
so release notes can find the original feature PRs. Put fixes into `dev` before
promoting them. A branch creation also emits a push and can start distribution.

Firebase uploads are serialized across both environments. Running uploads are
not cancelled by newer pushes; GitHub concurrency retains only the latest pending
run/job when several are waiting. A skipped build's changes remain in the next
build's notes. An older publishing commit cannot be distributed after a newer
successful build from that same branch, because its baseline would no longer be
an ancestor.

Both variants currently use `com.sceyt.chat.demo`, so installing one replaces the
other. Configure the Firebase app registered for that package. Use separate tester
groups for staging and production; the two environments may use the same Firebase
app or apps in separate Firebase projects.

### GitHub environment configuration

Set these variables in each environment (shared values may be repository variables):

| Variable | Value |
| --- | --- |
| `FIREBASE_APP_ID` | Firebase Android app ID, such as `1:123456789:android:abc123`. |
| `FIREBASE_GROUPS` | Comma-separated Firebase tester group aliases, such as `android-qa`. |
| `FIREBASE_VERSION_CODE_BASE` | Positive integer above every version code already distributed for this package, such as `10000`. Subsequent builds advance above both environments' recorded codes. |
| `FIREBASE_WORKLOAD_IDENTITY_PROVIDER` | Full Google workload identity provider resource name. |
| `FIREBASE_SERVICE_ACCOUNT` | Email of the Google service account used for distribution. |

Set these secrets in each environment:

| Secret | Value |
| --- | --- |
| `FIREBASE_KEYSTORE_BASE64` | Base64 contents of the persistent signing keystore. |
| `FIREBASE_KEYSTORE_PASSWORD` | Keystore password. |
| `FIREBASE_KEY_ALIAS` | Signing key alias. |
| `FIREBASE_KEY_PASSWORD` | Signing key password. |

Use the signing key that signed your existing Firebase builds so testers can
update them without reinstalling. A different certificate requires reinstalling.
CI fails before building when required settings are missing. Local builds keep
their existing signing and version unless CI overrides are supplied.

### Google and Firebase setup

1. Enable App Distribution for the Android app in the Firebase console and create
   the staging/production tester groups.
2. Create a dedicated service account and grant **Firebase App Distribution Admin**
   (`roles/firebaseappdistro.admin`) in the matching Firebase project.
3. Configure a workload identity pool/provider for GitHub's OIDC issuer
   `https://token.actions.githubusercontent.com`. Restrict trust to this repository
   and the appropriate publishing branch/environment. Because jobs use GitHub
   environments, their OIDC subject is
   `repo:sceyt/sceyt-chat-android-uikit:environment:firebase-staging` or
   `repo:sceyt/sceyt-chat-android-uikit:environment:firebase-prod`.
4. Grant the matching federated identity **Workload Identity User**
   (`roles/iam.workloadIdentityUser`) on the service account.
5. Allow the publishing job's `GITHUB_TOKEN` to create `firebase/staging/*` and
   `firebase/prod/*` tags. The workflow requests `contents: write` only for this job;
   repository policies and tag rules must permit those new tags.

Google authentication runs immediately before upload and exports Application
Default Credentials. No Firebase login token or Google private key is stored.

See [Firebase Gradle distribution](https://firebase.google.com/docs/app-distribution/android/distribute-gradle)
and [Google's GitHub authentication action](https://github.com/google-github-actions/auth)
for the provider and service-account setup details.

### Build summary and successful-build markers

Release notes contain the APK's version/name, environment, source SHA, included
feature/fix PRs, and optional QA instructions. The PR template supplies
`Release notes` and `QA notes` sections. Empty release notes fall back to the PR
title; direct commits without a merged PR use their commit subject. Promotion PRs
into `publish/*` and raw merge commits are omitted. Feature PRs are deduplicated.

The successful upload creates an immutable lightweight tag
`firebase/<staging|prod>/<versionCode>` on its exact source commit. The latest
tag for the same flavor is the next build's comparison baseline. Failed tests,
builds, and uploads do not create a tag; failure of the Waafi notification does
not undo a successfully recorded distribution. Keep these tags to preserve
history and version sequencing. If tag recording fails after an upload, the job
fails and the next run can repeat those release notes; raise the version-code
base above the uploaded code before retrying.

The first automated upload has no previous automatic marker: it summarizes the
incoming push range when available and explicitly says that earlier manual
builds were not compared. Creating a new branch provides no previous push SHA,
so the initial build contains only its identity and that explanation.

The same notes appear in Firebase, the GitHub run summary, and the existing Waafi
notification. GitHub also saves the signed APK and notes as a run artifact. The
install link uses Firebase's tester URL, which requires tester access. If the
plugin does not emit that URL, the upload is still recorded and GitHub shows a
warning instead of an install link.

### First live verification

Merge these workflow changes through `dev`, configure the environments, and then
promote to a publishing branch. Verify on GitHub that tests pass before the
upload, the correct flavor/group receives the build, the version code increases,
the install link works, and the recorded tag matches the installed build. Promote
another feature/fix and verify the next notes include only changes since that
flavor's previous successful upload. Firebase upload, OIDC, Linux emulator tests,
and live Waafi delivery require this configured GitHub run to verify.

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
node --test .github/scripts/firebase-release-notes.test.cjs

./gradlew --no-daemon :SceytDemoApp:assembleStagingRelease :SceytDemoApp:assembleProdRelease \
  -PfirebaseVersionCode=10000

./gradlew --no-daemon --stacktrace test :SceytChatUiKit:assembleRelease :SceytChatConnection:assembleRelease

ANDROID_SERIAL=<device-serial> ./gradlew --no-daemon --stacktrace connectedCheck \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.sceyt.chatuikit.persistence.logicimpl.channel.create_channel.FindOrCreateChanelByUriTests
```
