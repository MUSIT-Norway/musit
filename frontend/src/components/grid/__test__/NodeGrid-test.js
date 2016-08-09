import { assert, React, ReactTestUtils } from '../../../../test/setup';
import NodeGrid from '../NodeGrid';

describe('NodeGrid', () => {
  let inputComponent;
  before('should render NodeGrid', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <NodeGrid
        id={1}
        translate={(key) => key}
        onPick={(key) => key}
        onClick={(key) => key}
        tableData={[
          {
            name: 'Eske',
            type: 'Lagringsenh',
            objectCount: 0,
            totalObjectCount: 12,
            nodeCount: 0
          },
          {
            name: 'Pose',
            type: 'Lagringsenh',
            objectCount: 0,
            totalObjectCount: 16,
            nodeCount: 0
          }
        ]}
      />
    );
    inputComponent = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'td');
  });

  it('Check the first row first column is created.', () => {
    assert(inputComponent[0].getAttribute('id') === '1_Eske_Lagringsenh_nodeName')
  })
  it('Check the first row second column is created.', () => {
    assert(inputComponent[1].getAttribute('id') === '1_Eske_Lagringsenh_nodeType')
  })
  it('Check the value of first row second column.', () => {
    assert(inputComponent[1].innerHTML === 'Lagringsenh')
  })
  it('Check the value of second row second column.', () => {
    assert(inputComponent[10].innerHTML === 'Lagringsenh')
  })
  it('Check the value of second row fourth column.', () => {
    assert(inputComponent[12].innerHTML === '16')
  })
})
