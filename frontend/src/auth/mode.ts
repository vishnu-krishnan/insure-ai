import type { Config } from '../config'

export function isMockAuthEnabled(config: Config): boolean {
  return import.meta.env.DEV && config.authMode === 'mock'
}
