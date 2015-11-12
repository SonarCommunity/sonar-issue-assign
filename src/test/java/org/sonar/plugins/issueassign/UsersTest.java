/*
 * SonarQube Issue Assign Plugin
 * Copyright (C) 2014 SonarSource
 * sonarqube@googlegroups.com
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.plugins.issueassign.exception.SonarUserNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersTest {

  @Mock
  UserFinder userFinder;
  @Mock
  User nonEmailUser;
  @Mock
  Settings settings;
  @Mock
  User emailUser;
  
  @InjectMocks
  Users testSubject;

  private static final String NON_EMAIL_USERNAME = "username";
  private static final String EMAIL_USERNAME = "username@domain.com";
  private static final String EMBEDDED_EMAIL_USERNAME = "UserName<username@domain.com>";
  private static final String NON_MATCHING_EMAIL = "dontmatch@domain.com";
  private List<User> sonarUsers;

  @Before
  public void before() {
    sonarUsers = new ArrayList<>();
    sonarUsers.add(emailUser);
    sonarUsers.add(nonEmailUser);
  }

  @Test
  public void findSonarUser() throws Exception {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);
    when(userFinder.findByLogin(NON_EMAIL_USERNAME)).thenReturn(nonEmailUser);

    final User user = testSubject.getSonarUser(NON_EMAIL_USERNAME);
    assertThat(user).isSameAs(nonEmailUser);
  }

  @Test(expected = SonarUserNotFoundException.class)
  public void sonarUserNotFoundAnywhere() throws Exception {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);
    when(userFinder.findByLogin(NON_EMAIL_USERNAME)).thenReturn(null);

    testSubject.getSonarUser(NON_EMAIL_USERNAME);
  }

  @Test
  public void findSonarUserAsEmailAddress() throws SonarUserNotFoundException {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);
    when(userFinder.findByLogin(EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(EMAIL_USERNAME);
    when(nonEmailUser.email()).thenReturn(null);

    final User user = testSubject.getSonarUser(EMAIL_USERNAME);

    assertThat(user).isSameAs(this.emailUser);
  }

  @Test
  public void findSonarUserAsEmbeddedEmailAddress() throws SonarUserNotFoundException {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);
    when(userFinder.findByLogin(EMBEDDED_EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(EMAIL_USERNAME);
    when(nonEmailUser.email()).thenReturn(null);

    final User user = testSubject.getSonarUser(EMBEDDED_EMAIL_USERNAME);

    assertThat(user).isSameAs(this.emailUser);
  }

  @Test
  public void findSonarUserAsEmailAddressTwiceToTestCache() throws SonarUserNotFoundException {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);
    when(userFinder.findByLogin(EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(EMAIL_USERNAME);
    when(nonEmailUser.email()).thenReturn(null);

    User user = testSubject.getSonarUser(EMAIL_USERNAME);
    assertThat(user).isSameAs(this.emailUser);

    user = testSubject.getSonarUser(EMAIL_USERNAME);
    assertThat(user).isSameAs(this.emailUser);
  }

  @Test(expected = SonarUserNotFoundException.class)
  public void findSonarUserAsEmailAddressNotFound() throws SonarUserNotFoundException {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);
    when(userFinder.findByLogin(EMAIL_USERNAME)).thenReturn(null);
    when(userFinder.find(isA(UserQuery.class))).thenReturn(this.sonarUsers);
    when(emailUser.email()).thenReturn(NON_MATCHING_EMAIL);
    when(nonEmailUser.email()).thenReturn(null);

    final Users testSubject = new Users(userFinder, settings);
    testSubject.getSonarUser(EMAIL_USERNAME);
  }

  @Test
  public void testHasEmbeddedEmailAddress() {
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(null);

    String userName = "UserName<user@company.com>";
    boolean result = testSubject.hasEmbeddedEmailAddress(userName);
    assertThat(result).isEqualTo(true);

    userName = "<user@company.com>";
    result = testSubject.hasEmbeddedEmailAddress(userName);
    assertThat(result).isEqualTo(true);

    userName = "UserName";
    result = testSubject.hasEmbeddedEmailAddress(userName);
    assertThat(result).isEqualTo(false);
  }

  @Test
  public void extractSonarUserFromScmUser() throws SonarUserNotFoundException {
    final String scmUserName = "joe.blow.123456";

    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(".*\\..*\\.(\\d{6})");
    when(userFinder.findByLogin("123456")).thenReturn(this.nonEmailUser);

    final User user = this.testSubject.getSonarUser(scmUserName);
    assertThat(user).isEqualTo(this.nonEmailUser);
  }

  @Test(expected = SonarUserNotFoundException.class)
  public void extractSonarUserFromScmUserButRegexFails() throws SonarUserNotFoundException {
    final String scmUserName = "nobody";
    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(".*\\..*\\.(\\d{6})");

    this.testSubject.getSonarUser(scmUserName);
  }

  @Test(expected = SonarUserNotFoundException.class)
  public void extractSonarUserFromScmUserButDoesntExistInSonar() throws SonarUserNotFoundException {
    final String scmUserName = "joe.blow.123456";

    when(settings.getString(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)).thenReturn(".*\\..*\\.(\\d{6})");
    when(userFinder.findByLogin("123456")).thenReturn(null);

    this.testSubject.getSonarUser(scmUserName);
  }
}
