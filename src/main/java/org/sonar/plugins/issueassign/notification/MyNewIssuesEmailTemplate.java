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
import org.sonar.api.config.EmailSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_NEW;

/**
 * Creates email message for notification "my-new-issues".
 */
public class MyNewIssuesEmailTemplate extends EmailTemplate {

  public static final String FIELD_PROJECT_NAME = "projectName";
  public static final String FIELD_PROJECT_KEY = "projectKey";
  public static final String FIELD_PROJECT_DATE = "projectDate";
  public static final String FIELD_ASSIGNEE = "assignee";

  private final EmailSettings settings;
  private final I18n i18n;

  public MyNewIssuesEmailTemplate(EmailSettings settings, I18n i18n) {
    this.settings = settings;
    this.i18n = i18n;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!NOTIFICATION_TYPE_NEW.equals(notification.getType())) {
      return null;
    }
    String projectName = notification.getFieldValue(FIELD_PROJECT_NAME);

    StringBuilder sb = new StringBuilder();
    sb.append("Project: ").append(projectName).append("\n\n");
    sb.append(notification.getFieldValue("count")).append(" new issues").append("\n\n");
    sb.append("   ");
    for (Iterator<String> severityIterator = Lists.reverse(Severity.ALL).iterator(); severityIterator.hasNext(); ) {
      String severity = severityIterator.next();
      String severityLabel = i18n.message(getLocale(), "severity." + severity, severity);
      sb.append(severityLabel).append(": ").append(notification.getFieldValue("count-" + severity));
      if (severityIterator.hasNext()) {
        sb.append("   ");
      }
    }
    sb.append('\n');

    appendFooter(sb, notification);

    return new EmailMessage()
        .setMessageId(NOTIFICATION_TYPE_NEW + "/" + notification.getFieldValue(FIELD_PROJECT_KEY) + "/" + notification.getFieldValue(FIELD_ASSIGNEE))
        .setSubject(projectName + ": new issues assigned to you")
        .setMessage(sb.toString());
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String projectKey = notification.getFieldValue(FIELD_PROJECT_KEY);
    String dateString = notification.getFieldValue(FIELD_PROJECT_DATE);
    String assignee = notification.getFieldValue(FIELD_ASSIGNEE);
    if (projectKey != null && dateString != null) {
      Date date = DateUtils.parseDateTime(dateString);
      String url = generateUrl(projectKey, assignee, date);
      sb.append("\n").append("See it in SonarQube: ").append(url).append("\n");
    }
  }

  private String generateUrl(String projectKey, String assignee, Date date) {
    return String.format("%s/issues/search#componentRoots=%s|createdAt=%s|assignees=%s",
        settings.getServerBaseURL(), encode(projectKey), encode(DateUtils.formatDateTime(date)), encode(assignee));
  }

  public static String encode(String toEncode) {
    try {
      return URLEncoder.encode(toEncode, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  private Locale getLocale() {
    return Locale.ENGLISH;
  }

}
