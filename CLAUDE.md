# LambdaCraft Project Notes

## Public Content URL Stability

Public article URLs must be semantic, short, and stable over time.

### URL Principles

- Public URLs should express content meaning, not internal workflow state.
- Do not expose chapter numbers, series numbers, dates, draft state, or other temporary markers in public paths.
- Internal directory layout may help authoring, but must not define the public URL shape by default.

### IR / RAG Routing

- Public namespace should use `/ir-rag/`, not `/ir-rag-course/`.
- Preferred structure:
  - `/ir-rag/`
  - `/ir-rag/{series-slug}/`
  - `/ir-rag/{series-slug}/{article-slug}/`
- Example:
  - `/ir-rag/bm25/from-vsm-to-probability/`

### Slug Rules

- Use semantic slugs such as `bm25` or `from-vsm-to-probability`.
- Do not use internal names such as `series-01-bm25` or `ch01-vsm-to-probability` as public URLs.
- Ordering should live in metadata such as `order`, not in the slug.

### Compatibility Rules

- Once a public URL is published, avoid changing it.
- When restructuring routes, preserve old links through explicit compatibility redirects or mapped legacy routes.

### Rendering Rules

- Reader-facing navigation and lists should use titles and explicit metadata.
- Do not derive public labels or public URLs directly from `entry.id` when that id comes from file paths.
