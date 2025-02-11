/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_APP_NAME: string
  readonly VITE_LOGO_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}