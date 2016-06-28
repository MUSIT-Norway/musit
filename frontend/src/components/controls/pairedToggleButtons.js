import React, { Component, PropTypes } from 'react';
import FontAwesome from 'react-fontawesome'

export default class pairedToogleButtons extends Component {
  PropTypes = {
    label: PropTypes.string.isRequired,
    value: PropTypes.string,
    controlId: PropTypes.string
  }

  render() {
    return (
      <div>
        <FontAwesome name="check" />
        <FontAwesome name="close" />
      </div>
    )
  }

}
