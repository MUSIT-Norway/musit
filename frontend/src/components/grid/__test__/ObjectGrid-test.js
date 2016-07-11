import { assert, React, ReactTestUtils } from '../../../../test/setup';
import ObjectGrid from '../ObjectGrid';

describe('ObjectGrid', () => {
  let firstComponent;
  let secondComponent;

  before('should render ObjectGrid', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <ObjectGrid
        id="1"
        translate={(key) => key}
        objectGridData={[
          {
            museumsNumber: 'C10001',
            uNumber: 1,
            term: 'Gråstein'
          },
          {
            museumsNumber: 'C10002',
            uNumber: 2,
            term: 'Spydspiss'
          }
        ]}
      />
    );
    const inputComponent = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'td');
    firstComponent = inputComponent[0];
    secondComponent = inputComponent[1];
  });

  it('Check the temperature component is created by checking down button id', () => {
    assert(firstComponent.getAttribute('id') === '1_C10001_1_museumNumber')
  })
  it('Check the relativeHumidity component is created by checking down button id', () => {
    assert(secondComponent.getAttribute('id') === '1_C10001_1_uNumber')
  })
})
