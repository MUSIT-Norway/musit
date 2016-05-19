import React, { Component, PropTypes, bindActionCreators } from 'react'

import { I18n, Translate } from 'react-i18nify'
import marked from 'react-marked'

marked.setOptions({
  renderer: new marked.Renderer(),
  gfm: true,
  tables: true,
  breaks: true,
  pedantic: false,
  sanitize: true,
  smartLists: false,
  smartypants: false
});

export default class Language extends Component {
  static propTypes = {
    language: PropTypes.object
  }

  static contextTypes = {
    store: PropTypes.object.isRequired
  }

  render () {
    const { language, value, markdown } = this.props
    I18n.loadTranslations(language.data)
    I18n.setLocale("no")

    var text = I18n.t(value)
    if (markdown && text) {
      try {
        const tmp = marked(text)
        text = tmp[0]

      } catch (err) {
        //console.log(err)
      }
    }

    return (
      <div>{text}</div>
    )
  }
}