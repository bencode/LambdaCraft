export type AccessTokenResponse = {
  access_token: string
  expires_in: number
}

export type UploadResponse = {
  media_id: string
  url: string
}

export type DraftAddResponse = {
  media_id: string
}

export type Article = {
  title: string
  content: string
  thumb_media_id: string
  author?: string
  digest?: string
  content_source_url?: string
  need_open_comment?: 0 | 1
  only_fans_can_comment?: 0 | 1
}

export type WxError = {
  errcode: number
  errmsg: string
}
