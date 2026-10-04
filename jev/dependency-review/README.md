# dependency-review

A small local CLI experiment for evaluating dependency updates with Spring AI and Jev.

The goal is deliberately narrow:

1. read a real local Git diff,
2. detect one dependency version update,
3. collect bounded source/test context from the repository,
4. use Spring AI to turn that context into grounded evidence,
5. use Jev (`Noul`, `Score`, `Choice`) to evaluate that evidence,
6. print the result and timings in the terminal.

There is no GitHub Action, web UI, merge policy or auto-merge in this starter.

## Requirements

- Java 21+
- Gradle 8.x (this starter intentionally does not include the Gradle wrapper)
- Git
- `GEMINI_API_KEY`
- `TYPESAFE_API_KEY`
- `GITHUB_TOKEN` or `GH_TOKEN` for private GitHub repositories (public PRs do not require it)

The project currently targets:

- Spring Boot `4.1.1`
- Spring AI `2.0.1`
- `spring-ai-starter-typesafe` `0.2.0`

If the TypeSafe starter version has moved by the time you use the project, update `springAiTypeSafeVersion` in `build.gradle`.

## Build

```bash
gradle clean bootJar
```

## Run

Run it against a local repository containing a dependency update on the current branch:

```bash
export GEMINI_API_KEY=...
# Optional: override the default Gemini model (gemini-3.8-flash)
export GEMINI_MODEL=gemini-3.8-flash
export TYPESAFE_API_KEY=...

java -jar build/libs/dependency-review-0.1.0-SNAPSHOT.jar \
  analyze \
  --repo /path/to/my-service \
  --base main
```

The default head ref is `HEAD`.

### Dry run

Before calling either model, inspect exactly what the tool detected and what source it collected:

```bash
java -jar build/libs/dependency-review-0.1.0-SNAPSHOT.jar \
  analyze \
  --repo /path/to/my-service \
  --base main \
  --dry-run
```

`--dry-run` is useful when tuning repository scanning heuristics.

For `*-bom` updates, the scanner also collects dependency declarations and source/test imports from the BOM's parent group. This shows which managed artifacts are used locally, but it cannot establish the upstream changes between BOM versions; for that, provide release notes or a resolved dependency report as additional input.

### Analyze a GitHub PR directly

Pass a GitHub PR URL to analyze it without cloning or checking out the target repository yourself. The CLI loads PR metadata, creates a temporary shallow checkout of the PR head, compares it to the PR base revision, and removes that checkout when the review completes. The PR description, including Dependabot release notes, is included as upstream evidence.

```bash
java -jar build/libs/dependency-review-0.1.0-SNAPSHOT.jar \
  analyze \
  --github-pr https://github.com/asterisk-java/asterisk-java/pull/786
```

This supports explicit Maven `group:artifact:version` declarations and standard Maven `<dependency>` blocks, so the command above detects the Guava update in that Dependabot PR automatically.

For private repositories, set a token with read access to Pull requests or Contents. The Git installation must also have credentials for cloning the private repository:

```bash
export GITHUB_TOKEN=...
```

When no `--github-pr` URL is supplied, the CLI analyzes the local Git range and tries to locate the PR associated with `HEAD` only to obtain release notes:

```bash
java -jar build/libs/dependency-review-0.1.0-SNAPSHOT.jar \
  analyze \
  --repo /path/to/my-service \
  --base main
```

If GitHub cannot be reached or no PR is associated with `HEAD`, the local review continues without GitHub release notes. A local file remains available as an offline override; it takes precedence over GitHub retrieval.

```bash
java -jar build/libs/dependency-review-0.1.0-SNAPSHOT.jar \
  analyze \
  --repo /path/to/my-service \
  --base main \
  --release-notes-file /path/to/release-notes.md
```

### Manual dependency override

Automatic detection supports explicit `group:artifact:version` changes and ordinary Maven `<dependency>` blocks. For version catalogs, BOMs, Maven properties or unusual build files, provide the update explicitly.

```bash
java -jar build/libs/dependency-review-0.1.0-SNAPSHOT.jar \
  analyze \
  --repo /path/to/my-service \
  --base main \
  --dependency org.example:http-client \
  --from 2.4.1 \
  --to 3.0.0
```

## Example output

The numeric values below are illustrative. Real values come from Jev.

```text
🔎 Dependency Review
════════════════════════════════════════════════════════════
┌─────────────────────────┬───────┬───────┐
│ Dependency              │ From  │ To    │
├─────────────────────────┼───────┼───────┤
│ org.example:http-client │ 2.4.1 │ 3.0.0 │
└─────────────────────────┴───────┴───────┘

📚 Repository evidence
┌──────────────────┬───────────────────────────────────────────────────┬─────────────────────────────────────────────┐
│ Area             │ File                                              │ Finding                                     │
├──────────────────┼───────────────────────────────────────────────────┼─────────────────────────────────────────────┤
│ 🏭 Production    │ src/main/java/example/PaymentGateway.java         │ Dependency is used for outbound payments.   │
│ ⚙️  Configuration │ src/main/java/example/HttpClientConfiguration.java │ Application configures the client explicitly. │
│ 🧪 Tests         │ src/test/java/example/PaymentGatewayTest.java     │ Successful requests are covered.             │
└──────────────────┴───────────────────────────────────────────────────┴─────────────────────────────────────────────┘

❓ Unknowns
┌──────────────────────────────────────────────────────┐
│ Observation                                          │
├──────────────────────────────────────────────────────┤
│ No evidence for retry behavior tests was found.      │
└──────────────────────────────────────────────────────┘

📊 Jev evaluation
┌────────────────────────┬──────────┬────────────┬─────────┐
│ Assessment             │ Score    │ Confidence │ Details │
├────────────────────────┼──────────┼────────────┼─────────┤
│ Migration required     │ 0.27     │ —          │         │
│ Runtime behavior change│ 0.78     │ —          │         │
│ Usage impact           │ 3.10 / 4 │ 0.91       │ ...     │
│ Relevant test coverage │ 1.40 / 4 │ 0.88       │ ...     │
│ Primary affected area  │ RUNTIME  │ 0.86       │         │
└────────────────────────┴──────────┴────────────┴─────────┘

⏱️  Timing
┌──────────────────────────┬─────────┐
│ Stage                    │ Duration│
├──────────────────────────┼─────────┤
│ Git + context collection │ 0.032 s │
│ Spring AI investigation  │ 1.312 s │
│ Jev evaluation           │ 0.781 s │
│ Total                    │ 2.125 s │
└──────────────────────────┴─────────┘
```

## Important limitations

This is a starter, not a production dependency scanner.

- It retrieves a GitHub PR description, but does not independently discover vendor changelogs outside that PR.
- It does not resolve transitive dependencies.
- Automatic update detection handles one explicit dependency coordinate per run.
- Source discovery is heuristic and intentionally bounded.
- Spring AI is instructed to reason only from local repository evidence.
- AI evidence is verified against exact source substrings before Jev sees it.
- Jev returns structured judgments, but this starter intentionally does not decide whether a PR should be merged.

Good next steps are release-note retrieval, Gradle/Maven model parsing, smarter symbol-level usage discovery, multiple update support, and only then a policy layer or CI integration.
