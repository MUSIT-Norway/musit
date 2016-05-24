/**
 * Created by steinaol on 5/20/16.
 */
var jsdom = require('mocha-jsdom');
var assert = require('assert');
var React = require('react');
const ReactDOM = require('react-dom');
var ReactTestUtils = require('react-addons-test-utils');


require('../../../../test/setup');


describe('MusitTextField', function () {
  it('should render MusitTextField', function () {
      var MusitTextField = require('../index');
      var myDiv = ReactTestUtils.renderIntoDocument(
        <MusitTextField
          labelText="Navn"
          placeHolderText="skriv inn navn her"
          valueText="flint"
          onChange={() => null}
        />
      );

      var actualDiv = ReactDOM.findDOMNode(myDiv);
      const label = actualDiv.querySelectorAll('div')[0];
      assert.equal(label.textContent, 'Navn', 'Navn må være tilstede')
      const field = actualDiv.querySelectorAll('input')[0];
      assert.equal(field.valueText, 'flint', 'Felt må være tilstede')
    });
});
