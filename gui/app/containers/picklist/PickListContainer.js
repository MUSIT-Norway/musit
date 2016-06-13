import React from 'react'
import { PickListComponent } from '../../components/picklist'
import { Panel, PageHeader } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class PickListContainer extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    picks: React.PropTypes.array.isRequired
  }

  render() {
    const { translate, picks } = this.props
    const renderIcon = () => {return (<FontAwesome name="archive" />)}
    const renderLabel = (pick) => {return pick.name}

    return (
      <div>
        <main>
          <PageHeader>
            {translate('musit.pickList.title', false)}
          </PageHeader>
          <Panel>
            <PickListComponent
              header={translate('musit.pickList.grid.title', false)}
              picks={picks}
              iconRendrer={renderIcon}
              labelRendrer={renderLabel}
            />
          </Panel>
        </main>
      </div>
    )
  }
}
