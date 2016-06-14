import React from 'react'
import { PickListComponent } from '../../components/picklist'
import { Panel, PageHeader, Button } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class PickListContainer extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    picks: React.PropTypes.array.isRequired,
    marked: React.PropTypes.array.isRequired,
    onToggleMarked: React.PropTypes.func.isRequired
  }

  render() {
    const style = require('../../components/picklist/index.scss')
    const { translate, picks, marked, onToggleMarked } = this.props
    const renderIcon = () => {return (<FontAwesome name="archive" />)}
    const renderLabel = (pick) => {return pick.name}
    const checkSymbol = (marked.length > 0) ? 'square-o' : 'check-square-o'

    return (
      <div className={style.picklist}>
        <main>
          <Panel>
            <PageHeader>
              {translate('musit.pickList.title', false)}
              <Button onClick={(e) => onToggleMarked(e, null)}>
                <FontAwesome name={checkSymbol} className={style.normalAction} />
                {translate('musit.pickList.action.markAll', false)}
              </Button>
              <Button>
                <FontAwesome name="play-circle" className={style.normalAction} />
                {translate('musit.pickList.action.executeAll', false)}
              </Button>
            </PageHeader>
            <PickListComponent
              picks={picks}
              marked={marked}
              iconRendrer={renderIcon}
              labelRendrer={renderLabel}
              onToggleMarked={onToggleMarked}
            />
          </Panel>
        </main>
      </div>
    )
  }
}
