import React from 'react'
import StorageUnitList from '../../components/storageunits/StorageUnitList'


export default class StorageUnitsContainer extends React.Component {
  // static propTypes = {
  //   tuples: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  // };

  constructor(props) {
    super(props)
    this.state = {
      tuples: [
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
    // const styles = require('./StorageUnitsContainer.scss');

    return (
      <div>
        <main>
          <StorageUnitList
            tuples={ this.state.tuples }
          />
        </main>
      </div>
    );
  }
}
