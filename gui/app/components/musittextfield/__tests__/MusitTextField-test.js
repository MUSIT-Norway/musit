/**
 * Created by steinaol on 5/20/16.
 */
var jsdom = require('mocha-jsdom');
var assert = require('assert');
var React = require('react');
var ReactTestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');


require('../../../../test/setup');


describe('MusitTextField', function() {
    it('should render MusitTextField', function() {
        var MusitTextField = require('../index');
        var myDiv = ReactTestUtils.renderIntoDocument(
            <MusitTextField type="text"
                            placeHolderText="test-text"
                            componentClass="input"
                            controlId="id1"/>
        );

        var actualDiv=ReactDOM.findDOMNode(myDiv);
        assert(actualDiv!= 'undefined');
        var t = ReactTestUtils.scryRenderedDOMComponentsWithTag(actualDiv,'FormControl')
        assert.equal(t.length,1,'Not equal')






        

    });
});