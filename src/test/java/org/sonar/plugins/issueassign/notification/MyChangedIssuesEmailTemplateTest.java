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
package org.sonar.plugins.issueassign.notification;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.DateUtils;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MyChangedIssuesEmailTemplateTest {

  MyChangedIssuesEmailTemplate template;

  @Mock
  I18n i18n;

  @Before
  public void setUp() {
    Settings settings = mock(Settings.class);
    when(settings.getString(anyString())).thenReturn(null);
    EmailSettings emailSettings = mock(EmailSettings.class);
    when(emailSettings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new MyChangedIssuesEmailTemplate(settings, emailSettings, i18n);
  }

  @Test
  public void hasCorrectNotificationName() {
    String notificationName = template.getNotificationName();
    assertThat(notificationName).isEqualTo("changed issues");
  }

  @Test
  public void hasCorrectNotificationType() {
    String notificationType = template.getNotificationType();
    assertThat(notificationType).isEqualTo("my-changed-issues");
  }

  @Test
  public void hasCorrectUrl() {
    Date date = DateUtils.parseDateTime("2010-05-18T14:50:45+0000");
    String url = template.generateUrl("org.apache:struts", "user1", date);
    assertThat(url).isEqualTo("http://nemo.sonarsource.org/issues/search#componentRoots=org.apache%3Astruts|assignees=user1|sort=UPDATE_DATE|asc=false");
  }

}
