/**
 * Created by steinaol on 5/20/16.
 */
const assert = require('assert');
const React = require('react');
const ReactDOM = require('react-dom');
const ReactTestUtils = require('react-addons-test-utils');


require('../../../../test/setup');


describe('MusitTextField', () => {
  it('should render MusitTextField', () => {
    const MusitTextField = require('../index');
    const myDiv = ReactTestUtils.renderIntoDocument(
        <MusitTextField
          controlId="navn"
          labelText="Navn"
          placeHolderText="skriv inn navn her"
          validationState={(key) => key}
          valueText={() => 'flint'}
          valueType="text"
          onChange={() => null}
        />
      );

    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const label = actualDiv.querySelectorAll('label')[0];
    assert.equal(label.textContent, 'Navn', 'Navn må være tilstede')
    const field = actualDiv.querySelectorAll('input')[0];
    assert.equal(field.value, 'flint', 'Felt må være tilstede')
  });
});
