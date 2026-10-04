# Design

The starter intentionally separates three concerns.

## 1. Deterministic repository facts

Git and local file scanning determine the diff, dependency coordinate, versions and candidate source snippets. No AI is asked to infer facts that the repository can provide directly.

## 2. Spring AI investigation

Spring AI turns the bounded source snippets into `DependencyEvidence`. It must cite exact source substrings. `EvidenceVerifier` drops evidence that cannot be grounded back to the collected source.

This first version does **not** fetch release notes or make claims about upstream library changes.

## 3. Jev structured judgment

Jev evaluates independent questions over the same evidence:

- migration required (`Noul`)
- possible runtime behavior impact (`Noul`)
- usage impact (`Score`)
- relevant test coverage (`Score`)
- primary affected area (`Choice`)

The starter does not turn those signals into `SAFE`, `BLOCK` or `AUTO_MERGE`. Add policy only after you have tested the evaluation on real dependency updates.
