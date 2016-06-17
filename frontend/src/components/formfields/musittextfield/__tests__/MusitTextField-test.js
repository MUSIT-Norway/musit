import { assert, React, ReactDOM, ReactTestUtils } from '../../../../../test/setup';
const MusitTextField = require('../index');

describe('MusitTextField', () => {
  it('should render MusitTextField', () => {
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
