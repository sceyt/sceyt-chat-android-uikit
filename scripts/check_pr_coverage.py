"""Fail when a commit covers fewer than 80% of its changed SDK lines."""

import argparse
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "build/reports/jacoco/sdk/report.xml"
SOURCE_ROOTS = tuple(
    f"{module}/src/main/{language}/"
    for module in ("SceytChatUiKit", "SceytChatConnection")
    for language in ("java", "kotlin")
)
MIN_COVERAGE = 80
HUNK = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", re.MULTILINE)
KOTLIN_IDENTIFIER = r"(?:[^\W\d]\w*|`[^`\r\n]+`)"
KOTLIN_QUALIFIED_NAME = rf"{KOTLIN_IDENTIFIER}(?:[ \t]*\.[ \t]*{KOTLIN_IDENTIFIER})*"
KOTLIN_HEADER_TOKEN = re.compile(
    r"\s+|//[^\r\n]*|/\*|"
    rf"package[ \t]+{KOTLIN_QUALIFIED_NAME}[ \t]*;?|"
    rf"import[ \t]+{KOTLIN_QUALIFIED_NAME}"
    rf"(?:[ \t]*\.[ \t]*\*|[ \t]+as[ \t]+{KOTLIN_IDENTIFIER})?[ \t]*;?"
)
KOTLIN_COMMENT_DELIMITER = re.compile(r"/\*|\*/")


def git(*args):
    return subprocess.check_output(
        ["git", "--literal-pathspecs", *args],
        cwd=ROOT,
        encoding="utf-8",
        errors="surrogateescape",
    )


def has_only_kotlin_header(source):
    """Recognize files containing only whitespace, comments, and directives."""
    source = source.lstrip("\ufeff")
    offset = 0
    while offset < len(source):
        token = KOTLIN_HEADER_TOKEN.match(source, offset)
        if token is None:
            return False
        offset = token.end()
        if token[0] != "/*":
            continue

        depth = 1
        for delimiter in KOTLIN_COMMENT_DELIMITER.finditer(source, offset):
            depth += 1 if delimiter[0] == "/*" else -1
            if depth == 0:
                offset = delimiter.end()
                break
        else:
            return False
    return True


def changed_sdk_lines(base):
    base = git("merge-base", "--", base, "HEAD").strip()
    entries = iter(git(
        "diff", "--name-status", "-z", "--find-renames", "--diff-filter=AMRT",
        base, "HEAD", "--", *SOURCE_ROOTS,
    ).split("\0")[:-1])
    for status in entries:
        old_path = next(entries)
        renamed = status.startswith("R")
        path = next(entries) if renamed else old_path
        source_root = next((root for root in SOURCE_ROOTS if path.startswith(root)), None)
        if source_root is None or Path(path).suffix not in (".kt", ".java"):
            continue

        revisions = (
            (f"{base}:{old_path}", f"HEAD:{path}")
            if renamed else (base, "HEAD", "--", path)
        )
        diff = git("diff", "--no-color", "--no-ext-diff", "--no-textconv", "--unified=0", *revisions)
        lines = set()
        for start, length in HUNK.findall(diff):
            first_line = int(start)
            lines.update(range(first_line, first_line + int(length or "1")))
        if lines:
            yield path, path[len(source_root):], lines


def read_coverage():
    report = ET.parse(REPORT).getroot()
    line_counter = report.find("counter[@type='LINE']")
    if line_counter is None or int(line_counter.attrib["covered"]) == 0:
        raise ValueError(f"{REPORT} has no SDK line coverage data")
    return {
        f"{package.attrib['name']}/{source.attrib['name']}".lstrip("/"): {
            int(line.attrib["nr"]): int(line.attrib["ci"]) > 0
            for line in source.findall("line")
            if int(line.attrib["mi"]) + int(line.attrib["ci"]) > 0
        }
        for package in report.findall("package")
        for source in package.findall("sourcefile")
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("base", help="Target branch or commit to compare with HEAD")
    args = parser.parse_args()

    try:
        changes = list(changed_sdk_lines(args.base))
        if not changes:
            print("No added or modified SDK source lines.")
            return 0

        coverage = read_coverage()
        total = covered = 0
        for path, source, changed in changes:
            if source not in coverage:
                if path.endswith(".kt") and has_only_kotlin_header(git("show", f"HEAD:{path}")):
                    continue
                raise ValueError(f"missing JaCoCo source data for {path!r}")

            executable = changed & coverage[source].keys()
            uncovered = sorted(line for line in executable if not coverage[source][line])
            total += len(executable)
            covered += len(executable) - len(uncovered)
            if uncovered:
                print(f"{path!r}: uncovered lines {', '.join(map(str, uncovered))}")
    except (OSError, subprocess.CalledProcessError, ET.ParseError, ValueError) as error:
        print(f"SDK coverage check failed: {error}", file=sys.stderr)
        return 1

    if total == 0:
        print("No changed executable SDK lines.")
        return 0

    print(f"Changed SDK line coverage: {covered / total:.2%} ({covered}/{total}, required {MIN_COVERAGE}%)")
    return int(covered * 100 < total * MIN_COVERAGE)


if __name__ == "__main__":
    sys.exit(main())
