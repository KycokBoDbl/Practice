---
name: frontend-review-v2
description: Analyze React + TypeScript frontend codebases and produce a structured read-only architectural review. Use when Codex is asked to review frontend/src before merging or refactoring, assess component boundaries, hooks, data flow, React patterns, maintainability, TypeScript quality, signs of AI-generated code, and whether follow-up OpenSpec changes should be proposed.
---

# Frontend Review V2

## Purpose

Review the frontend codebase without modifying application source code.

Focus on architecture, maintainability, React correctness, component boundaries, data flow, and signs that code may have been generated or assembled by AI without enough integration discipline.

## Scope

Analyze `frontend/src`.

Ignore:
- `node_modules`
- `dist`
- generated files
- unrelated backend or infrastructure files unless they directly affect frontend behavior

## Rules

- Never modify application source code.
- Never generate patches.
- Never rewrite components in the answer.
- Read enough code to support concrete findings with file and line references.
- Prefer repo-local evidence over generic advice.
- Treat AI-generated-code signals as risk indicators, not proof of authorship.
- If running checks, use read-only verification commands when possible.
- If a command writes build artifacts and fails because of sandbox permissions, report that as a verification limitation.

## Review Workflow

1. Inspect project shape.
   - List files under `src`.
   - Identify pages, components, layouts, hooks, API modules, stores, types, styles, and router setup.
   - Note missing expected layers only when the current codebase size makes them useful.

2. Trace data flow.
   - Identify where API calls happen.
   - Check whether pages load data and pass it down to components.
   - Flag shared layout/navigation components that fetch business data.
   - Flag duplicated fetches, local cache drift, and route screens that load broad collections to find one record.

3. Review component boundaries.
   - Check whether components have one primary responsibility.
   - Flag components that combine fetching, transformation, filtering, validation, routing, and large rendering blocks.
   - Check whether reusable UI lives in `components/`, route-specific orchestration in `pages/`, shared types in `types/`, and API access in `api/`.

4. Review hooks and state.
   - Flag state that can be derived during render.
   - Flag duplicated state synced from props, URL params, or other state through effects.
   - Flag effects used for pure derivation.
   - Flag missing loading/error/empty states.
   - Flag unstable dependencies, stale closures, excessive prop drilling, and unnecessary context/global state.

5. Review React patterns.
   - Check list keys, conditional rendering, controlled inputs, route params, navigation side effects, accessibility semantics, heading hierarchy, and event cleanup.
   - Check whether custom hooks would clarify repeated behavior.
   - Flag deeply nested JSX, duplicated card/list/form markup, and large components.

6. Review TypeScript.
   - Flag `any`, broad `string` where a union is known, unnecessary assertions, unsafe non-null assertions, duplicated interfaces, and untyped props.
   - Check API response types and whether UI-specific types are mixed with transport types.

7. Review maintainability.
   - Flag large files, long functions, magic numbers, hardcoded business rules, duplicated formatting, inconsistent style, and scattered domain constants.
   - Check whether behavior is testable as pure functions or isolated hooks.

8. Check AI-generated-code risk signals.
   - Look for inconsistent formatting across nearby files.
   - Look for duplicated logic with small naming differences.
   - Look for overbroad components that solve several concerns at once.
   - Look for unused CSS/classes, dead imports, generic placeholder pages, inconsistent language, mojibake text, emoji-as-icons in production UI, suspicious comments, or mismatched heading levels.
   - Look for code that passes visually but misses loading/error states, accessibility, cancellation, or boundary cases.
   - Report these as maintainability risks and explain the concrete consequence.

9. Verify when useful.
   - Run `npm run lint` if available.
   - Run `npm run build` only if appropriate for review and safe in the environment.
   - Do not treat unavailable tools as source findings; list them under verification limits.

## Finding Quality Bar

Each finding should include:
- priority
- file and line reference
- what is wrong
- why it matters
- suggested direction, without implementing it

Prefer fewer, stronger findings over a long list of style preferences.

Do not flag an issue solely because a different architecture is possible. Flag it when the current structure creates duplication, incorrect behavior, unclear ownership, poor scalability, or high change risk.

## Priority Guide

### High Priority

Use for:
- user-visible bugs
- broken loading/error states
- data flow that causes incorrect or stale UI
- architectural coupling that affects multiple routes
- React hook misuse caught by lint or likely to cause render loops/stale data
- unsafe API assumptions likely to break real usage

### Medium Priority

Use for:
- large components with mixed responsibilities
- duplicated fetch/filter/format logic
- weak TypeScript models
- missing reusable hooks/components where duplication already exists
- hardcoded domain rules that are likely to change
- accessibility or semantic issues that are real but not blocking core flow

### Low Priority

Use for:
- formatting inconsistency
- minor naming issues
- small semantic HTML improvements
- low-risk cleanup
- style organization issues

## Report Format

Produce the report in this structure:

### Summary

Give a short overall assessment of architecture, risk level, and readiness.

### High Priority

List critical findings ordered by severity.

For each finding:
- `Location:` file and line
- `Issue:` concise problem statement
- `Why it matters:` concrete consequence
- `Direction:` suggested improvement without code

### Medium Priority

Use the same finding format.

### Low Priority

Use the same finding format, or concise bullets for minor issues.

### AI-Generated Code Signals

List only concrete signals found in the codebase. Avoid claiming the code was generated by AI. Phrase as:

`This has AI-generated-code risk characteristics because ...`

If no meaningful signals are found, say so.

### Verification

List checks run and outcomes.

Include:
- lint/build/test command results
- commands that could not run
- environment limitations

### Recommendations

Provide a prioritized refactoring plan.

Keep it practical:
1. bug fixes and correctness
2. data flow/API boundaries
3. component and hook extraction
4. typing and constants
5. cleanup

### OpenSpec Follow-Up

If the review identifies changes that are large, cross-cutting, or require product decisions, recommend creating an OpenSpec change.

Suggest OpenSpec when:
- multiple pages or shared data flow need redesign
- API contracts need new endpoints or response shapes
- booking/search/filter behavior needs clarified requirements
- refactoring has user-visible behavior risk

Do not create OpenSpec artifacts during the review unless the user explicitly asks.

Format:
- `OpenSpec recommended:` yes/no
- `Reason:`
- `Suggested change title:`
- `Suggested scope:`

## Output Constraints

- Keep the report direct and evidence-based.
- Do not include full rewritten code.
- Do not include speculative issues without local evidence.
- Do not end with vague offers; provide concrete next steps.
