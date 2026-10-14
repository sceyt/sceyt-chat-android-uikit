const {execFileSync} = require('node:child_process');
const {readFileSync, writeFileSync} = require('node:fs');

const git = (...args) => execFileSync('git', args, {encoding: 'utf8'}).trim();

function section(body, name) {
  const lines = (body || '').replace(/<!--[\s\S]*?-->/g, '').split('\n');
  const start = lines.findIndex(line => new RegExp(`^#{1,6}\\s+${name}\\s*$`, 'i').test(line.trim()));
  if (start < 0) return '';
  const end = lines.findIndex((line, index) => index > start && /^#{1,6}\s/.test(line));
  return lines.slice(start + 1, end < 0 ? undefined : end).join('\n').trim();
}

async function prepare({github, context, flavor, versionBase}) {
  if (!['staging', 'prod'].includes(flavor)) throw new Error('Unknown Firebase flavor.');
  const base = Number(versionBase);
  if (!Number.isSafeInteger(base) || base < 1) {
    throw new Error('Set FIREBASE_VERSION_CODE_BASE above all previously distributed version codes.');
  }
  const {data: refs} = await github.rest.git.listMatchingRefs({
    ...context.repo, ref: 'tags/firebase/'
  });
  const markers = refs.flatMap(ref => {
    const match = /^refs\/tags\/firebase\/(staging|prod)\/(\d+)$/.exec(ref.ref);
    return match ? [{flavor: match[1], code: Number(match[2]), sha: ref.object.sha}] : [];
  });
  const versionCode = Math.max(base, ...markers.map(marker => marker.code + 1));
  if (!Number.isSafeInteger(versionCode) || versionCode > 2100000000) {
    throw new Error('Firebase version code exceeds the Android limit.');
  }
  const previous = markers.filter(marker => marker.flavor === flavor)
    .sort((a, b) => b.code - a.code)[0];
  const before = context.payload.before;
  const baseline = previous?.sha || (before && !/^0+$/.test(before) ? before : '');
  if (baseline) {
    try {
      git('merge-base', '--is-ancestor', baseline, context.sha);
    } catch {
      throw new Error('The previous build is not an ancestor. Preserve publishing history with merges.');
    }
  }
  return {versionCode, baseline, firstBuild: !previous};
}

async function getChanges({github, context, baseline}) {
  if (!baseline) return {changes: [], qa: []};
  const commits = git('rev-list', '--reverse', `${baseline}..${context.sha}`).split('\n').filter(Boolean);
  const seen = new Set();
  const changes = [];
  const qa = [];
  for (const sha of commits) {
    const prs = await github.paginate(github.rest.repos.listPullRequestsAssociatedWithCommit, {
      ...context.repo, commit_sha: sha, per_page: 100
    });
    const included = prs.filter(pr => pr.merged_at && !pr.base.ref.startsWith('publish/'));
    for (const pr of included) {
      if (seen.has(pr.number)) continue;
      seen.add(pr.number);
      const notes = section(pr.body, 'Release notes');
      changes.push(notes ? `${notes}\n(PR #${pr.number})` : `• ${pr.title.replace(/\s+/g, ' ').trim()} (#${pr.number})`);
      const instructions = section(pr.body, 'QA notes');
      if (instructions) qa.push(`${instructions}\n(PR #${pr.number})`);
    }
    if (!included.length && !prs.some(pr => pr.merged_at && pr.base.ref.startsWith('publish/'))) {
      const parents = git('show', '-s', '--format=%P', sha).split(' ').filter(Boolean);
      if (parents.length < 2) changes.push(`• ${git('show', '-s', '--format=%s', sha)}`);
    }
  }
  return {changes, qa};
}

function formatNotes({versionName, versionCode, flavor, sha, baseline, firstBuild, changes, qa}) {
  return [
    `Sceyt Chat ${versionName} (${versionCode}) — ${flavor === 'prod' ? 'Production' : 'Staging'}`,
    '',
    ...(firstBuild ? ['First automated Firebase build; earlier manual builds are not compared.', ''] : []),
    baseline ? 'Changes included in this build:' : 'No previous build or push history to compare.',
    ...changes,
    ...(baseline && !changes.length ? ['No new feature or fix changes.'] : []),
    ...(qa.length ? ['', 'QA focus:', ...qa] : []),
    '', `Commit: ${sha}`,
    ...(baseline ? [`Compared from: ${baseline}`] : [])
  ].join('\n');
}

async function generate({github, context, flavor, baseline, firstBuild, versionCode, outputPath}) {
  const metadata = JSON.parse(readFileSync(
    `examples/SceytDemoApp/build/outputs/apk/${flavor}/release/output-metadata.json`, 'utf8'
  ));
  const apk = metadata.elements[0];
  if (apk.versionCode !== Number(versionCode)) throw new Error('APK version does not match the release.');
  const entries = await getChanges({github, context, baseline});
  const notes = formatNotes({
    ...entries, flavor, baseline, firstBuild, sha: context.sha,
    versionCode: apk.versionCode, versionName: apk.versionName
  });
  writeFileSync(outputPath, notes + '\n');
  return notes;
}

async function complete({github, context, core, flavor, versionCode, notesPath, logPath}) {
  await github.rest.git.createRef({
    ...context.repo, ref: `refs/tags/firebase/${flavor}/${versionCode}`, sha: context.sha
  });
  const notes = readFileSync(notesPath, 'utf8').trim();
  const log = readFileSync(logPath, 'utf8');
  const testerUrl = log.match(/https:\/\/appdistribution\.firebase\.google\.com\/testerapps\/[^\s\x1b]+/)?.[0] || '';
  if (!testerUrl) core.warning('The upload succeeded, but no Firebase tester link was found in its output.');
  core.setOutput('release-notes', notes);
  core.setOutput('tester-url', testerUrl);
  await core.summary.addHeading('Firebase build distributed')
    .addCodeBlock(notes, 'text')
    .addRaw(testerUrl ? `\n[Install this build](${testerUrl})\n` : '')
    .write();
}

module.exports = {prepare, getChanges, formatNotes, generate, complete, section};
