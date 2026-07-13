import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const SOURCE_ROOT = 'src'
const EXTENSIONS = new Set(['.ts', '.tsx'])
const PATTERNS = [
  'Рџ',
  'Рќ',
  'Р—',
  'РЎ',
  'СЃ',
  'С‚',
  'в†',
  'в‚',
  'рџ',
]

function hasSourceExtension(filePath) {
  return [...EXTENSIONS].some((extension) => filePath.endsWith(extension))
}

function collectSourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(directory, entry.name)

    if (entry.isDirectory()) {
      return collectSourceFiles(entryPath)
    }

    return hasSourceExtension(entryPath) ? [entryPath] : []
  })
}

const findings = []

for (const filePath of collectSourceFiles(SOURCE_ROOT)) {
  const lines = readFileSync(filePath, 'utf8').split(/\r?\n/)

  lines.forEach((line, index) => {
    const matchedPattern = PATTERNS.find((pattern) => line.includes(pattern))

    if (matchedPattern) {
      findings.push(`${filePath}:${index + 1}: ${matchedPattern}`)
    }
  })
}

if (findings.length > 0) {
  console.error('Possible mojibake text found:')
  console.error(findings.join('\n'))
  process.exit(1)
}

console.log('No mojibake patterns found.')
