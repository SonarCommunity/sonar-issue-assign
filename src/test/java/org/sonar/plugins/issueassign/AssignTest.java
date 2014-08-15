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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.plugins.issueassign.exception.SettingNotConfiguredException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AssignTest {

  @Mock
  private Settings mockSettings;
  @Mock
  private UserFinder mockUserFinder;
  @Mock
  private User overrideUser;
  @Mock
  private User scmAuthorUser;
  @Mock
  private User defaultUser;

  private static final String DEFAULT_ASSIGNEE = "defaultAssignee";
  private static final String OVERRIDE_ASSIGNEE = "overrideAssignee";
  private static final String SCM_AUTHOR = "scmAuthor";

  @Test
  public void testGetAssigneeWithScmAuthorAndOverride() throws Exception {

    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE))
      .thenReturn(OVERRIDE_ASSIGNEE);
    when(mockUserFinder.findByLogin(OVERRIDE_ASSIGNEE)).thenReturn(overrideUser);

    final Assign classUnderTest = new Assign(mockSettings, mockUserFinder);
    final User user = classUnderTest.getAssignee(SCM_AUTHOR);
    assertThat(user).isSameAs(overrideUser);
  }

  @Test
  public void testGetAssigneeWithScmAuthorAndNoOverride() throws Exception {
    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE))
      .thenThrow(SettingNotConfiguredException.class);
    when(mockUserFinder.findByLogin(SCM_AUTHOR)).thenReturn(scmAuthorUser);

    final Assign classUnderTest = new Assign(mockSettings, mockUserFinder);
    final User user = classUnderTest.getAssignee(SCM_AUTHOR);
    assertThat(user).isSameAs(scmAuthorUser);
  }

  @Test
  public void testGetAssigneeWithScmAuthorNotFound() throws Exception {
    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE))
      .thenThrow(SettingNotConfiguredException.class);
    when(mockUserFinder.findByLogin(SCM_AUTHOR)).thenReturn(null);

    // on to the default assignee
    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE))
      .thenReturn(DEFAULT_ASSIGNEE);
    when(mockUserFinder.findByLogin(DEFAULT_ASSIGNEE)).thenReturn(defaultUser);

    final Assign classUnderTest = new Assign(mockSettings, mockUserFinder);
    final User user = classUnderTest.getAssignee(SCM_AUTHOR);
    assertThat(user).isSameAs(defaultUser);
  }

  @Test
  public void testGetAssigneeWithoutScmAuthorAuthorAndOverride() throws Exception {

    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE))
      .thenReturn(OVERRIDE_ASSIGNEE);
    when(mockUserFinder.findByLogin(OVERRIDE_ASSIGNEE)).thenReturn(overrideUser);

    final Assign classUnderTest = new Assign(mockSettings, mockUserFinder);
    final User user = classUnderTest.getAssignee();
    assertThat(user).isSameAs(overrideUser);
  }

  @Test
  public void testGetAssigneeWithoutScmAuthorAndNoOverride() throws Exception {
    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE))
      .thenThrow(SettingNotConfiguredException.class);
    when(mockSettings.getString(IssueAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE))
      .thenReturn(DEFAULT_ASSIGNEE);
    when(mockUserFinder.findByLogin(DEFAULT_ASSIGNEE)).thenReturn(defaultUser);

    final Assign classUnderTest = new Assign(mockSettings, mockUserFinder);
    final User user = classUnderTest.getAssignee();
    assertThat(user).isSameAs(defaultUser);
  }
}
