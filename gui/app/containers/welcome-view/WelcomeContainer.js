import React from 'react'
import {Panel, Button, Grid, Row, Col, PageHeader} from 'react-bootstrap';

export default class WelcomeContainer extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object,
  };

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const styles = require('./WelcomeContainer.scss');
    const translate = this.props.translate;
    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row styleClass="row-centered">
                <Col xs={10} md={10} style={{textAlign: "center"}}>
                  <PageHeader>
                    {translate("musit.welcomePage.title", true)}
                  </PageHeader>
                  <div className={styles.panelWithBackground}>
                    <span style={{fontSize: "larger", margin: "1em"}}>
                      {translate("musit.welcomePage.body", true)}
                    </span>
                  </div>
                </Col>
              </Row>
              <Row styleClass="row-centered">
                { /*<Col xs={10} md={10} style={{textAlign: "center"}}>
                  {translate("musit.welcomePage.noAccess", false)}
                </Col>*/ }
              </Row>
              {!this.props.user &&
                <Row styleClass="row-centered">
                  <Col xs={10} md={10} style={{textAlign: "center"}}>
                    <Button
                      bsStyle={"default"}
                      style={{marginTop: "1em"}}
                      height={"20"}
                      src={"feide-login-icon.png"}
                    >
                      <a href="/musit">{translate("musit.login", true)}</a>
                    </Button>
                  </Col>
                </Row>
              }
            </Grid>
          </Panel>
        </main>
      </div>
    );
  }
}