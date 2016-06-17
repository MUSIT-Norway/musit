import React, { Component, PropTypes } from 'react'

import { I18n } from 'react-i18nify'
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
    key: PropTypes.string.isRequired,
    markdown: PropTypes.bool
  }

  static translate = (key, markdown) => {
    let text = I18n.t(key)
    if (markdown && text) {
      try {
        const tmp = marked(text)
        text = tmp[0]
      } catch (err) {
        console.log(`Could not compile markdown for ${key}`)
      }
    }
    return text;
  }

  render() {
    let text = Language.translate(this.props.key, this.props.markdown)
    return (
      <div>{text}</div>
    )
  }
}
