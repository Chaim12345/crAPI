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

package com.crapi.service.Impl;

import com.crapi.config.JwtProvider;
import com.crapi.constant.UserMessage;
import com.crapi.entity.User;
import com.crapi.entity.UserDetails;
import com.crapi.enums.ERole;
import com.crapi.model.CRAPIResponse;
import com.crapi.model.GoogleTokenRequest;
import com.crapi.repository.UserDetailsRepository;
import com.crapi.repository.UserRepository;
import com.crapi.service.GoogleOAuth2Service;
import com.crapi.utils.SMTPMailServer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleOAuth2ServiceImpl implements GoogleOAuth2Service {
  private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2ServiceImpl.class);
  private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

  @Autowired UserRepository userRepository;

  @Autowired UserDetailsRepository userDetailsRepository;

  @Autowired PasswordEncoder encoder;

  @Autowired JwtProvider jwtProvider;

  @Autowired SMTPMailServer smtpMailServer;

  @Transactional
  @Override
  public User authenticateWithGoogle(GoogleTokenRequest tokenRequest) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + tokenRequest.getAccessToken());
    HttpEntity<String> entity = new HttpEntity<>("", headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(GOOGLE_USERINFO_URL, HttpMethod.GET, entity, String.class);
      JsonObject root = JsonParser.parseString(response.getBody()).getAsJsonObject();

      String email = root.get("email").getAsString();
      String name = root.get("name").getAsString();
      String sub = root.get("sub").getAsString();

      User user = userRepository.findByEmail(email);
      if (user == null) {
        user = new User();
        user.setEmail(email);
        user.setNumber("google_" + sub);
        user.setPassword(encoder.encode(UUID.randomUUID().toString()));
        user.setRole(ERole.ROLE_USER);
        user = userRepository.save(user);

        UserDetails userDetails = new UserDetails();
        userDetails.setName(name);
        userDetails.setUser(user);
        userDetails.setAvailable_credit(100.0);
        userDetails.setStatus("ACTIVE");
        userDetailsRepository.save(userDetails);
        logger.info("New Google user registered: {}", email);
      }

      return user;
    } catch (Exception e) {
      logger.error("Google OAuth authentication failed: {}", e.getMessage());
      return null;
    }
  }
}
