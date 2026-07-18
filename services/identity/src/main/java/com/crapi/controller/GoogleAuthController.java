/*
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

package com.crapi.controller;

import com.crapi.config.JwtProvider;
import com.crapi.entity.User;
import com.crapi.model.GoogleTokenRequest;
import com.crapi.model.JwtResponse;
import com.crapi.service.GoogleOAuth2Service;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/identity/api/auth")
public class GoogleAuthController {

  @Autowired GoogleOAuth2Service googleOAuth2Service;

  @Autowired JwtProvider jwtProvider;

  @PostMapping("/google")
  public ResponseEntity<JwtResponse> authenticateWithGoogle(
      @Valid @RequestBody GoogleTokenRequest tokenRequest) {
    User user = googleOAuth2Service.authenticateWithGoogle(tokenRequest);
    if (user != null) {
      String jwt = jwtProvider.generateJwtToken(user);
      return ResponseEntity.status(HttpStatus.OK).body(new JwtResponse(jwt));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new JwtResponse("", "Google authentication failed"));
  }
}
