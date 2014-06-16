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

import com.google.common.collect.Lists;
import org.apache.commons.lang.text.StrSubstitutor;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Parent template for my-new-issues and my-changed-issues notifications.
 */
public abstract class MyIssuesEmailTemplate extends EmailTemplate {

  public static final String FIELD_PROJECT_NAME = "projectName";
  public static final String FIELD_PROJECT_KEY = "projectKey";
  public static final String FIELD_PROJECT_DATE = "projectDate";
  public static final String FIELD_ASSIGNEE = "assignee";

  private final Settings settings;
  private final EmailSettings emailSettings;
  private final I18n i18n;
  private final String subjectTemplateSetting;
  private final String contentTemplateSetting;

  public MyIssuesEmailTemplate(Settings settings, EmailSettings emailSettings, I18n i18n, String subjectTemplateSetting, String contentTemplateSetting) {
    this.settings = settings;
    this.i18n = i18n;
    this.emailSettings = emailSettings;
    this.subjectTemplateSetting = subjectTemplateSetting;
    this.contentTemplateSetting = contentTemplateSetting;
  }

  protected abstract String getNotificationType();

  protected abstract String generateUrl(String projectKey, String assignee, Date date);

  @Override
  public EmailMessage format(Notification notification) {
    if (!getNotificationType().equals(notification.getType())) {
      return null;
    }
    String projectName = notification.getFieldValue(FIELD_PROJECT_NAME);

    String projectKey = notification.getFieldValue(FIELD_PROJECT_KEY);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    String assignee = notification.getFieldValue(FIELD_ASSIGNEE);
    if (projectKey == null || dateString == null || assignee == null) {
      return null;
    }
    Date date = DateUtils.parseDateTime(dateString);
    String url = generateUrl(projectKey, assignee, date);

    StringBuilder sb = new StringBuilder();
    for (Iterator<String> severityIterator = Lists.reverse(Severity.ALL).iterator(); severityIterator.hasNext(); ) {
      String severity = severityIterator.next();
      String severityLabel = i18n.message(getLocale(), "severity." + severity, severity);
      sb.append(severityLabel).append(": ").append(notification.getFieldValue("count-" + severity));
      if (severityIterator.hasNext()) {
        sb.append("   ");
      }
    }
    String countBySeverity = sb.toString();

    String count = notification.getFieldValue("count");

    Map<String, String> values = new HashMap<String, String>();
    values.put("projectName", projectName);
    values.put("date", dateString);
    values.put("count", count);
    values.put("countBySeverity", countBySeverity);
    values.put("url", url);

    String subject = StrSubstitutor.replace(settings.getString(subjectTemplateSetting), values);
    String content = StrSubstitutor.replace(settings.getString(contentTemplateSetting), values);

    return new EmailMessage()
        .setMessageId(getNotificationType() + "/" + notification.getFieldValue(FIELD_PROJECT_KEY) + "/" + notification.getFieldValue(FIELD_ASSIGNEE))
        .setSubject(subject)
        .setMessage(content);
  }

  protected String getServerBaseURL() {
    return emailSettings.getServerBaseURL();
  }

  private Locale getLocale() {
    return Locale.ENGLISH;
  }

}
