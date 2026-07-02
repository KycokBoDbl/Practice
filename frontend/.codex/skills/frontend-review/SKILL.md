---
name: frontend-review
description: Analyze the frontend codebase and produce an architectural review without modifying the code.
license: MIT
compatibility: React + TypeScript + Vite
metadata:
  author: Ilya
  version: "1.0"
---

# Frontend Review

## Purpose

Review the frontend codebase before merging changes.
The goal is to identify architectural, maintainability and code quality issues.
The skill must never modify the code.

## Scope

Analyze the `frontend/src` directory.

Ignore:
- node_modules
- dist
- generated files

## Review checklist

### 1. Project structure

Check whether files are located in appropriate directories.

Examples:

- reusable UI → components/
- route logic → pages/
- API interaction → api/
- shared types → types/

---

### 2. Component responsibility

Verify that every React component has a single responsibility.

Examples of violations:

- navigation component loads business data;
- layout component contains search logic;
- one component performs rendering, filtering and API requests simultaneously.

---

### 3. State management

Review usage of React state.

Check:

- unnecessary useState;
- duplicated state;
- state that could be derived;
- excessive prop drilling.

---

### 4. Data flow

Check where data is loaded.

Preferred approach:

API
↓

Page

↓

Components

Avoid:

API

↓

Header

↓

Other pages

---

### 5. React quality

Look for:

- large components;
- duplicated JSX;
- deeply nested JSX;
- missing reusable components.

---

### 6. TypeScript

Check:

- usage of any;
- duplicated interfaces;
- missing prop typing;
- unnecessary type assertions.

---

### 7. Maintainability

Estimate whether the code is easy to extend.

Highlight:

- large files;
- duplicated logic;
- long functions;
- magic numbers;
- hardcoded strings.

---

## Output

Produce a report containing:

### Summary

Overall assessment.

### High priority

Critical architectural problems.

### Medium priority

Issues affecting maintainability.

### Low priority

Minor improvements.

### Recommendations

Suggest a prioritized refactoring plan.

## Rules

- Never modify source code.
- Never generate patches.
- Do not rewrite components.
- Explain why each issue is a problem.
- Suggest improvements without implementing them.