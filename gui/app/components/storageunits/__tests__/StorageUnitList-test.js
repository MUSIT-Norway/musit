const assert = require('assert');
const React = require('react');
const ReactDOM = require('react-dom');
const ReactTestUtils = require('react-addons-test-utils');

require('../../../../test/setup');

describe('StorageUnitList', () => {
  it('should render all units', () => {
    const StorageUnitList = require('../StorageUnitList');
    const myDiv = ReactTestUtils.renderIntoDocument(
      <StorageUnitList
        units={[
          {
            id: 1,
            name: 'Hallo',
          },
          {
            id: 2,
            name: 'Kokko',
          },
          {
            id: 3,
            name: 'Lalal',
          }
        ]}
        onDelete={() => null}
        onEdit={() => null}
      />
    );
    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const td = actualDiv.querySelectorAll('td');
    assert.equal(td.length, 12);
    // row 1
    assert.equal(td[0].textContent, '1');
    assert.equal(td[1].textContent, 'Hallo');
    // row 2
    assert.equal(td[4].textContent, '2');
    assert.equal(td[5].textContent, 'Kokko');
    // row 3
    assert.equal(td[8].textContent, '3');
    assert.equal(td[9].textContent, 'Lalal');
  });
});
