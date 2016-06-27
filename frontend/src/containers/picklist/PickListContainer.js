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

  constructor(props) {
    super(props)
    this.state = {
      showActionDialog: false
    }
    this.onOpenActionDialog = this.onOpenActionDialog.bind(this)
    this.onCloseActionDialog = this.onCloseActionDialog.bind(this)
  }

  onOpenActionDialog() {
    this.setState({ showActionDialog: true })
  }

  onCloseActionDialog() {
    this.setState({ showActionDialog: false })
  }

  render() {
    const demoActions = [
      {
        id: 'testAction',
        description: 'Demo Action',
        func: () => ('works')
      },
      {
        id: 'testAction2',
        description: 'Demo Action 2',
        func: () => ('works 2')
      }
    ]
    const style = require('../../components/picklist/index.scss')
    const { translate, picks, marked, onToggleMarked } = this.props
    const { showActionDialog } = this.state
    const renderIcon = () => { return (<FontAwesome name="archive" />) }
    const renderLabel = (pick) => { return pick.name }
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
              <Button disabled={marked.length <= 0} onClick={() => this.onOpenActionDialog()}>
                <FontAwesome name="play-circle" className={style.normalAction} />
                {translate('musit.pickList.action.executeAll', false)}
              </Button>
            </PageHeader>
            <PickListComponent
              picks={picks}
              marked={marked}
              actions={demoActions}
              showActionDialog={showActionDialog}
              onCloseActionDialog={() => this.onCloseActionDialog()}
              iconRendrer={renderIcon}
              labelRendrer={renderLabel}
              onToggleMarked={onToggleMarked}
            />
            <div style={{ textAlign: 'right' }}>
              {marked.length}/{picks.length} node(r) valgt.
            </div>
          </Panel>
        </main>
      </div>
    )
  }
}
