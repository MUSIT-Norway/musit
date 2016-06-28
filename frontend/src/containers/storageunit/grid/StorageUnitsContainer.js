import React from 'react'
import StorageUnitList from '../../../components/storageunits/StorageUnitList'
import { Panel, Grid, Row, PageHeader } from 'react-bootstrap'

const StorageUnitsContainer = (props) =>
  <div>
    <main>
      <Panel>
        <Grid>
          <Row styleClass="row-centered">
            <PageHeader>
              {props.translate('musit.storageUnits.title', true)}
            </PageHeader>
            <StorageUnitList
              units={props.units}
              onDelete={() => null}
              onEdit={() => null}
            />
          </Row>
        </Grid>
      </Panel>
    </main>
  </div>

StorageUnitsContainer.propTypes = {
  units: React.PropTypes.arrayOf(React.PropTypes.object),
  translate: React.PropTypes.func.isRequired,
};

export default StorageUnitsContainer
