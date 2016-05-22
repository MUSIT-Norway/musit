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
    language: PropTypes.object.isRequired,
    value: PropTypes.string.isRequired,
    markdown: PropTypes.bool
  }

  static contextTypes = {
    store: PropTypes.object.isRequired
  }

  render() {
    let text = I18n.t(this.props.value)
    if (this.props.markdown && text) {
      try {
        const tmp = marked(text)
        text = tmp[0]
      } catch (err) {
        // console.log(err)
      }
    }
    return (
      <div>{text}</div>
    )
  }
}
