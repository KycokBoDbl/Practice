import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const SOURCE_ROOT = 'src'
const EXTENSIONS = new Set(['.ts', '.tsx'])
const PATTERNS = [
  { label: 'replacement character', pattern: /\uFFFD/u },
  { label: 'latin mojibake prefix', pattern: /[\u00D0\u00D1][\u0080-\u00BF]/u },
  { label: 'double-encoded Рђ/Рџ/etc', pattern: /\u0420[\u0402\u0403\u0405\u0406\u0407\u0408\u040A\u040B\u040C\u040E\u040F\u0452\u0453\u0455\u0456\u0457\u0458\u045A\u045B\u045C\u045E\u045F]/u },
  { label: 'double-encoded СЃ/СЊ/etc', pattern: /\u0421[\u0402\u0403\u0405\u0406\u0407\u0408\u040A\u040B\u040C\u040E\u040F\u0452\u0453\u0455\u0456\u0457\u0458\u045A\u045B\u045C\u045E\u045F]/u },
  { label: 'double-encoded symbols', pattern: /[\u0432\u0440][\u0402\u0403\u2018-\u201E\u20AC]/u },
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
    const matchedPattern = PATTERNS.find(({ pattern }) => pattern.test(line))

    if (matchedPattern) {
      findings.push(`${filePath}:${index + 1}: ${matchedPattern.label}`)
    }
  })
}

if (findings.length > 0) {
  console.error('Possible mojibake text found:')
  console.error(findings.join('\n'))
  process.exit(1)
}

console.log('No mojibake patterns found.')
