---
name: testmate-post-processing
description: Convert `// TestMate-<hash>` comments in test method bodies to `@TestMate(name = "TestMate-<hash>")` annotations on those methods
license: Apache-2.0
metadata:
  author: folio-org
  version: "1.1.0"
---

# TestMate Post-Processing

| Property        | Value                                                       |
|-----------------|-------------------------------------------------------------|
| Name            | testmate-post-processing                                    |
| Version         | 1.1.0                                                       |
| Trigger         | `// TestMate-<hash>` comments in test method bodies         |
| Action          | Convert to `@TestMate(name = "TestMate-<hash>")` annotation |
| Import          | `import org.folio.dataexp.TestMate;`                        |
| No-op condition | Zero `// TestMate-` comments found in file                  |

## Role

You are an automated Java code transformer. Your sole task is mechanical: move hash identifiers from comments to annotations. You do not refactor, improve, or interpret test logic.

## Input

One or more Java test files (JUnit 5) that may contain `// TestMate-<hash>` comments inside method bodies. Process each file independently.

## Output

Each file with all `// TestMate-<hash>` comments converted to `@TestMate` annotations — or the file returned unchanged if no comments exist.

## Context

- Java 21, Spring Boot 3, JUnit 5, Lombok.
- Format: Google Java Format (enforced by Spotless).
- Required import: `import org.folio.dataexp.TestMate;`

## Step-by-Step Workflow

Follow these steps **in order**. Do not skip or reorder.

### Step 1 — Pre-check (gate)

Scan the file for lines matching the regex `^\s*// TestMate-[0-9a-f]+\s*$`.

- **If zero matches** → return the file **completely unchanged**. Output only: `"No changes needed — no TestMate comments found in the file."` Stop here.
- **If matches exist** → proceed to Step 2.

### Step 2 — Collect mappings

For each match, record:
- The **exact hash** (e.g., `53d0001901840b961064d3e70adf40be`).
- The **enclosing method** (the method whose body contains that comment).

### Step 3 — Apply transformations

For each recorded mapping:

1. **Add annotation**: Insert `@TestMate(name = "TestMate-<hash>")` on the method, positioned:
   - After `@Test` or `@ParameterizedTest` or `@RepeatedTest`.
   - Before `@DisplayName`, `@SneakyThrows`, or the method signature (whichever comes first).
2. **Remove comment**: Delete the `// TestMate-<hash>` line from the method body.
3. **Handle conflicts**: If a `@TestMate(name = "...")` annotation already exists on that method with a **different** hash, replace it with the hash from the comment.
4. **Preserve existing**: If a method has a `@TestMate` annotation but **no** comment in the body, leave it untouched.

### Step 4 — Ensure import

If `import org.folio.dataexp.TestMate;` is not present, add it in the correct sorted position (after static imports, alphabetically among regular imports).

### Step 5 — Final verification

Perform these checks. If any fails, go back and fix before outputting:

- [ ] Zero lines matching `// TestMate-` remain in the file.
- [ ] Every hash that was in a comment now exists in a `@TestMate(name = "TestMate-<hash>")` annotation — character-for-character identical.
- [ ] `import org.folio.dataexp.TestMate;` is present.
- [ ] No test logic, assertions, method names, or other annotations were modified.

## Hard Rules

| # | Rule |
|---|------|
| 1 | One comment → one annotation on the same method. |
| 2 | NEVER generate, invent, truncate, or modify a hash. Copy verbatim. |
| 3 | Do NOT merge methods with different hashes into a `@ParameterizedTest`. |
| 4 | Do NOT change test logic, assertions, structure, method names, or `@DisplayName` values. |
| 5 | Do NOT add or remove annotations other than `@TestMate`. |

## Transformation Examples

**Example 1 — Simple `@Test`:**

Before:
```java
@Test
void shouldReturnEmpty() {
  // TestMate-53d0001901840b961064d3e70adf40be
  // Given
  ...
}
```

After:
```java
@Test
@TestMate(name = "TestMate-53d0001901840b961064d3e70adf40be")
void shouldReturnEmpty() {
  // Given
  ...
}
```

**Example 2 — `@Test` with `@SneakyThrows`:**

Before:
```java
@Test
@SneakyThrows
void shouldCloseWriter() {
  // TestMate-d336844ac6915d74cfdbd10e27afbf4b
  // Given
  ...
}
```

After:
```java
@Test
@TestMate(name = "TestMate-d336844ac6915d74cfdbd10e27afbf4b")
@SneakyThrows
void shouldCloseWriter() {
  // Given
  ...
}
```

**Example 3 — `@ParameterizedTest` with source annotation:**

Before:
```java
@ParameterizedTest
@NullAndEmptySource
void shouldUseAllRecords(String query) {
  // TestMate-08ee9a7528ddf18f0a322d5db9a6b61a
  // Given
  ...
}
```

After:
```java
@ParameterizedTest
@TestMate(name = "TestMate-08ee9a7528ddf18f0a322d5db9a6b61a")
@NullAndEmptySource
void shouldUseAllRecords(String query) {
  // Given
  ...
}
```

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| File has zero `// TestMate-` comments | Return unchanged, no edits |
| Method already has correct `@TestMate` + no body comment | Leave as-is |
| Method has `@TestMate` with hash A + body comment with hash B | Replace annotation hash with B, remove comment |
| Multiple methods each with their own comment | Transform each independently |
| Comment appears outside a method (e.g., class-level) | Ignore — only process comments inside method bodies |

## Common Mistakes to Avoid

- Inventing or truncating a hash instead of copying it character-for-character.
- Merging methods with different hashes into one parameterized test.
- Forgetting to add the import statement.
- Leaving `// TestMate-` comments in the body after adding the annotation.
- Changing test logic, assertions, or method structure.
- Processing a file with no TestMate comments (should be a no-op).
- Placing `@TestMate` right after `@Test` instead of before it.
