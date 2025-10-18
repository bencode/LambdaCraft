# CLAUDE.md

LambdaCraft - Clojure notebook platform for mathematics and computational thinking using [Clerk](https://clerk.vision/).

## Development

```bash
# Development server with REPL
clojure -M:dev

# Build and deploy
clojure -X:build
./apps/web/deploy.sh
```

REPL commands:
```clojure
(u/start!)              ; Start server at http://localhost:7777
(clerk/halt!)           ; Stop server
(clerk/show! "pages/home.clj")
```

## Architecture

### Directory Structure
```
apps/web/
├── pages/              # Public notebooks
│   ├── sicm/          # SICM learning notebooks (sicm-X-Y.clj)
│   └── ...
├── docs/              # Documentation notebooks
├── books/sicm/        # SICM reader notebooks (load from books/sicm/*.md)
└── resources/         # Static assets

books/sicm/            # SICM markdown source files
```

### Content Types
- **pages/**: Public content, use domain names for directories (e.g., `sicm/` not `learning/`)
- **docs/**: Technical documentation
- **books/sicm/**: Reader notebooks that render markdown files from `books/sicm/*.md`

## SICM Content

### Learning Notebooks (`pages/sicm/`)
Interactive learning notes with Emmy code.

Naming: `sicm-X-Y.clj` (chapter-section), namespace: `pages.sicm.sicm-X-Y`

Structure:
```clojure
^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.sicm.sicm-1-4
  "SICM 第 1.4 节：计算作用量"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer [literal-function up D Gamma compose simplify]]
            [emmy.mechanics.lagrange :as lag]))

;; # Title
;; ## Concepts
;; ## Implementation
;; ## Verification
;; ## Summary
```

Key Emmy functions:
- `lag/Lagrangian-action`, `lag/make-path`, `lag/find-path`
- `literal-function`, `D`, `Gamma`, `compose`
- `definite-integral`, `minimize`, `multidimensional-minimize`

### Book Reader (`apps/web/books/sicm/`)
Reader notebooks load markdown from `books/sicm/*.md` using `user.dir` path resolution.

Adding chapters:
1. Add markdown to `books/sicm/X.Y-title.md`
2. Create reader notebook `apps/web/books/sicm/chX.clj`
3. Update `apps/web/books/sicm/contents.clj`

## Common Issues

### Markdown LaTeX Formatting

**1. `$$` 后缺少空行**
```bash
awk '{if ($0 == "$$" && NR > 1) print ""; print}' file.md > temp.md && mv temp.md file.md
```

**2. LaTeX 命令双反斜杠错误**
应使用单反斜杠。常见：`\\dots`, `\\mathcal`, `\\vec`, `\\quad`, `\\dot`, `x\_`

```bash
sed -i '' \
  -e 's/\\\\dots/\\dots/g' \
  -e 's/\\\\mathcal/\\mathcal/g' \
  -e 's/\\\\vec/\\vec/g' \
  -e 's/\\\\quad/\\quad/g' \
  -e 's/\\\\dot/\\dot/g' \
  -e 's/x\\_/x_/g' \
  file.md
```
