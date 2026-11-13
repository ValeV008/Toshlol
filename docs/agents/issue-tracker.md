# Issue tracker: Local Markdown

Issues and specs for this repo live as markdown files in `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`
- Implementation issues use `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01`
- Triage state is recorded as a `Status:` line near the top of each issue file
- Comments append to the bottom under a `## Comments` heading

## Parent-spec coverage

Before implementing a ticket, read its parent `spec.md` and add a `## Parent-spec requirements` section linking applicable user stories and decisions. Carry each requirement into an observable acceptance criterion, including effects on other screens. For each critical journey, name the test that asserts the displayed values or state. Resolve the ticket when those criteria and their tests pass.

## When a skill says "publish to the issue tracker"

Create a new file under `.scratch/<feature-slug>/`.

## When a skill says "fetch the relevant ticket"

Read the referenced file path.

## Wayfinding operations

- **Map**: `.scratch/<effort>/map.md`
- **Child ticket**: `.scratch/<effort>/issues/NN-<slug>.md`
- **Blocking**: use a `Blocked by: NN, NN` line near the top
- **Claim**: set `Status: claimed`
- **Resolve**: append an `## Answer`, set `Status: resolved`, and update the map
