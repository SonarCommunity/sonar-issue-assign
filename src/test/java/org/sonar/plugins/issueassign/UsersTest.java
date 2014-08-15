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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.plugins.issueassign.exception.SonarUserNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class UsersTest {

  @Mock
  UserFinder userFinder;
  @Mock
  User nonEmailUser;
  @Mock
  User emailUser;

  private static final String NON_EMAIL_USERNAME = "username";
  private static final String EMAIL_USERNAME = "username@domain.com";
  private static final String NON_MATCHING_EMAIL = "dontmatch@domain.com";
  private List<User> sonarUsers;

  @Before
  public void before() {
    sonarUsers = new ArrayList<User>();
    sonarUsers.add(emailUser);
    sonarUsers.add(nonEmailUser);
  }

  @Test
  public void findSonarUser() throws Exception {
    when(userFinder.findByLogin(NON_EMAIL_USERNAME)).thenReturn(nonEmailUser);

    final Users classUnderTest = new Users(userFinder);
    final User user = classUnderTest.getSonarUser(NON_EMAIL_USERNAME);
    assertThat(user).isSameAs(nonEmailUser);
  }

  @Test(expected = SonarUserNotFoundException.class)
  public void sonarUserNotFoundAnywhere() throws Exception {
    when(userFinder.findByLogin(NON_EMAIL_USERNAME)).thenReturn(null);

    final Users classUnderTest = new Users(userFinder);
    classUnderTest.getSonarUser(NON_EMAIL_USERNAME);
  }

  @Test
  public void findSonarUserAsEmailAddress() throws SonarUserNotFoundException {
    when(userFinder.findByLogin(EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(EMAIL_USERNAME);
    when(nonEmailUser.email()).thenReturn(null);

    final Users classUnderTest = new Users(userFinder);
    final User user = classUnderTest.getSonarUser(EMAIL_USERNAME);

    assertThat(user).isSameAs(this.emailUser);
  }

  @Test
  public void findSonarUserAsEmailAddressTwiceToTestCache() throws SonarUserNotFoundException {
    when(userFinder.findByLogin(EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(EMAIL_USERNAME);
    when(nonEmailUser.email()).thenReturn(null);

    final Users classUnderTest = new Users(userFinder);
    User user = classUnderTest.getSonarUser(EMAIL_USERNAME);
    assertThat(user).isSameAs(this.emailUser);

    user = classUnderTest.getSonarUser(EMAIL_USERNAME);
    assertThat(user).isSameAs(this.emailUser);
  }

  @Test(expected = SonarUserNotFoundException.class)
  public void findSonarUserAsEmailAddressNotFound() throws SonarUserNotFoundException {
    when(userFinder.findByLogin(EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(NON_MATCHING_EMAIL);
    when(nonEmailUser.email()).thenReturn(null);

    final Users classUnderTest = new Users(userFinder);
    classUnderTest.getSonarUser(EMAIL_USERNAME);
  }
}
