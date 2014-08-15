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

import static org.sonar.plugins.issueassign.IssueAssignPlugin.*;
import static org.sonar.plugins.issueassign.util.PluginUtils.urlEncode;

import java.util.Date;

import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.DateUtils;

/**
 * Creates email message for notification "my-new-issues".
 */
public class MyNewIssuesEmailTemplate extends MyIssuesEmailTemplate {

  public MyNewIssuesEmailTemplate(Settings settings, EmailSettings emailSettings, I18n i18n) {
    super(settings, emailSettings, i18n, PROPERTY_NEW_ISSUES_NOTIFICATION_SUBJECT, PROPERTY_NEW_ISSUES_NOTIFICATION_CONTENT);
  }

  @Override
  protected String getNotificationType() {
    return NOTIFICATION_TYPE_NEW;
  }

  @Override
  protected String generateUrl(String projectKey, String assignee, Date date) {
    return String.format("%s/issues/search#componentRoots=%s|createdAt=%s|assignees=%s",
      getServerBaseURL(), urlEncode(projectKey), urlEncode(DateUtils.formatDateTime(date)), urlEncode(assignee));
  }

}
