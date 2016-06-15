import { assert, React, ReactDOM, ReactTestUtils } from '../../../../test/setup';

describe('MusitField', () => {
  it('should render MusitField', () => {
    const MusitField = require('../index');
    const myDiv = ReactTestUtils.renderIntoDocument(
        <MusitField
          id="navn"
          placeHolder="skriv inn navn her"
          value= "just test value"
          onChange={() => null}
        />
      );

    const inputField = ReactDOM.findDOMNode(myDiv);
    assert.equal(inputField.value, 'just test value', 'Field must be present')
  });



    it('should render MusitField 2', () => {
      const MusitField = require('../index');
      const myDiv = ReactTestUtils.renderIntoDocument(
          <MusitField
            id="navn"
            placeHolder="skriv inn navn her"
            help= "Help text"
            value= "just test value"
            onChange={() => null}
          />
        );

      const actualDiv = ReactDOM.findDOMNode(myDiv);
      const field = actualDiv.querySelectorAll('input')[0];
      assert.equal(field.value, 'just test value', 'Field must be present');

    });
});
