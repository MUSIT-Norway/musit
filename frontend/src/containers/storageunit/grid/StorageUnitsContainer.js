import React from 'react'
import StorageUnitList from '../../../components/storageunits/StorageUnitList'
import { Grid, Row, PageHeader } from 'react-bootstrap'
import Loader from 'react-loader';

export default class StorageUnitsContainer extends React.Component {
  static propTypes = {
    units: React.PropTypes.arrayOf(React.PropTypes.object),
    translate: React.PropTypes.func.isRequired,
    loadStorageUnits: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEdit: React.PropTypes.func.isRequired,
    onPick: React.PropTypes.func.isRequired
  }

  componentWillMount() {
    this.props.loadStorageUnits()
  }

  render() {
    return (
      <div>
        <Grid>
          <Row
            style={{ paddingLeft: 40 }}
          >
            <PageHeader>
              {this.props.translate('musit.storageUnits.title', true)}
            </PageHeader>
            <Loader
              loaded={this.props.units}
            >
              <StorageUnitList
                units={this.props.units}
                onDelete={(unit) => this.props.onDelete(unit)}
                onEdit={(unit) => this.props.onEdit(unit)}
                onPick={(unit) => this.props.onPick(unit)}
              />
            </Loader>
          </Row>
        </Grid>
      </div>
    )
  }
}
