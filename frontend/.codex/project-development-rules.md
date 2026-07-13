# Project Development Rules

These rules apply to frontend OpenSpec implementation work in this repository.

1. Before implementing specs, analyze the change and split implementation into chapters from simpler, lower-risk work to larger, riskier work.
2. Implementation edits must stay within the `frontend` workspace. Do not change backend files or project databases unless the user explicitly requests it.
3. If frontend work cannot continue without backend changes, stop and explain what backend change is needed and why. Proceed only after user approval.
4. After implementation, verify the whole project and the newly implemented part in particular.
5. After work, report changed files and explain the concrete changes in simple language.
6. After each implementation stage, tell the user whether they need to manually rebuild/restart the project and what to manually verify.
7. During larger spec implementation, periodically check text fonts/language, UI correctness, and visible layout quality.
