

import { assert, React, ReactTestUtils } from '../../../../../test/setup';
import NodeLeftMenuComponent from '../index';

describe('NodeLeftMenuComponent', () => {
  let labels
  let buttons
  before('should render ObservationControlComponent', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <NodeLeftMenuComponent
        id="1"
        translate={(key) => key}
        onClickNewNode={(key) => key}
        objectsOnNode={11}
        totalObjectCount={78}
        underNodeCount={5}
        onClickProperties={(key) => key}
        onClickObservations={(key) => key}
        onClickController={(key) => key}
        onClickMoveNode={(key) => key}
        onClickDelete={(key) => key}
      />
    );
    labels = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'label');
    buttons = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'button');
  });
  it('Check the New newNode button is created.', () => {
    assert(buttons[0].getAttribute('id') === '1_newNode')
  })
  it('Check the New properties button is created.', () => {
    assert(buttons[1].getAttribute('id') === '1_properties')
  })
  it('Check the objectsOnNode label is created.', () => {
    assert(labels[0].getAttribute('id') === '1_objectsOnNode')
  })
  it('Check the totalObjectCount label is created.', () => {
    assert(labels[1].getAttribute('id') === '1_totalObjectCount')
  })
})
