import React from 'react'
import { Button } from 'react-bootstrap'
import { MusitField } from '../../../components/formfields'

export default class Toolbar extends React.Component {
  static propTypes = {
    showObjects: React.PropTypes.bool.isRequired,
    showNodes: React.PropTypes.bool.isRequired,
    clickShowNodes: React.PropTypes.func.isRequired,
    clickShowObjects: React.PropTypes.func.isRequired
  }

  render() {
    const styles = require('./Toolbar.scss')
    return (
      <div className={styles.wrapper}>
        <div className={styles.searchField}>
          <MusitField
            id={'search'}
            addOnPrefix={'\u2315'}
            placeHolder="Filtrer i liste"
            value=""
            validate="text"
          />
        </div>
        <div className={styles.toolBarButtons}>
          <Button active={this.props.showNodes ? 'active' : undefined} onClick={() => this.props.clickShowNodes()}>
            Noder
          </Button>
          <Button active={this.props.showObjects ? 'active' : undefined} onClick={() => this.props.clickShowObjects()}>
            Objekter
          </Button>
        </div>
      </div>
    )
  }
}
