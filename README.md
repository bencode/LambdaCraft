# LambdaCraft

**() => study(math) => practice(code)**

A Clojure notebook-based platform for studying math, practicing code, and exploring computational thinking using [Clerk](https://clerk.vision/).

## 🚀 Quick Start

### Prerequisites
- [Clojure CLI](https://clojure.org/guides/getting_started) (1.11+)
- [Node.js](https://nodejs.org/) (for font dependencies)

### Development Setup

1. **Install font dependencies:**
   ```bash
   pnpm install
   # or npm install
   ```

2. **Start development server:**
   ```bash
   clojure -M:dev
   ```

   Server will be available at: http://localhost:7777

## 🔧 Available Commands

### Development
```bash
# Start dev server with auto-reload
clojure -M:run

# Start REPL for interactive development
clojure -M:dev
```

### Building
```bash
# Build static site for GitHub Pages
clojure -X:build

# Output will be in public/ directory
```

### REPL Commands
```clojure
;; Server management
(start!)           ; Start server
(clerk/halt!)        ; Stop server
```

## 📋 Development Workflow

1. **Create notebook:** Add `.clj` file in `notebooks/`
2. **Live preview:** Start dev server with `clojure -M:run`
3. **Interactive dev:** Use REPL with `(clerk/show! "path/to/notebook")`
4. **Build:** Generate static site with `clojure -X:build`
5. **Deploy:** Push to GitHub for automatic Pages deployment

## 📚 Resources

- [Clerk Documentation](https://book.clerk.vision/)
