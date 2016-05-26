import React from 'react';

class StorageUnitList extends React.Component {
  render() {
    return (
      <div>
        {this.props.units.map(u =>
          <span>{u.name}</span>
        )}
      </div>
    );
  }
}

StorageUnitList.propTypes = {
  units: React.PropTypes.arrayOf(React.PropTypes.shape({
    name: React.PropTypes.string.isRequired
  })).isRequired
};

export default StorageUnitList
