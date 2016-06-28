import { assert, React, ReactTestUtils } from '../../../../test/setup';
import EnvironmentRequirementComponent from '../EnvironmentRequirementComponent';

describe('EnvironmentRequirementComponent', () => {
  let inputElement;

  before('should render EnvironmentRequirementComponent', () => {
    const comments = {
      id: 'comments',
      numberOfRows: 4,
      validate: 'text',
      value: 'Hi'
    };
    const myDiv = ReactTestUtils.renderIntoDocument(
      <EnvironmentRequirementComponent
        comments={comments}
        translate={(key) => key}
      />
    );
    const inputComponent = ReactTestUtils.findRenderedDOMComponentWithTag(myDiv, 'textarea');
    inputElement = inputComponent;
  });

  it('Title should show the lanaguage title', () => {
    assert(inputElement.getAttribute('title') === 'musit.storageUnits.environmentRequirements.comments.tooltip');
  });
});
