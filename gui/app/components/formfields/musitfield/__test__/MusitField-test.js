import { assert, React, ReactDOM, ReactTestUtils } from '../../../../../test/setup';

describe('MusitField', () => {
  it('should render MusitField 1', () => {
    const MusitField = require('../index');
    const myDiv = ReactTestUtils.renderIntoDocument(
        <MusitField
          id="navn"
          placeHolder="skriv inn navn her"
          value= "test value"
          validate= "text"
          onChange={() => null}
        />
      );

    const inputField = ReactDOM.findDOMNode(myDiv);
    const field = inputField.querySelectorAll('input')[0];
    assert.equal(field.value, 'test value', 'Field must be present')
  });

    it('should render MusitField 2', () => {
      const MusitField = require('../index');
      const myDiv = ReactTestUtils.renderIntoDocument(
          <MusitField
            id="navn"
            placeHolder="skriv inn navn her"
            help= "Help text"
            value= "just test value"
            validate= "text"
            onChange={() => null}
          />
        );

      const actualDiv = ReactDOM.findDOMNode(myDiv);
      const field = actualDiv.querySelectorAll('input')[0];
      assert.equal(field.value, 'just test value', 'Field must be present');

    });

/*
    it('should render MusitField 3', () => {
      const MusitField = require('../index');
      const myDiv = ReactTestUtils.renderIntoDocument(
          <MusitField
            id= "comments2"
            placeHolder= "test placeHolder"
            value= "Test value"
            help= "Help text"
            addOnPrefix= '\u00b1'
            // validate: () => EnvironmentRequirementComponent.validateString(this.state.environmentRequirement.comments2),
            tooltip= "tooltip text"
            validate= "text"
            minimumLength= {2}
            maximumLength= {5}
            onChange={() => null}
          />
        );

      const actualDiv = ReactDOM.findDOMNode(myDiv);
      console.log(actualDiv);
      //const field = actualDiv.querySelectorAll('input')[0];
      const a = actualDiv.getElementById('comments2');

      assert.equal(a.value, 'just test value', 'Field must be present');

    });
    */

    /*

    import { MusitField as Field } from '../../components/formfields'

    this.comments2 = {
      id: 'comments2',
      placeHolder: 'test placeHolder',
      // value: this.state.environmentRequirement.comments2,
      help: 'Help text',
      addOnPrefix: '\u00b1',
      // validate: () => EnvironmentRequirementComponent.validateString(this.state.environmentRequirement.comments2),
      tooltip: 'tooltip text',
      validate: 'text',
      minimumLength: 2,
      maximumLength: 5,
      /* validator: (value, {}) => {
        console.log('custom validator')
        const isSomething = value === 'test'
        return isSomething ? 'success' : 'error'
      },*/
      /*
      onChange: (comments2) => {
        this.setState({
          environmentRequirement: {
            ...this.state.environmentRequirement,
            comments2
          }
        })
      }
    }

    render() {
    const renderFieldBlock = (bindValue, fieldProps) => (
      <FormGroup>
        <label className="col-sm-3 control-label" htmlFor="comments2">Kommentar</label>
        <div class="col-sm-9" is="null">
          <Field {...fieldProps} value={bindValue} />
        </div>
      </FormGroup>
    )

                <Row>
                <Col md={6}>
                  <Form horizontal>
                    {renderFieldBlock(this.state.environmentRequirement.comments2, this.comments2)}
                  </Form>
                </Col>
              </Row>

    */
});
