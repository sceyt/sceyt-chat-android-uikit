const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {mkdirSync, mkdtempSync, readFileSync, writeFileSync} = require('node:fs');
const {tmpdir} = require('node:os');
const {join} = require('node:path');
const {test} = require('node:test');
const {prepare, getChanges, formatNotes, generate, complete} = require('./firebase-release-notes.cjs');

const repo = {owner: 'example', repo: 'chat'};
const directory = mkdtempSync(join(tmpdir(), 'firebase-notes-'));
const git = (...args) => execFileSync('git', args, {cwd: directory, encoding: 'utf8'}).trim();
git('init', '-q');
git('config', 'user.name', 'CI test');
git('config', 'user.email', 'ci@example.test');
function commit(subject) {
  git('commit', '--allow-empty', '-qm', subject);
  return git('rev-parse', 'HEAD');
}
const previous = commit('Previous distributed build');
const feature = commit('Add message search');
const fix = commit('Fix reconnect duplicates');
const direct = commit('Improve attachment errors');
const head = commit('Promote dev');
const context = {repo, sha: head, payload: {before: direct}};
const ref = (flavor, code, sha) => ({ref: `refs/tags/firebase/${flavor}/${code}`, object: {sha}});
const markers = [ref('staging', 101, previous), ref('prod', 109, fix)];
const markerClient = refs => ({rest: {git: {listMatchingRefs: async () => ({data: refs})}}});
const inRepo = async action => {
  const cwd = process.cwd();
  process.chdir(directory);
  try { return await action(); } finally { process.chdir(cwd); }
};

test('uses the last successful build of this flavor and a code above both flavors', async () => {
  const result = await inRepo(() => prepare({
    github: markerClient(markers), context, flavor: 'staging', versionBase: '100'
  }));
  assert.deepEqual(result, {versionCode: 110, baseline: previous, firstBuild: false});
});

test('first upload uses incoming push history and honors the configured version floor', async () => {
  const result = await inRepo(() => prepare({
    github: markerClient([]), context, flavor: 'prod', versionBase: '10000'
  }));
  assert.deepEqual(result, {versionCode: 10000, baseline: direct, firstBuild: true});
  const created = await prepare({
    github: markerClient([]), context: {...context, payload: {before: '0'.repeat(40)}},
    flavor: 'prod', versionBase: '10000'
  });
  assert.equal(created.baseline, '');
});

test('refuses reversed history and invalid version configuration', async () => {
  await assert.rejects(inRepo(() => prepare({
    github: markerClient(markers), context: {...context, sha: previous},
    flavor: 'prod', versionBase: '100'
  })), /not an ancestor/);
  await assert.rejects(prepare({
    github: markerClient([]), context, flavor: 'staging', versionBase: ''
  }), /FIREBASE_VERSION_CODE_BASE/);
  await assert.rejects(prepare({
    github: markerClient([]), context, flavor: 'staging', versionBase: '2100000001'
  }), /Android limit/);
});

test('deduplicates feature PRs, extracts QA notes, omits promotion PRs, and includes direct fixes', async () => {
  const search = {
    number: 12, title: 'Add search', merged_at: '2026-10-15', base: {ref: 'dev'},
    body: '## Release notes\n<!-- instruction -->\n• Search channel messages.\n\n## QA notes\n• Test long histories.\n\n## Implementation\nInternal details'
  };
  const reconnect = {number: 13, title: 'Fix reconnect duplicates', merged_at: '2026-10-15', base: {ref: 'dev'}};
  const promotion = {number: 14, title: 'Merge dev', merged_at: '2026-10-15', base: {ref: 'publish/staging'}};
  const associated = {[feature]: [search], [fix]: [search, reconnect], [direct]: [], [head]: [promotion]};
  const github = {
    rest: {repos: {listPullRequestsAssociatedWithCommit: 'associated-prs'}},
    paginate: async (_, {commit_sha}) => associated[commit_sha]
  };
  const entries = await inRepo(() => getChanges({github, context, baseline: previous}));
  assert.deepEqual(entries.changes, [
    '• Search channel messages.\n(PR #12)',
    '• Fix reconnect duplicates (#13)',
    '• Improve attachment errors'
  ]);
  assert.deepEqual(entries.qa, ['• Test long histories.\n(PR #12)']);
  const notes = formatNotes({
    ...entries, versionName: '1.3.3', versionCode: 110, flavor: 'staging', sha: head,
    baseline: previous, firstBuild: false
  });
  assert.match(notes, /Sceyt Chat 1\.3\.3 \(110\) — Staging/);
  assert.match(notes, /QA focus:/);
  assert.ok(!notes.includes('Merge dev') && !notes.includes('Internal details'));
});

test('uses the built APK metadata and rejects a mismatched version before writing notes', async () => {
  const apkDirectory = join(directory, 'examples/SceytDemoApp/build/outputs/apk/staging/release');
  mkdirSync(apkDirectory, {recursive: true});
  writeFileSync(join(apkDirectory, 'output-metadata.json'), JSON.stringify({
    elements: [{versionName: '1.3.3', versionCode: 110}]
  }));
  const outputPath = join(directory, 'generated-notes.txt');
  const args = {
    github: {}, context, flavor: 'staging', baseline: '', firstBuild: true,
    versionCode: '110', outputPath
  };
  await inRepo(() => generate(args));
  assert.match(readFileSync(outputPath, 'utf8'), /Sceyt Chat 1\.3\.3 \(110\) — Staging/);
  await assert.rejects(inRepo(() => generate({...args, versionCode: '111'})), /APK version/);
});

test('records a successful upload separately from notification and exposes its tester link', async () => {
  const notesPath = join(directory, 'notes.txt');
  const logPath = join(directory, 'upload.log');
  writeFileSync(notesPath, 'QA build\n');
  const testerUrl = 'https://appdistribution.firebase.google.com/testerapps/1:123:android:abc/releases/example';
  writeFileSync(logPath, `Share this release with testers who have access: ${testerUrl}\n`);
  const outputs = {};
  const calls = [];
  const summary = {
    addHeading() { return this; }, addCodeBlock() { return this; }, addRaw() { return this; },
    async write() { calls.push('summary'); }
  };
  const core = {summary, setOutput: (key, value) => { outputs[key] = value; }, warning: assert.fail};
  await complete({
    github: {rest: {git: {createRef: async args => { calls.push(args); }}}},
    context, core, flavor: 'prod', versionCode: 110, notesPath, logPath
  });
  assert.deepEqual(calls[0], {...repo, ref: 'refs/tags/firebase/prod/110', sha: head});
  assert.equal(outputs['tester-url'], testerUrl);
  assert.equal(outputs['release-notes'], readFileSync(notesPath, 'utf8').trim());
});
