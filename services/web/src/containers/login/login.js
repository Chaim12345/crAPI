/*
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useState } from "react";
import { connect } from "react-redux";
import PropTypes from "prop-types";
import Login from "../../components/login/login";
import { logInUserAction } from "../../actions/userActions";
import { logInWithGoogleAction } from "../../actions/googleAuthActions";
import { crapienv } from "../../config";
import responseTypes from "../../constants/responseTypes";

const LoginContainer = (props) => {
  const { history, logInUser, logInWithGoogle } = props;

  const [hasErrored, setHasErrored] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const callback = (res, data) => {
    if (res === responseTypes.SUCCESS) {
      history.push("/dashboard");
    } else {
      setHasErrored(true);
      setErrorMessage(data);
    }
  };

  const onFinish = (values) => {
    logInUser({ ...values, callback });
  };

  const handleGoogleLogin = () => {
    if (!window.google || !window.google.accounts || !window.google.accounts.oauth2) {
      setHasErrored(true);
      setErrorMessage("Google Sign-In is not available");
      return;
    }

    const tokenClient = window.google.accounts.oauth2.initTokenClient({
      client_id: crapienv.GOOGLE_CLIENT_ID,
      scope: "openid email profile",
      callback: (response) => {
        if (response && response.access_token) {
          logInWithGoogle({ accessToken: response.access_token, callback });
        }
      },
    });

    tokenClient.requestAccessToken();
  };

  return (
    <Login
      hasErrored={hasErrored}
      errorMessage={errorMessage}
      onFinish={onFinish}
      onGoogleLogin={handleGoogleLogin}
      history={history}
    />
  );
};

const mapDispatchToProps = {
  logInUser: logInUserAction,
  logInWithGoogle: logInWithGoogleAction,
};

LoginContainer.propTypes = {
  logInUser: PropTypes.func,
  logInWithGoogle: PropTypes.func,
  history: PropTypes.object,
};

export default connect(null, mapDispatchToProps)(LoginContainer);
