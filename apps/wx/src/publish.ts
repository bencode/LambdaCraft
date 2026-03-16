import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { config } from 'dotenv'
import { getAccessToken, uploadMaterial, createDraft } from './api.js'
import { title, content, author, digest } from '../articles/hello.js'
import type { Article } from './types.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')

config({ path: resolve(root, '.env') })

const appId = process.env.APP_ID
const appSecret = process.env.APP_SECRET

if (!appId || !appSecret) {
  console.error('Missing APP_ID or APP_SECRET in .env')
  process.exit(1)
}

async function main() {
  console.log('Getting access token...')
  const token = await getAccessToken(appId, appSecret)
  console.log('Access token obtained.')

  const coverPath = resolve(root, 'assets/cover.png')
  console.log(`Uploading cover image: ${coverPath}`)
  const { media_id: thumbMediaId } = await uploadMaterial(token, coverPath)
  console.log(`Cover uploaded, media_id: ${thumbMediaId}`)

  const draft: Article = {
    title,
    content,
    thumb_media_id: thumbMediaId,
    author,
    digest,
  }

  console.log('Creating draft...')
  const { media_id: draftMediaId } = await createDraft(token, [draft])
  console.log(`Draft created, media_id: ${draftMediaId}`)
  console.log('Go to mp.weixin.qq.com -> Draft to preview.')
}

main().catch((err) => {
  console.error('Publish failed:', err)
  process.exit(1)
})
