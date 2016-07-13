import 'react-select/dist/react-select.css'
import React, { Component, PropTypes } from 'react'
import { connect } from 'react-redux'
import { IndexLink } from 'react-router'
import { LinkContainer } from 'react-router-bootstrap'
import { Navbar, Nav, NavItem, Badge } from 'react-bootstrap'
import { routerActions } from 'react-router-redux'
import { I18n } from 'react-i18nify'
import FontAwesome from 'react-fontawesome'

const mapStateToProps = (state) => {
  I18n.loadTranslations(state.language.data)
  I18n.setLocale('no')
  return {
    user: state.auth.user,
    pushState: routerActions.push,
    pickListCount: state.picks.lists[state.picks.active].length
  }
}

@connect(mapStateToProps)
class App extends Component {
  static propTypes = {
    children: PropTypes.object.isRequired,
    user: PropTypes.object,
    pushState: PropTypes.func.isRequired,
    store: PropTypes.object,
    pickListCount: PropTypes.number.isRequired
  }

  static contextTypes = {
    store: PropTypes.object.isRequired,
    router: PropTypes.object.isRequired
  }

  componentWillReceiveProps(nextProps) {
    if (!this.props.user && nextProps.user) {
      this.context.router.push('/musit');
    } else if (this.props.user && !nextProps.user) {
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
    const { user, pickListCount } = this.props;
    const styles = require('./index.scss')
    const rootPath = user ? '/musit/' : '/'

    return (
      <div className={styles.app}>
        <Navbar fixedTop>
          <Navbar.Header>
            <Navbar.Brand>
              <IndexLink to={rootPath} activeStyle={{ color: '#33e0ff' }}>
                <div className={styles.brand}>
                  <img height="40" alt="logo" src="/assets/images/favicons/unimus_transparent100x100.png" />
                </div>
                <span>MUSIT</span>
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
                <LinkContainer to="/musit/survey">
                  <NavItem eventKey={4}>Survey</NavItem>
                </LinkContainer>
              }
              <LinkContainer to="/example">
                <NavItem eventKey={5}>Example</NavItem>
              </LinkContainer>
              <LinkContainer to="/observation">
                <NavItem eventKey={5}>Observation</NavItem>
              </LinkContainer>
              <LinkContainer to="/magasin">
                <NavItem eventKey={6}>Magasin</NavItem>
              </LinkContainer>
              <LinkContainer to="/musit/about">
                <NavItem eventKey={7}>About Us</NavItem>
              </LinkContainer>
              <LinkContainer to="/control/add">
                <NavItem eventKey={8}>ControlAdd</NavItem>
              </LinkContainer>
              <LinkContainer to="/control/1">
                <NavItem eventKey={9}>ControlView</NavItem>
              </LinkContainer>
              <LinkContainer to="/observationcontrol">
                <NavItem eventKey={13}>ObservatinControlLeftMenu</NavItem>
              </LinkContainer>
              {user &&
                <LinkContainer to="/picklist">
                  <NavItem><Badge><FontAwesome name="shopping-cart" /> {pickListCount}</Badge></NavItem>
                </LinkContainer>
              }
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
