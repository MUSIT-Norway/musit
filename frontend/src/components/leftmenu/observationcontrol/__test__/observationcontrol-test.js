import { assert, React, ReactTestUtils } from '../../../../../test/setup';
import ObservationControlComponent from '../index';

describe('ObservationControlComponent', () => {
  let inputComponent
  let buttons
  before('should render ObservationControlComponent', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <ObservationControlComponent
        id="1"
        translate={(key) => key}
        selectObservation={(key) => key}
        selectControl={(key) => key}
        onClickNewObservation={(key) => key}
        onClickNewControl={(key) => key}
        onClickSelectObservation={(key) => key}
        onClickSelectControl={(key) => key}
      />
    );
    inputComponent = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'input');
    buttons = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'button');
  });
  it('Check the new observation button is created.', () => {
    assert(buttons[0].getAttribute('id') === '1_newObservation')
  })
  it('Check the new control button is created.', () => {
    assert(buttons[1].getAttribute('id') === '1_newControl')
  })
  it('Check the control checkbox is created.', () => {
    assert(inputComponent[0].getAttribute('id') === '1_selectControl')
  })
  it('Check the observation checkbox is created.', () => {
    assert(inputComponent[1].getAttribute('id') === '1_selectObservation')
  })
})
