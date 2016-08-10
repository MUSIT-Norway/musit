import React from 'react'
import { Button } from 'react-bootstrap'
import { MusitField } from '../components/formfields'
import styles from './Toolbar.scss'

export default class Toolbar extends React.Component {
  static propTypes = {
    showLeft: React.PropTypes.bool.isRequired,
    showRight: React.PropTypes.bool.isRequired,
    clickShowLeft: React.PropTypes.func.isRequired,
    clickShowRight: React.PropTypes.func.isRequired,
    labelLeft: React.PropTypes.string.isRequired,
    labelRight: React.PropTypes.string.isRequired,
    placeHolderSearch: React.PropTypes.string
  }

  render() {
    return (
      <div className={styles.wrapper}>
        <div className={styles.searchField}>
          <MusitField
            id={'search'}
            addOnPrefix={'\u2315'}
            placeHolder={this.props.placeHolderSearch}
            value=""
            validate="text"
          />
        </div>
        <div className={styles.toolBarButtons}>
          <Button active={this.props.showLeft} onClick={() => this.props.clickShowLeft()}>
            {this.props.labelLeft}
          </Button>
          <Button active={this.props.showRight} onClick={() => this.props.clickShowRight()}>
            {this.props.labelRight}
          </Button>
        </div>
      </div>
    )
  }
}
