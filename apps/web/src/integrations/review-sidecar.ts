// Astro integration: dev-only middleware that persists review notes to
// sidecar `.review.md` files living next to the source MDX.
//
// Only runs in `astro dev`; production builds never load this middleware.

import type { AstroIntegration } from 'astro'
import type { IncomingMessage, ServerResponse } from 'node:http'
import path from 'node:path'
import {
  appendNoteToSidecar,
  newNoteId,
  sidecarPathFor,
  slugFromPath,
  type ReviewNote,
  type ReviewPosition,
} from '../lib/review-sidecar-format'

type ReviewRequestBody = {
  file: string
  position: ReviewPosition
  content: string
}

async function readJson<T>(req: IncomingMessage): Promise<T> {
  const chunks: Buffer[] = []
  for await (const chunk of req) {
    chunks.push(typeof chunk === 'string' ? Buffer.from(chunk) : (chunk as Buffer))
  }
  const raw = Buffer.concat(chunks).toString('utf8')
  return JSON.parse(raw) as T
}

function sendJson(res: ServerResponse, status: number, body: unknown): void {
  res.statusCode = status
  res.setHeader('content-type', 'application/json')
  res.end(JSON.stringify(body))
}

function isPathSafe(target: string, root: string): boolean {
  const rel = path.relative(root, target)
  return rel !== '' && !rel.startsWith('..') && !path.isAbsolute(rel)
}

export function reviewSidecar(): AstroIntegration {
  return {
    name: 'review-sidecar',
    hooks: {
      'astro:server:setup': ({ server }) => {
        // project root = where astro dev was invoked (apps/web)
        const projectRoot = server.config.root

        server.middlewares.use('/__review', async (req, res) => {
          if (req.method !== 'POST') {
            res.statusCode = 405
            res.end('method not allowed')
            return
          }

          try {
            const body = await readJson<ReviewRequestBody>(req)
            if (!body.file || !body.content?.trim() || !body.position) {
              sendJson(res, 400, { error: 'missing file/position/content' })
              return
            }

            // Guard against path traversal; only accept MDX/MD under project root.
            // body.file may be relative (entry.filePath from Astro) or absolute.
            const abs = path.isAbsolute(body.file)
              ? body.file
              : path.resolve(projectRoot, body.file)
            if (!isPathSafe(abs, projectRoot)) {
              sendJson(res, 400, { error: 'file outside project root' })
              return
            }

            const sidecar = sidecarPathFor(abs)
            const note: ReviewNote = {
              id: newNoteId(),
              createdAt: new Date().toISOString(),
              position: body.position,
              content: body.content,
            }
            await appendNoteToSidecar(sidecar, slugFromPath(abs), note)
            sendJson(res, 200, { ok: true, id: note.id, sidecar })
          } catch (err) {
            console.error('[review-sidecar] write failed', err)
            sendJson(res, 500, { error: String(err) })
          }
        })
      },
    },
  }
}
