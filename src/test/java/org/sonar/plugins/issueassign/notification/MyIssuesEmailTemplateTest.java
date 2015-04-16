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
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import java.util.Date;
import java.util.Locale;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MyIssuesEmailTemplateTest {

  private static final String NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
  private static final String PROPERTY_SUBJECT = "PROPERTY_SUBJECT";
  private static final String PROPERTY_CONTENT = "PROPERTY_CONTENT";

  MyIssuesEmailTemplate template;

  @Mock
  I18n i18n;

  @Mock
  Settings settings;

  @Before
  public void setUp() {
    EmailSettings emailSettings = mock(EmailSettings.class);
    when(emailSettings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new MyIssuesEmailTemplate(settings, emailSettings, i18n, PROPERTY_SUBJECT, PROPERTY_CONTENT) {

      @Override
      protected String getNotificationType() {
        return NOTIFICATION_TYPE;
      }

      @Override
      protected String generateUrl(String projectKey, String assignee, Date date) {
        return "NOTIFICATION_URL|" + projectKey + "|" + assignee + "|" + DateUtils.formatDateTime(date);
      }
    };
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  /**
   * <pre>
   * Subject: Project Struts, new issues
   * From: Sonar
   *
   * Project: Foo
   * 32 new issues
   *
   * See it in SonarQube: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations
   * </pre>
   */
  @Test
  public void shouldFormatCommentAdded() {
    Notification notification = new Notification(NOTIFICATION_TYPE)
      .setFieldValue("count", "32")
      .setFieldValue("count-INFO", "1")
      .setFieldValue("count-MINOR", "3")
      .setFieldValue("count-MAJOR", "10")
      .setFieldValue("count-CRITICAL", "5")
      .setFieldValue("count-BLOCKER", "0")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectDate", "2010-05-18T14:50:45+0000")
      .setFieldValue("assignee", "user1");

    when(i18n.message(any(Locale.class), eq("severity.BLOCKER"), anyString())).thenReturn("Blocker");
    when(i18n.message(any(Locale.class), eq("severity.CRITICAL"), anyString())).thenReturn("Critical");
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), anyString())).thenReturn("Major");
    when(i18n.message(any(Locale.class), eq("severity.MINOR"), anyString())).thenReturn("Minor");
    when(i18n.message(any(Locale.class), eq("severity.INFO"), anyString())).thenReturn("Info");
    when(settings.getString(PROPERTY_SUBJECT)).thenReturn("${projectName}|${count}");
    when(settings.getString(PROPERTY_CONTENT)).thenReturn("${projectName}|${count}|${countBySeverity}|${date}|${url}");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo(NOTIFICATION_TYPE + "/org.apache:struts/user1");
    assertThat(message.getSubject()).isEqualTo("Struts|32");

    assertThat(message.getMessage()).startsWith(
      "Struts|32|Blocker: 0   Critical: 5   Major: 10   Minor: 3   Info: 1|2010-05-18T14:50:45+0000|NOTIFICATION_URL|org.apache:struts|user1|2010-05-1");
  }

  @Test
  public void shouldNotAddFooterIfMissingProperties() {
    Notification notification = new Notification(NOTIFICATION_TYPE)
      .setFieldValue("count", "32")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("assignee", "user1");

    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }
}
