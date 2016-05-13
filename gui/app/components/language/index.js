/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
import React, { Component, PropTypes, bindActionCreators } from 'react'
import { connect } from 'react-redux'
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

const mapStateToProps = (state) => {
    return {
        language: state.language
    }
}

@connect(mapStateToProps)
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
