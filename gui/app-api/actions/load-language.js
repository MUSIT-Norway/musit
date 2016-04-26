var yaml = require('js-yaml')
var fs = require('fs')
var path = require('path')

export default function loadLanguage() {
  return new Promise((resolve) => {
    var languageDef = yaml.safeLoad(fs.readFileSync(path.resolve(__dirname, 'language.yaml'), 'utf-8'))
    resolve(languageDef)
  })
}
