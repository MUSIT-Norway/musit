import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { IndexLink } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';
import { Navbar, Nav, NavItem } from 'react-bootstrap';
import Helmet from 'react-helmet';
import { isLoaded as isInfoLoaded, load as loadInfo } from '../../reducers/info';
import { isLoaded as isAuthLoaded, load as loadAuth, logout } from '../../reducers/auth';
import InfoBar from '../../components/info-bar';
import { routerActions } from 'react-router-redux';
import config from '../../config';
import { asyncConnect } from 'redux-async-connect';

const mapStateToProps = (state) => {
  return {
    user: state.auth.user,
    logout: logout,
    pushState: routerActions.push
  }
}

@asyncConnect([{
  promise: ({store: {dispatch, getState}}) => {
    const promises = [];

    if (!isInfoLoaded(getState())) {
      promises.push(dispatch(loadInfo()));
    }
    if (!isAuthLoaded(getState())) {
      promises.push(dispatch(loadAuth()));
    }

    return Promise.all(promises);
  }
}])
@connect(mapStateToProps)
class App extends Component {
  static propTypes = {
    children: PropTypes.object.isRequired,
    user: PropTypes.object,
    logout: PropTypes.func.isRequired,
    pushState: PropTypes.func.isRequired
  };

  static contextTypes = {
    store: PropTypes.object.isRequired
  };

  componentWillReceiveProps(nextProps) {
    if (!this.props.user && nextProps.user) {
      // login
      this.props.pushState('/loginSuccess');
    } else if (this.props.user && !nextProps.user) {
      // logout
      this.props.pushState('/');
    }
  }

  handleLogout = (event) => {
    event.preventDefault();
    this.props.logout();
  };

  render() {
    const {user} = this.props;
    const styles = require('./index.scss');

    return (
     <div className={styles.app}>


        <Helmet {...config.app.head}/>
        <Navbar fixedTop>
          <Navbar.Header>
            <Navbar.Brand>
              <IndexLink to="/" activeStyle={{color: '#33e0ff'}}>
                <div className={styles.brand}><img height="40" src="kulturminne.png" /></div><span>{config.app.title}</span>
              </IndexLink>
            </Navbar.Brand>
            <Navbar.Toggle/>
          </Navbar.Header>

          <Navbar.Collapse eventKey={0}>
            <Nav navbar>
              {user &&
                  <LinkContainer to="/chat">
                    <NavItem eventKey={1}>Chat</NavItem>
                  </LinkContainer>
              }
              {user &&
                  <LinkContainer to="/widgets">
                    <NavItem eventKey={2}>Widgets</NavItem>
                  </LinkContainer>
              }
              {user &&
                  <LinkContainer to="/magasin">
                  <NavItem eventKey={3}>Magasin</NavItem>
                  </LinkContainer>
              }
             {user &&
                  <LinkContainer to="/survey">
                    <NavItem eventKey={4}>Survey</NavItem>
                  </LinkContainer>
              }
              <LinkContainer to="/about">
                <NavItem eventKey={5}>About Us</NavItem>
              </LinkContainer>
              {user &&
                  <LinkContainer to="/logout">
                    <NavItem eventKey={6} className="logout-link" onClick={this.handleLogout}>Logout</NavItem>
                  </LinkContainer>
              }
              {!user &&
                  <LinkContainer to="/welcomeUser">
                    <NavItem eventKey={7}>Login</NavItem>
                  </LinkContainer>
              }
            </Nav>
            {user &&
            <p className={styles.loggedInMessage + ' navbar-text'}>Logged in as <strong>{user.name}</strong>.</p>}
            <Nav navbar pullRight>
              <NavItem eventKey={1} target="_blank" title="View on Github" href="https://github.com/MUSIT-Norway/musit">
                <i className="fa fa-github"/>
              </NavItem>
            </Nav>
          </Navbar.Collapse>
        </Navbar>

        <div className={styles.appContent}>
          {this.props.children}
        </div>
       { null && <InfoBar/>}

        <div className="well text-center">
          Have questions? Ask for help <a
          href="https://github.com/MUSIT-Norway/musit/issues"
          target="_blank">on Github</a>
        </div>
      </div>
    );
  }
}

export default App