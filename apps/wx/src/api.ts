import { readFile } from 'node:fs/promises'
import { basename } from 'node:path'
import type {
  AccessTokenResponse,
  Article,
  DraftAddResponse,
  UploadResponse,
  WxError,
} from './types.js'

const BASE_URL = 'https://api.weixin.qq.com/cgi-bin'

function assertOk(data: Partial<WxError>): void {
  if (data.errcode && data.errcode !== 0) {
    throw new Error(`WeChat API error ${data.errcode}: ${data.errmsg}`)
  }
}

export async function getAccessToken(appId: string, appSecret: string): Promise<string> {
  const url = `${BASE_URL}/token?grant_type=client_credential&appid=${appId}&secret=${appSecret}`
  const res = await fetch(url)
  const data = (await res.json()) as AccessTokenResponse & Partial<WxError>
  assertOk(data)
  return data.access_token
}

export async function uploadMaterial(
  token: string,
  filePath: string,
  type: 'image' | 'voice' | 'video' | 'thumb' = 'image',
): Promise<UploadResponse> {
  const url = `${BASE_URL}/material/add_material?access_token=${token}&type=${type}`
  const fileBuffer = await readFile(filePath)
  const fileName = basename(filePath)

  const form = new FormData()
  form.append('media', new Blob([fileBuffer]), fileName)

  const res = await fetch(url, { method: 'POST', body: form })
  const data = (await res.json()) as UploadResponse & Partial<WxError>
  assertOk(data)
  return data
}

export async function createDraft(token: string, articles: Article[]): Promise<DraftAddResponse> {
  const url = `${BASE_URL}/draft/add?access_token=${token}`
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ articles }),
  })
  const data = (await res.json()) as DraftAddResponse & Partial<WxError>
  assertOk(data)
  return data
}
