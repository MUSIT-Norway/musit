import { assert, React, ReactTestUtils } from '../../../../test/setup';
import ObservationDoubleTextAreaComponent from '../ObservationDoubleTextAreaComponent';

describe('ObservationDoubleTextAreaComponent', () => {
  let inputElementLeft;
  let inputElementRight;

  before('should render ObservationDoubleTextAreaComponent', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <ObservationDoubleTextAreaComponent
        id="test"
        translate={(key) => key}
        leftLabel="Left label"
        leftValue="left"
        leftTooltip="Left tooltip"
        onChangeLeft={() => ('ji')}
        rightLabel="Right label"
        rightValue="right"
        rightTooltip="Right tooltip"
        onChangeRight={() => ('ji')}
      />
    );
    const inputComponent = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'textarea');
    inputElementLeft = inputComponent[0];
    inputElementRight = inputComponent[1];
  });

  it('Check the left text area number of row', () => {
    assert(inputElementLeft.getAttribute('rows') === '5')
  })

  it('Check the left text area id', () => {
    assert(inputElementLeft.getAttribute('id') === 'test_left')
  })

  it('Check the left text area Tooltip', () => {
    assert(inputElementLeft.getAttribute('title') === 'Left tooltip')
  })

  it('Check left value', () => {
    assert(inputElementLeft.value === 'left')
  })

  it('Check the right text area number of row', () => {
    assert(inputElementRight.getAttribute('rows') === '5')
  })
})
