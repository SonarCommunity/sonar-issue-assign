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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.component.Component;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.IssuesBySeverity;

import java.util.Map;

/**
 * Send notifications related to issues.
 */
public class IssueNotifications {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private final NotificationManager notificationsManager;

  public IssueNotifications(NotificationManager notificationsManager) {
    this.notificationsManager = notificationsManager;
  }

  public void sendIssues(Project project, Map<String, IssuesBySeverity> newIssuesByAssignee, String notificationType) {
    for (Map.Entry<String, IssuesBySeverity> entry : newIssuesByAssignee.entrySet()) {
      String assignee = entry.getKey();
      IssuesBySeverity newIssues = entry.getValue();
      logger.debug("Generating notification to {}.", assignee);
      Notification notification = newNotification(project, notificationType)
        .setDefaultMessage(newIssues.size() + " new issues on " + project.getLongName() + ".\n")
        .setFieldValue("projectDate", DateUtils.formatDateTime(project.getAnalysisDate()))
        .setFieldValue("count", String.valueOf(newIssues.size()))
        .setFieldValue("assignee", assignee);
      for (String severity : Severity.ALL) {
        notification.setFieldValue("count-" + severity, String.valueOf(newIssues.issues(severity)));
      }
      notificationsManager.scheduleForSending(notification);
    }
  }

  private Notification newNotification(Component project, String key) {
    return new Notification(key)
      .setFieldValue("projectName", project.longName())
      .setFieldValue("projectKey", project.key());
  }

}
