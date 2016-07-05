import { assert, React, ReactDOM, ReactTestUtils } from '../../../../test/setup';
const StorageUnitList = require('../StorageUnitList');

describe('StorageUnitList', () => {
  it('should render all units', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <StorageUnitList
        units={[
          {
            id: 1,
            name: 'Hallo'
          },
          {
            id: 2,
            name: 'Kokko'
          },
          {
            id: 3,
            name: 'Lalal'
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
    assert.equal(td[2].textContent, 'Hallo');
    // row 2
    assert.equal(td[4].textContent, '2');
    assert.equal(td[6].textContent, 'Kokko');
    // row 3
    assert.equal(td[8].textContent, '3');
    assert.equal(td[10].textContent, 'Lalal');
  });
});
