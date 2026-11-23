from contextlib import ExitStack, redirect_stderr, redirect_stdout
from io import StringIO
import sys
import unittest
from unittest.mock import patch

import check_pr_coverage


class ChangedSdkLinesTest(unittest.TestCase):
    def test_rename_counts_single_and_multiple_added_lines(self):
        root = "SceytChatUiKit/src/main/java/example"
        before = f"{root}/Before.kt"
        after = f"{root}/After.kt"
        diff = (
            "@@ -2 +3 @@\n-before\n+after\n"
            "@@ -7 +8,2 @@\n-before\n+first\n+second\n"
            "@@ -13 +14,0 @@\n-removed\n"
        )

        with patch.object(check_pr_coverage, "git", side_effect=[
            "base\n", f"R090\0{before}\0{after}\0", diff
        ]) as git:
            self.assertEqual(
                list(check_pr_coverage.changed_sdk_lines("target")),
                [(after, "example/After.kt", {3, 8, 9})],
            )

        self.assertEqual(git.call_args.args[-2:], (f"base:{before}", f"HEAD:{after}"))


class CoverageThresholdTest(unittest.TestCase):
    def test_passes_at_80_percent_and_fails_below(self):
        source = "example/Example.kt"
        path = f"SceytChatUiKit/src/main/java/{source}"
        executable = range(1, 101)

        for covered, expected in ((80, 0), (79, 1)):
            with self.subTest(covered=covered), ExitStack() as stack:
                output = stack.enter_context(redirect_stdout(StringIO()))
                stack.enter_context(patch.object(sys, "argv", ["check_pr_coverage.py", "base"]))
                stack.enter_context(patch.object(
                    check_pr_coverage, "changed_sdk_lines", return_value=[(path, source, set(executable))]
                ))
                stack.enter_context(patch.object(
                    check_pr_coverage, "read_coverage", return_value={source: {
                        line: line <= covered for line in executable
                    }}
                ))

                self.assertEqual(check_pr_coverage.main(), expected)
                self.assertIn(f"{covered}.00% ({covered}/100, required 80%)", output.getvalue())


class MissingCoverageSourceTest(unittest.TestCase):
    def check_source(self, source_path, contents):
        path = f"SceytChatUiKit/src/main/java/{source_path}"
        output = StringIO()
        with ExitStack() as stack:
            stack.enter_context(patch.object(sys, "argv", ["check_pr_coverage.py", "base"]))
            stack.enter_context(patch.object(
                check_pr_coverage, "changed_sdk_lines", return_value=[(path, source_path, {1})]
            ))
            stack.enter_context(patch.object(
                check_pr_coverage, "read_coverage", return_value={"example/Existing.kt": {3: True}}
            ))
            git = stack.enter_context(patch.object(check_pr_coverage, "git", return_value=contents))
            stack.enter_context(redirect_stdout(output))
            stack.enter_context(redirect_stderr(output))
            result = check_pr_coverage.main()
        return result, output.getvalue(), git

    def test_kotlin_file_with_only_comments_and_directives_passes(self):
        contents = (
            "/* Header /* nested comment */ continues. */\n"
            "package example; // Package notes.\n"
            "import kotlin.collections.*\n"
            "import kotlin.String as Text\n"
        )
        result, output, git = self.check_source("example/PackageNotes.kt", contents)
        self.assertEqual(result, 0)
        self.assertIn("No changed executable SDK lines", output)
        git.assert_called_once_with("show", "HEAD:SceytChatUiKit/src/main/java/example/PackageNotes.kt")

    def test_kotlin_declaration_with_missing_coverage_fails(self):
        contents = "package example\n/* Notes. */\nfun example() = 1\n"
        result, output, _ = self.check_source("example/Example.kt", contents)
        self.assertEqual(result, 1)
        self.assertIn("missing JaCoCo source data", output)

    def test_java_missing_coverage_still_fails(self):
        contents = "package example;\nclass Example {}\n"
        result, output, git = self.check_source("example/Example.java", contents)
        self.assertEqual(result, 1)
        self.assertIn("missing JaCoCo source data", output)
        git.assert_not_called()
