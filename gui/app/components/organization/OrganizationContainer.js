import React, { Component } from 'react'
import TextField from '../../components/musittextfield'
import { Form } from 'react-bootstrap'

export default class OrganizationContainer extends Component {
  static propTypes = {
    org: React.PropTypes.shape({
      fn: React.PropTypes.string,
      nickname: React.PropTypes.string,
      tel: React.PropTypes.string,
      web: React.PropTypes.string,
    }),
    updateFN: React.PropTypes.func.isRequired,
    updateNickname: React.PropTypes.func.isRequired,
    updateTel: React.PropTypes.func.isRequired,
    updateWeb: React.PropTypes.func.isRequired
  }

  static validateString(value, minimumLength = 3, maximumLength = 20) {
    const isSomething = value.length >= minimumLength
    const isValid = isSomething ? 'success' : null
    return value.length > maximumLength ? 'error' : isValid
  }

  constructor(props) {
    super(props)

    this.fields = [
      {
        key: 'orgFN',
        controlId: 'fullName',
        labelText: 'Full name',
        tooltip: 'Full name',
        valueType: 'text',
        placeHolderText: 'enter organization name here',
        validationState: () => OrganizationContainer.validateString(this.props.org.fn, 3, 60),
        valueText: () => this.props.org.fn,
        onChange: (fn) => this.props.updateFN(fn)
      },
      {
        key: 'orgNickname',
        controlId: 'nickname',
        labelText: 'Nickname',
        tooltip: 'Nickname',
        valueType: 'text',
        placeHolderText: 'enter organization short name here',
        validationState: () => OrganizationContainer.validateString(this.props.org.nickname, 0, 10),
        valueText: () => this.props.org.nickname,
        onChange: (nickname) => this.props.updateNickname(nickname)
      },
      {
        key: 'orgPhone',
        controlId: 'phone',
        labelText: 'Telephone number',
        tooltip: 'Telephone number',
        valueType: 'text',
        placeHolderText: 'enter phone number here',
        validationState: () => OrganizationContainer.validateString(this.props.org.fn, 0, 12),
        valueText: () => this.props.org.tel,
        onChange: (tel) => this.props.updateTel(tel)
      },
      {
        key: 'orgWeb',
        controlId: 'web',
        labelText: 'Web',
        tooltip: 'Web',
        valueType: 'text',
        placeHolderText: 'enter web address here',
        validationState: () => OrganizationContainer.validateString(this.props.org.fn, 0, 512),
        valueText: () => this.props.org.web,
        onChange: (web) => this.props.updateWeb(web)
      }
    ]
  }

  render() {
    return (
      <Form horizontal>
        {this.fields.map(field => <TextField {...field} />)}
      </Form>
    )
  }

}
