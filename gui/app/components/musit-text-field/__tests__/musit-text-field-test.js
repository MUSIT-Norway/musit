/**
 * Created by steinaol on 5/20/16.
 */
var jsdom = require('mocha-jsdom');
var assert = require('assert');
var React = require('react');
var ReactTestUtils = require('react-addons-test-utils');


require('../../../../test/setup');

describe('MusitTextField', function() {
    it('should render all units', function() {
        var MusitTextField = require('../../musit-text-field');
        var myDiv = ReactTestUtils.renderIntoDocument(
            <MusitTextField type='text' placeHolderText='test-text' controlId='id1'/>
        );
        let actualDiv = React.findDOMNode(myDiv);
        let form = actualDiv.querySelectorAll("form");
        assert.equal(form.length, 2);
        assert.equal(form[0].textContent, "Hallo");
        assert.equal(form[1].textContent, "Kokko");
    });
});