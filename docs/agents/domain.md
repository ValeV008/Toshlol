# Domain Docs

Engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- `GLOSSARY.md` at the repo root, if it exists.
- `docs/adr/`: read ADRs that touch the area being changed.

If these files don't exist, proceed silently. Don't flag their absence or suggest creating them upfront. The `/domain-modeling` skill creates them lazily when domain terms or decisions are resolved.

## File structure

This is a single-context repo:

/
├── GLOSSARY.md
├── docs/adr/
└── src/

## Use the glossary's vocabulary

When naming a domain concept in an issue, refactor proposal, or test, use the term defined in `GLOSSARY.md`. If the concept is not defined yet, note the gap for `/domain-modeling`.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
