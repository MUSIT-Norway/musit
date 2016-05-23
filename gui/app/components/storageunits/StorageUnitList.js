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
  tuples: React.PropTypes.arrayOf(React.PropTypes.shape({
    id: React.PropTypes.number.isRequired,
    name: React.PropTypes.string.isRequired
  })).isRequired
};

export default StorageUnitList
