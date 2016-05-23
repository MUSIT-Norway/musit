const yaml = require('js-yaml')
const fs = require('fs')
const path = require('path')

export default function loadLanguage() {
  return new Promise((resolve) =>
    resolve(yaml.safeLoad(fs.readFileSync(path.resolve(__dirname, 'language.yaml'), 'utf-8')))
  )
}
