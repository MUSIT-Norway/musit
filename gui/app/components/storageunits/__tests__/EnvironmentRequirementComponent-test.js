import { assert, React, ReactDOM, ReactTestUtils } from '../../../../test/setup';
import EnvironmentRequirementComponent from '../EnvironmentRequirementComponent';

describe('EnvironmentRequirementComponent', () => {
  it('should render EnvironmentRequirementComponent', () => {
    const comments = {
      id: 'comments',
      numberOfRows: 4,
      validate: 'text'
    }
    const myDiv = ReactTestUtils.renderIntoDocument(
        <EnvironmentRequirementComponent
          comments= {comments}
        />
      );

    const actualDiv = ReactDOM.findDOMNode(myDiv);
    const field = actualDiv.querySelectorAll('textarea')[0];
    // assert.equal(field.value, 'flint', 'Felt må være tilstede')
  });

});
