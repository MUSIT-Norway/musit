const styles = require('./WelcomeContainer.scss');

import React from 'react'
import { Panel, Grid, Row, Col, PageHeader } from 'react-bootstrap'
import LoginButton from '../../components/login-button'

export default class WelcomeContainer extends React.Component {
  static propTypes = {
    translate: React.PropTypes.func.isRequired,
    user: React.PropTypes.object,
    setUser: React.PropTypes.func.isRequired,
  };

  render() {
    return (
      <div>
        <main>
          <Panel>
            <Grid>
              <Row className="row-centered">
                <Col xs={10} md={10} style={{ textAlign: 'center' }}>
                  <PageHeader>
                    {this.props.translate('musit.welcomePage.title', true)}
                  </PageHeader>
                  <div className={styles.panelWithBackground}>
                    <span style={{ fontSize: 'larger', margin: '1em' }}>
                      {this.props.translate('musit.welcomePage.body', true)}
                    </span>
                  </div>
                </Col>
              </Row>
              {!this.props.user &&
                <Row className="row-centered">
                  <Col xs={10} md={10} style={{ textAlign: 'center' }}>
                    <LoginButton
                      setUser={this.props.setUser}
                    >
                      {this.props.translate('musit.login', true)}
                    </LoginButton>
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
