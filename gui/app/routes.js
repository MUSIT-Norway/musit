import React from 'react';
import {IndexRoute, Route} from 'react-router';
import { isLoaded as isAuthLoaded, load as loadAuth } from 'redux/modules/auth';
import MainIndex from './components/main_index'
import NotFound from './components/NotFound'
import {WelcomeView, ExampleView} from './containers'

export default (store) => {
  const requireLogin = (nextState, replace, cb) => {
    function checkAuth() {
      const { auth: { user }} = store.getState();
      if (!user) {
        // oops, not logged in, so can't be here!
        replace('/');
      }
      cb();
    }

    if (!isAuthLoaded(store.getState())) {
      store.dispatch(loadAuth()).then(checkAuth);
    } else {
      checkAuth();
    }
  };

  /**
   * Please keep routes in alphabetical order
   */
  return (
    <Route path="/" component={MainIndex}>
        <IndexRoute component={WelcomeView} />
        <Route path="example" component={ExampleView} />

        <Route path="*" component={NotFound} status={404} />
    </Route>

    {/*
    <Route path="/" component={App}>
      <IndexRoute component={Home}/>

      -- Routes requiring login
      <Route onEnter={requireLogin}>
        <Route path="chat" component={Chat}/>
        <Route path="loginSuccess" component={LoginSuccess}/>
      </Route>

      -- Routes
      <Route path="about" component={About}/>
      <Route path="login" component={Login}/>
      <Route path="survey" component={Survey}/>
      <Route path="widgets" component={Widgets}/>

      -- Catch all route
      <Route path="*" component={NotFound} status={404} />
    </Route>
*/}
  );
};