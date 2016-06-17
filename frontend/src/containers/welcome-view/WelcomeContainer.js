import React from 'react'
import { Panel, Button, Grid, Row, Col, PageHeader } from 'react-bootstrap'
import FakeLoginSelector from '../../components/fake-login-selector'
require('./WelcomeContainer.scss')

export default class WelcomeContainer extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object,
    setUser: React.PropTypes.func.isRequired,
  };

  render() {
    const translate = this.props.translate;
    let loginButton
    if (__FAKE_FEIDE__) {
      loginButton = <FakeLoginSelector />
    } else {
      loginButton = (
        <Button
          bsStyle="default"
          style={{ marginTop: '1em' }}
          height="20"
          src="feide-login-icon.png"
        >
          <span
            onClick={() => {
              const url = `/musit?t=${new Date().getTime()}`;
              const webWindow = window.open(url, '_blank',
                'menubar=yes,location=no,scrollbars=yes,width=800,height=600,' +
                 'status=no,resizable=yes,top=0,left=0,dependent=yes,alwaysRaised=yes');
              webWindow.opener = window;
              webWindow.focus();
              const checkLoaded = () => {
                if (webWindow.closed === true) {
                  this.props.setUser({
                    name: localStorage.getItem('musitUserName'),
                    accessToken: localStorage.getItem('musitAccessToken'),
                    email: localStorage.getItem('musitUserEmail'),
                    userId: localStorage.getItem('musitUserId')
                  });
                } else {
                  setTimeout(checkLoaded, 500);
                }
              };
              checkLoaded();
            }}
          >
              {translate('musit.login', true)}
          </span>
        </Button>
      )
    }
    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row styleClass="row-centered">
                <Col xs={10} md={10} style={{ textAlign: 'center' }}>
                  <PageHeader>
                    {translate('musit.welcomePage.title', true)}
                  </PageHeader>
                  <div className="panelWithBackground">
                    <span style={{ fontSize: 'larger', margin: '1em' }}>
                      {translate('musit.welcomePage.body', true)}
                    </span>
                  </div>
                </Col>
              </Row>
              <Row styleClass="row-centered">
                { /* <Col xs={10} md={10} style={{textAlign: "center"}}>
                  {translate("musit.welcomePage.noAccess", false)}
                </Col>*/ }
              </Row>
              {!this.props.user &&
                <Row styleClass="row-centered">
                  <Col xs={10} md={10} style={{ textAlign: 'center' }}>
                    {loginButton}
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
