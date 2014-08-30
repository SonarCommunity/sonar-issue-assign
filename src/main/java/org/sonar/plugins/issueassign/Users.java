/*
 * SonarQube Issue Assign Plugin
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.issueassign;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.plugins.issueassign.exception.SonarUserNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Users {

  private static final Logger LOG = LoggerFactory.getLogger(Users.class);
  private static final Pattern EMBEDDED_EMAIL_PATTERN = Pattern.compile(".*<.*@.*>.*");
  private final UserFinder userFinder;
  private Map<String, User> emailToUserMap;

  public Users(final UserFinder userFinder) {
    this.userFinder = userFinder;
  }

  public User getSonarUser(final String userName) throws SonarUserNotFoundException {

    final User sonarUser = this.userFinder.findByLogin(userName);

    if (sonarUser == null) {
      if (isEmailAddress(userName)) {
        String emailAddress;
        if (hasEmbeddedEmailAddress(userName)) {
          LOG.debug("Username {} contains an embedded email address.", userName);
          emailAddress = extractEmail(userName);
          LOG.debug("Extracted email address: {}", emailAddress);
        } else {
          emailAddress = userName;
        }

        LOG.debug("SCM author is an email address, trying lookup by email...");
        return this.getSonarUserByEmail(emailAddress);
      }
      throw new SonarUserNotFoundException();
    }

    LOG.debug("Found Sonar user: " + sonarUser.login());
    return sonarUser;
  }

  // a cheap solution, but may be enough.
  private boolean isEmailAddress(final String userName) {
    return userName.contains("@");
  }

  // check for pattern like UserName<user@company.com>
  protected boolean hasEmbeddedEmailAddress(final String userName) {
    final Matcher matcher = EMBEDDED_EMAIL_PATTERN.matcher(userName);
    return matcher.find();
  }

  private String extractEmail(final String userName) {
    String tempUserName = userName;
    tempUserName = tempUserName.substring(tempUserName.indexOf("<") + 1);
    tempUserName = tempUserName.substring(0, tempUserName.indexOf(">"));
    return tempUserName;
  }

  private User getSonarUserByEmail(final String email) throws SonarUserNotFoundException {
    if (this.emailToUserMap == null) {
      this.initialiseUserMap();
    }

    final User user = this.emailToUserMap.get(email);
    if (user == null) {
      throw new SonarUserNotFoundException();
    }
    return user;
  }

  private void initialiseUserMap() {
    this.emailToUserMap = new HashMap<String, User>();
    final List<User> sonarUsers = this.getAllSonarUsers();

    for (final User user : sonarUsers) {
      final String email = user.email();
      if (StringUtils.isNotEmpty(email)) {
        LOG.debug("Caching user [" + user.login() + "] with email [" + email + "].");
        this.emailToUserMap.put(email, user);
      }
    }
  }

  private List<User> getAllSonarUsers() {
    final UserQuery userQuery = UserQuery.builder().build();
    return this.userFinder.find(userQuery);
  }
}
