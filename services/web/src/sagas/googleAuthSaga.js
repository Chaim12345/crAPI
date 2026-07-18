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

import { put, takeLatest } from "redux-saga/effects";
import { APIService, requestURLS } from "../constants/APIConstant";
import actionTypes from "../constants/actionTypes";
import responseTypes from "../constants/responseTypes";
import { INVALID_CREDS } from "../constants/messages";

export function* logInWithGoogle(param) {
  const { accessToken, callback } = param;
  let recievedResponse = {};
  try {
    yield put({ type: actionTypes.FETCHING_DATA });

    const postUrl = APIService.JAVA_MICRO_SERVICES + "api/auth/google";
    const headers = {
      "Content-Type": "application/json",
    };

    const responseJSON = yield fetch(postUrl, {
      headers,
      method: "POST",
      body: JSON.stringify({ access_token: accessToken }),
    }).then((response) => {
      recievedResponse = response;
      if (recievedResponse.ok) return response.json();
      return response;
    });

    if (!recievedResponse.ok) {
      throw responseJSON;
    }

    const getUrl = APIService.JAVA_MICRO_SERVICES + requestURLS.GET_USER;
    const authHeaders = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${responseJSON.token}`,
    };

    const userResponseJSON = yield fetch(getUrl, {
      headers: authHeaders,
      method: "GET",
    }).then((response) => {
      recievedResponse = response;
      return response.json();
    });

    if (!recievedResponse.ok) {
      throw responseJSON;
    }

    yield put({
      type: actionTypes.LOGGED_IN,
      payload: { token: responseJSON.token, user: userResponseJSON },
    });
    callback(responseTypes.SUCCESS, responseJSON.data);
  } catch (e) {
    yield put({ type: actionTypes.FETCHED_DATA, payload: recievedResponse });
    callback(responseTypes.FAILURE, INVALID_CREDS);
  }
}

export function* googleAuthActionWatcher() {
  yield takeLatest(actionTypes.LOG_IN_WITH_GOOGLE, logInWithGoogle);
}
