# syntax=docker/dockerfile:1.7

FROM node:24-slim AS builder
WORKDIR /app

RUN corepack enable && corepack prepare pnpm@9.15.4 --activate

COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/web/package.json ./apps/web/
COPY apps/wx/package.json ./apps/wx/

RUN pnpm install --frozen-lockfile --filter "web..."

COPY apps/web ./apps/web

RUN pnpm --filter web build

FROM caddy:2-alpine
COPY docker/Caddyfile /etc/caddy/Caddyfile
COPY --from=builder /app/apps/web/dist /srv
EXPOSE 80
