# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

This is a pnpm workspace with a Next.js web application in the `web/` directory.

### Root Level Commands
- `pnpm dev` - Start development servers for all workspaces in parallel
- `pnpm build` - Build all workspaces in parallel
- `pnpm test` - Run tests for all workspaces in parallel
- `pnpm lint` - Run linting for all workspaces in parallel
- `pnpm typecheck` - Run TypeScript type checking for all workspaces in parallel

### Web Application Commands (from web/ directory)
- `pnpm dev` or `next dev` - Start Next.js development server (localhost:3000)
- `pnpm build` or `next build` - Build Next.js application
- `pnpm start` or `next start` - Start production server
- `pnpm lint` or `eslint` - Run ESLint

## Architecture

### Project Structure
- **Root**: pnpm workspace configuration with shared TypeScript setup
- **web/**: Next.js 15 application with App Router
  - Uses React 19
  - Tailwind CSS v4 for styling
  - TypeScript with strict configuration
  - ESLint with Next.js preset

### Web Application Architecture
- **Framework**: Next.js 15 with App Router
- **Styling**: Tailwind CSS with CSS variables for theming
- **Fonts**: Geist Sans and Geist Mono via next/font/google
- **Path Aliases**: `@/*` maps to `./src/*`
- **TypeScript**: Strict mode enabled with Next.js plugin

### Key Files
- `web/src/app/layout.tsx`: Root layout with font configuration
- `web/src/app/page.tsx`: Homepage component
- `web/next.config.ts`: Next.js configuration
- `web/tsconfig.json`: TypeScript configuration with path aliases
- `web/eslint.config.mjs`: ESLint configuration using flat config format

## Development Notes

- The project uses pnpm workspaces for monorepo management
- All parallel commands should be run from the root directory
- Web-specific commands can be run from either root or web/ directory
- TypeScript paths are configured for `@/*` imports in the web application