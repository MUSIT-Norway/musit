import React from 'react'
import StorageUnitList from '../../components/storageunits/StorageUnitList'
import { Panel, Grid, Row, PageHeader } from 'react-bootstrap'

export default class StorageUnitsContainer extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props)
    this.state = {
      units: [
        {
          id: 1,
          name: 'Test data'
        },
        {
          id: 2,
          name: 'Test data'
        },
        {
          id: 3,
          name: 'Test data'
        },
        {
          id: 4,
          name: 'Test data'
        },
        {
          id: 5,
          name: 'Test data'
        },
        {
          id: 6,
          name: 'Test data'
        },
      ]
    }
  }

  render() {
    const styles = require('./StorageUnitsContainer.scss');

    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row styleClass="row-centered">
                <PageHeader className={styles.pageHeader}>
                  {this.props.translate('musit.storageUnits.title', true)}
                </PageHeader>
                <StorageUnitList
                  units={ this.state.units }
                  onDelete={(id) => console.log(`Deleting ${id}`)}
                  onEdit={(id) => console.log(`Editing ${id}`)}
                />
              </Row>
            </Grid>
          </Panel>
        </main>
      </div>
    );
  }
}
