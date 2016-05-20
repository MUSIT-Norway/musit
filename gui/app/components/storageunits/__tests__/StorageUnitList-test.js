var jsdom = require('mocha-jsdom');
var assert = require('assert');
var React = require('react');
var ReactTestUtils = require('react-addons-test-utils');

require('../../../../test/setup');

describe('StorageUnitList', function() {
    it('should render all units', function() {
        var StorageUnitList = require('../StorageUnitList');
        var myDiv = ReactTestUtils.renderIntoDocument(
            <StorageUnitList units={[
                {
                    name: "Hallo",
                },
                {
                    name: "Kokko",
                }
            ]} />
        );
        let actualDiv = React.findDOMNode(myDiv);
        let spans = actualDiv.querySelectorAll("span");
        assert.equal(spans.length, 2);
        assert.equal(spans[0].textContent, "Hallo");
        assert.equal(spans[1].textContent, "Kokko");
    });
});