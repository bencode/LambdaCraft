# LambdaCraft Blog

**() => study(math) => practice(code)**

An interactive blog powered by [Clerk](https://clerk.vision/) for exploring mathematics and programming through computational notebooks.

## Getting Started

### Prerequisites
- Java 11+
- Clojure CLI tools

### Development

Start the development server:

```bash
cd web
./dev.sh
```

Or manually:

```bash
cd web
clj -M:clerk
```

This will start Clerk on http://localhost:7777 and automatically open your browser.

### Project Structure

```
web/
├── deps.edn              # Clojure dependencies
├── clerk.edn             # Clerk configuration
├── src/blog/core.clj     # Core blog functions
├── notebooks/            # Blog posts as Clerk notebooks
│   ├── welcome.clj       # Welcome page
│   └── math-exploration.clj  # Math examples
└── resources/            # Static assets
```

### Writing Blog Posts

Create new notebook files in `notebooks/` directory. Each notebook is a Clojure file with Clerk metadata and markdown.

Example structure:
```clojure
^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.my-post
  (:require [nextjournal.clerk :as clerk]))

# Post Title

Your content here...

```clojure
;; Interactive code examples
(+ 1 2 3)
```

### Building Static Site

```bash
cd web
clj -M:build
```

## Features

- Interactive code evaluation
- Mathematical notation support
- Data visualizations with Plotly
- Live reload during development
- Static site generation for deployment