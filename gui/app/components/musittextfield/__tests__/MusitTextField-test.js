/**
 * Created by steinaol on 5/20/16.
 */
var jsdom = require('mocha-jsdom');
var assert = require('assert');
var React = require('react');
//var dom = require('dom')
var ReactTestUtils = require('react-addons-test-utils');


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
        let actualDiv = React.findDOMNode(myDiv);
        let form = actualDiv.querySelectorAll("form");

    });
});