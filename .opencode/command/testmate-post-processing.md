---
description: Convert `// TestMate-<hash>` comments in test method bodies to `@TestMate(name = "TestMate-<hash>")` annotations
agent: build
---

Process Java test files matching $ARGUMENTS (space-separated file paths, or leave blank for all `src/test/**/*.java`).

## Workflow

### Step 1 — Pre-check (gate)

Scan the file for lines matching `^\s*// TestMate-[0-9a-f]+\s*$`.

- **If zero matches** → return the file completely unchanged. Output only: `"No changes needed — no TestMate comments found in the file."` Stop.
- **If matches exist** → proceed to Step 2.

### Step 2 — Collect mappings

For each match, record:
- The exact hash.
- The enclosing method.

### Step 3 — Apply transformations

For each mapping:

1. **Add annotation**: Insert `@TestMate(name = "TestMate-<hash>")` after `@Test`/`@ParameterizedTest`/`@RepeatedTest`, before `@DisplayName`/`@SneakyThrows`/method signature.
2. **Remove comment**: Delete the `// TestMate-<hash>` line.
3. **Handle conflicts**: If a `@TestMate` with a different hash already exists, replace the hash. If same hash + no comment, leave untouched.

### Step 4 — Ensure import

If `import org.folio.dataexp.TestMate;` is missing, add it in sorted position (after static imports, alphabetically among regular imports).

### Step 5 — Verify

- [ ] Zero `// TestMate-` lines remain.
- [ ] Every hash from comments now in a `@TestMate(name = "TestMate-<hash>")` annotation.
- [ ] `import org.folio.dataexp.TestMate;` is present.
- [ ] No test logic, assertions, method names, or other annotations changed.

## Hard Rules

1. One comment → one annotation on the same method.
2. NEVER generate, truncate, or modify a hash. Copy verbatim.
3. Do NOT merge methods into a `@ParameterizedTest`.
4. Do NOT change test logic, assertions, or `@DisplayName`.
5. Do NOT add/remove annotations other than `@TestMate`.

## Examples

**`@Test` → after `@Test`, before method:**
```java
@Test
@TestMate(name = "TestMate-53d0001901840b961064d3e70adf40be")
void shouldReturnEmpty() {
  // Given ...
}
```

**`@ParameterizedTest` with source annotation:**
```java
@ParameterizedTest
@TestMate(name = "TestMate-08ee9a7528ddf18f0a322d5db9a6b61a")
@NullAndEmptySource
void shouldUseAllRecords(String query) {
  // Given ...
}
```
