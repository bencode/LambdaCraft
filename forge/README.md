# forge/

Workshop for forging programming techniques. Each subdirectory is an independent, runnable experiment that picks the stack best suited to express a specific technique (frontend, backend, or full-stack).

## Conventions

- Each subdirectory is its own pnpm workspace member with its own `package.json`.
- Runtime is free per demo: Node, Bun, Deno; Python (for LLM work) lives outside the pnpm workspace.
- Use kebab-case names that hint at the technique being demonstrated.
- Every demo ships a short `README.md`: what it is, why it's worth looking at, how to run it.

## Relationship to the main site

The main site (`apps/web`) lists forge projects as a demo index and can embed them via iframe or link out to standalone deploys.
