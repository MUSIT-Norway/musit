import React from 'react';
import {IndexRoute, Route} from 'react-router';
import { isLoaded as isAuthLoaded, load as loadAuth } from './helpers/auth';
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

        -- Authentication routes
        {/*
        <Route onEnter={requireLogin}>
          <Route path="chat" component={Chat}/>
          <Route path="loginSuccess" component={LoginSuccess}/>
        </Route>
        */}

        -- Routes
        <Route path="example" component={ExampleView} />

        -- Catch all route
        <Route path="*" component={NotFound} status={404} />
    </Route>
  );
};