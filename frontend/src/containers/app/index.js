import 'react-select/dist/react-select.css'
import React, { Component, PropTypes } from 'react'
import { connect } from 'react-redux'
import { IndexLink } from 'react-router'
import { LinkContainer } from 'react-router-bootstrap'
import { Navbar, Nav, NavItem, Badge } from 'react-bootstrap'
import { routerActions } from 'react-router-redux'
import { I18n } from 'react-i18nify'
import FontAwesome from 'react-fontawesome'
import { connectUser, clearUser } from '../../reducers/auth';
import LoginButton from '../../components/login-button'

const mapStateToProps = (state) => {
  I18n.loadTranslations(state.language.data)
  I18n.setLocale('no')
  return {
    user: state.auth.user,
    pushState: routerActions.push,
    pickListCount: state.picks.lists[state.picks.active].length
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    clearUser: () => dispatch(clearUser()),
    setUser: (user) => dispatch(connectUser(user))
  }
}

@connect(mapStateToProps, mapDispatchToProps)
class App extends Component {
  static propTypes = {
    children: PropTypes.object.isRequired,
    user: PropTypes.object,
    pushState: PropTypes.func.isRequired,
    store: PropTypes.object,
    pickListCount: PropTypes.number.isRequired,
    clearUser: PropTypes.func.isRequired,
    setUser: PropTypes.func.isRequired
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
    this.props.clearUser()
  }

  handleLogin = (event) => {
    event.preventDefault()
    new LoginButton().doLogin(this.props.setUser)
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
          <Navbar.Collapse>
            <Nav navbar>
              {user &&
                <LinkContainer to="/magasin">
                  <NavItem>Magasin</NavItem>
                </LinkContainer>
              }
              {user &&
                <LinkContainer to="/picklist">
                  <NavItem><Badge><FontAwesome name="shopping-cart" /> {pickListCount}</Badge></NavItem>
                </LinkContainer>
              }
              {!user &&
                <LinkContainer to="/musit/login">
                  <NavItem className="login-link" onClick={this.handleLogin}>Login</NavItem>
                </LinkContainer>
              }
              {user &&
                <LinkContainer to="/musit/logout">
                  <NavItem className="logout-link" onClick={this.handleLogout}>Logout</NavItem>
                </LinkContainer>
              }
            </Nav>
            {user &&
              <p className={`${styles.loggedInMessage} navbar-text`}>Logged in as <strong>{user.name}</strong>.</p>}
            <Nav navbar pullRight>
              <NavItem
                target="_blank"
                rel="noopener noreferrer"
                title="View on Github"
                href="https://github.com/MUSIT-Norway/musit"
              >
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
            rel="noopener noreferrer"
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
