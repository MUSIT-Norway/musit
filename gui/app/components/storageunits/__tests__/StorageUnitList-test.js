const assert = require('assert');
const React = require('react');
const ReactDOM = require('react-dom');
const ReactTestUtils = require('react-addons-test-utils');

require('../../../../test/setup');

describe('StorageUnitList', () => {
  it('should render all units', () => {
    const StorageUnitList = require('../StorageUnitList');
    const myDiv = ReactTestUtils.renderIntoDocument(
      <StorageUnitList units={[
        {
          name: 'Hallo',
        },
        {
          name: 'Kokko',
        }
      ]}
      />
    );
    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const spans = actualDiv.querySelectorAll('span');
    assert.equal(spans.length, 2);
    assert.equal(spans[0].textContent, 'Hallo');
    assert.equal(spans[1].textContent, 'Kokko');
  });
});
