import { assert, React, ReactTestUtils } from '../../../../test/setup';
import ObjectGrid from '../ObservationControlGrid';

describe('ObservationControlGrid', () => {
  let inputComponent
  before('should render ObservationControlGrid', () => {
    const myDiv = ReactTestUtils.renderIntoDocument(
      <ObjectGrid
        id={1}
        translate={(key) => key}
        tableData={[
          {
            id: 1,
            type: 'control',
            date: '01.01.1983',
            types: { temperature: true,
              inertAir: null,
              relativeHumidity: null,
              cleaning: null,
              lightCondition: null,
              alchohol: true,
              gas: null,
              mold: null,
              pest: true,
              envdata: null },
            doneBy: 'Blablabla...',
            registeredDate: '01.01.1983',
            registeredBy: 'Blabla...'
          },
          {
            id: 2,
            type: 'observation',
            date: '01.01.1984',
            types: { temperature: true,
              inertAir: null,
              relativeHumidity: null,
              cleaning: null,
              lightCondition: true,
              alchohol: false,
              gas: null,
              mold: null,
              envdata: null },
            doneBy: 'Blablabla...',
            registeredDate: '01.01.1983',
            registeredBy: 'Blabla...'
          }
        ]}
      />
    );
    inputComponent = ReactTestUtils.scryRenderedDOMComponentsWithTag(myDiv, 'td');
  });

  it('Check the 1st row Type number id.', () => {
    assert(inputComponent[0].getAttribute('id') === '1_01.01.1983_type')
  })
  it('Check the 1st row Date id', () => {
    assert(inputComponent[1].getAttribute('id') === '1_01.01.1983_date')
  })
  it('Check the 1st row Date value', () => {
    assert(inputComponent[1].innerHTML === '01.01.1983')
  })
  it('Check the 1st row registered date value', () => {
    assert(inputComponent[4].innerHTML === '01.01.1983')
  })
  it('Check the 2nd row Date value', () => {
    assert(inputComponent[8].innerHTML === '01.01.1984')
  })
  it('Check the 2nd row registered date value', () => {
    assert(inputComponent[11].innerHTML === '01.01.1983')
  })
})
