Generate a Git commit message based on the provided code changes.

Use this exact format:

<type>: <short summary>

- <change 1>
- <change 2>
- <change 3>

Rules:
- Use Conventional Commits for the first line, such as `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `perf:`, or `docs:`.
- Keep the first line concise and describe the main purpose of the change.
- Use lowercase after the commit type unless a proper noun requires capitalization.
- Do not end the first line with a period.
- Add a blank line after the first line.
- Add bullet points describing the important implementation changes.
- Each bullet must start with `- `.
- Use imperative or concise present-tense wording.
- Focus on behavior and meaningful implementation changes, not individual files.
- Mention test changes when tests were added or updated.
- Mention dependency or SDK version updates when relevant.
- Avoid vague bullets such as "update code", "fix issues", or "make improvements".
- Do not include unnecessary implementation details.
- Do not include explanations before or after the commit message.
- Return only the commit message.
