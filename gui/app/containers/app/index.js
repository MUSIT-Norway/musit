import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { IndexLink } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';
import { Navbar, Nav, NavItem } from 'react-bootstrap';
import Helmet from 'react-helmet';
import { isLoaded as isInfoLoaded, load as loadInfo } from '../../reducers/info';
import { isLoaded as isLanguageLoaded, load as loadLanguage } from '../../reducers/language';
import { isLoaded as isFakeAuthInfoLoaded, load as loadFakeAuthInfo } from '../../reducers/fake-auth-info';
import InfoBar from '../../components/info-bar';
import { routerActions } from 'react-router-redux';
import config from '../../config';
import { asyncConnect } from 'redux-async-connect';
import { I18n } from 'react-i18nify'

const mapStateToProps = (state) => {
  I18n.loadTranslations(state.language.data)
  I18n.setLocale('no')
  return {
    user: state.auth.user,
    pushState: routerActions.push
  }
}

@asyncConnect([{
  promise: ({ store: { dispatch, getState } }) => {
    const promises = [];

    if (!isInfoLoaded(getState())) {
      promises.push(dispatch(loadInfo()));
    }
    if (!isLanguageLoaded(getState())) {
      promises.push(dispatch(loadLanguage()));
    }
    if ((config.FAKE_STRATEGY === config.dataportenClientSecret) && !isFakeAuthInfoLoaded(getState())) {
      promises.push(dispatch(loadFakeAuthInfo()));
    }
    return Promise.all(promises);
  }
}])

@connect(mapStateToProps)
class App extends Component {
  static propTypes = {
    children: PropTypes.object.isRequired,
    user: PropTypes.object,
    pushState: PropTypes.func.isRequired,
    store: PropTypes.object
  };

  static contextTypes = {
    store: PropTypes.object.isRequired,
    router: PropTypes.object.isRequired
  };

  componentWillReceiveProps(nextProps) {
    if (!this.props.user && nextProps.user) {
      // login
      this.context.router.push('/musit/welcomeUser');
    } else if (this.props.user && !nextProps.user) {
      // logout
      this.context.router.push('/');
    }
  }

  handleLogout = (event) => {
    event.preventDefault()
    // this.props.dispatch(this.props.logout())
  }

  handleFakeLogin = (event) => {
    event.preventDefault()
    // this.props.dispatch(this.props.login('fake'))
  }

  render() {
    const { user } = this.props;
    const styles = require('./index.scss');
    const rootPath = user ? '/musit/' : '/'

    return (
      <div className={styles.app}>
        <Helmet {...config.app.head} />
        <Navbar fixedTop>
          <Navbar.Header>
            <Navbar.Brand>
              <IndexLink to={rootPath} activeStyle={{ color: '#33e0ff' }}>
                <div className={styles.brand}><img height="40" src="favicons/unimus_transparent100x100.png" /></div>
                <span>{config.app.title}</span>
              </IndexLink>
            </Navbar.Brand>
            <Navbar.Toggle />
          </Navbar.Header>
          <Navbar.Collapse eventKey={0}>
            <Nav navbar>
              {user &&
              <LinkContainer to="/musit/chat">
                <NavItem eventKey={1}>Chat</NavItem>
              </LinkContainer>
              }
              {user &&
              <LinkContainer to="/musit/widgets">
                <NavItem eventKey={2}>Widgets</NavItem>
              </LinkContainer>
              }
              {user &&
              <LinkContainer to="/musit/magasin">
                <NavItem eventKey={3}>Magasin</NavItem>
              </LinkContainer>
              }
              {user &&
              <LinkContainer to="/musit/survey">
                <NavItem eventKey={4}>Survey</NavItem>
              </LinkContainer>
              }
              <LinkContainer to="/example">
                <NavItem eventKey={5}>Example</NavItem>
              </LinkContainer>
              <LinkContainer to="/storageunits">
                <NavItem eventKey={6}>Dummy list</NavItem>
              </LinkContainer>
              <LinkContainer to="/musit/about">
                <NavItem eventKey={7}>About Us</NavItem>
              </LinkContainer>
              {user &&
              <LinkContainer to="/musit/logout">
                <NavItem eventKey={8} className="logout-link" onClick={this.handleLogout}>Logout</NavItem>
              </LinkContainer>
              }
              {!user &&
              <LinkContainer to="/musit/">
                <NavItem onClick={this.handleFakeLogin} eventKey={7}>Login</NavItem>
              </LinkContainer>
              }
            </Nav>
            {user &&
            <p className={`${styles.loggedInMessage} navbar-text`}>Logged in as <strong>{user.name}</strong>.</p>}
            <Nav navbar pullRight>
              <NavItem eventKey={1} target="_blank" title="View on Github" href="https://github.com/MUSIT-Norway/musit">
                <i className="fa fa-github" />
              </NavItem>
            </Nav>
          </Navbar.Collapse>
        </Navbar>
        <div className={styles.appContent}>
          {this.props.children}
        </div>
        {null && <InfoBar />}
        <div className="well text-center">
          Have questions? Ask for help<a
            href="https://github.com/MUSIT-Norway/musit/issues"
            target="_blank"
          >
            on Github
          </a>
        </div>
      </div>
    );
  }
}

export default App
