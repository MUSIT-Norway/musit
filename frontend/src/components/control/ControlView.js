import React, { Component } from 'react'
import { Accordion, Panel, FormGroup, PanelGroup, Button } from 'react-bootstrap'

export default class ControlView extends Component {
  constructor(...args) {
    super(...args);
    this.state = {
      open: true
    };
  }
  render() {
    const a = (
      <Accordion>
        <Panel header="We have to add something here" eventKey="1">
          this is wokring
        </Panel>
        <Panel header="Collapsible Group Item #2" eventKey="2">
          Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus
        </Panel>
        <Panel header="Collapsible Group Item #3" eventKey="3">
          Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus
        </Panel>
      </Accordion>
      )
    const b = (
      <PanelGroup defaultActiveKey="2" accordion>
        <Panel
          header="Panel 1"
          eventKey="1"
        >
          Panel 1 content
        </Panel>
        <Panel header="Panel 2" eventKey="2">Panel 2 content</Panel>
      </PanelGroup>
    )
    const c = (
      <div>
        <Button onClick={() => this.setState({ open: !this.state.open })}>
          click
        </Button>
        <Panel collapsible expanded={this.state.open}>
          Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid.
          Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
        </Panel>
      </div>
    )

    return (
      <FormGroup>
        <br />
        {a}
        <br />
        {b}
        <br />
        {c}
      </FormGroup>
    )
  }
}
