import { assert, React, ReactDOM, ReactTestUtils } from '../../../../test/setup';

describe('MusitTextArea', () => {
  it('should render MusitTextArea', () => {
    const MusitTextArea = require('../index');
    const myDiv = ReactTestUtils.renderIntoDocument(
        <MusitTextArea
          controlId="navn"
          labelText="Navn"
          placeHolderText="skriv inn navn her"
          validationState={(key) => key}
          valueText={() => 'flint'}
          onChange={() => null}
        />
      );

    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const label = actualDiv.querySelectorAll('label')[0];
    assert.equal(label.textContent, 'Navn', 'Navn må være tilstede')
    const field = actualDiv.querySelectorAll('textarea')[0];
    assert.equal(field.value, 'flint', 'Felt må være tilstede')
  });

  it('should render MusitTextArea blank', () => {
    const MusitTextArea = require('../index');
    const myDiv = ReactTestUtils.renderIntoDocument(
        <MusitTextArea
          controlId="navn"
          labelText="Navn"
          placeHolderText="skriv inn navn her"
          validationState={(key) => key}
          valueText={() => null}
          onChange={() => null}
        />
      );

    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const label = actualDiv.querySelectorAll('label')[0];
    assert.equal(label.textContent, 'Navn', 'Navn må være tilstede')
    const field = actualDiv.querySelectorAll('textarea')[0];
    assert.equal(field.value, '', 'Felt må være tilstede')
  });

  it('number input', () => {
    const MusitTextArea = require('../index');
    const myDiv = ReactTestUtils.renderIntoDocument(
      <MusitTextArea
        controlId="navn"
        labelText="Navn"
        placeHolderText="skriv inn navn her"
        validationState={(key) => key}
        valueText={() => '123454566'}
        onChange={() => null}
      />
      );

    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const label = actualDiv.querySelectorAll('label')[0];
    assert.equal(label.textContent, 'Navn', 'Navn må være tilstede')
    const field = actualDiv.querySelectorAll('textarea')[0];
    assert.equal(field.value, '123454566', 'Felt må være tilstede')
  });
});
