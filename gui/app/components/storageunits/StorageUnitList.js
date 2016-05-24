import React from 'react';

class StorageUnitList extends React.Component {
  render() {
    return (
      <ul>
        {this.props.units.map(u => (
          <li style={{ padding: 5 }}>
            <span style={{ paddingRight: 10 }}>{u.name}</span>
            <button onClick={() => this.props.onEdit(u.id)}>Edit</button>
            <button onClick={() => this.props.onDelete(u.id)}>Delete</button>
          </li>
        ))}
      </ul>
    );
  }
}

StorageUnitList.propTypes = {
  units: React.PropTypes.arrayOf(React.PropTypes.shape({
    id: React.PropTypes.number.isRequired,
    name: React.PropTypes.string.isRequired
  })).isRequired,
  onDelete: React.PropTypes.func.isRequired,
  onEdit: React.PropTypes.func.isRequired,
};

export default StorageUnitList
