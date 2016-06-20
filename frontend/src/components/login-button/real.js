import React from 'react';
import { Button } from 'react-bootstrap';

export default class FeideButton extends React.Component {
  static propTypes = {
    children: React.PropTypes.element,
    setUser: React.PropTypes.func.isRequired,
  }

  render() {
    return (
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
            if (!webWindow || webWindow.closed || typeof webWindow.closed === 'undefined') {
              /* eslint-disable no-alert */
              alert('Enable popups for login to work properly');
            }
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
            {this.props.children}
        </span>
      </Button>
    )
  }
}
