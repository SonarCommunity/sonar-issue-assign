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
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssuesBySeverity;

import java.util.HashMap;
import java.util.Map;

import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_CHANGED;
import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_NEW;

/**
 * Creates a "my-new-issues" notification for new issues assigned to a developer, and a "my-changed-issues" notificationper assignee for changed issues.
 */
public class SendIssueNotificationsPostJob implements PostJob {

  private Logger logger = LoggerFactory.getLogger(getClass());
  private final IssueCache issueCache;
  private final IssueNotifications notifications;

  protected SendIssueNotificationsPostJob(IssueCache issueCache, IssueNotifications notifications) {
    this.issueCache = issueCache;
    this.notifications = notifications;
  }

  public SendIssueNotificationsPostJob(IssueCache issueCache, NotificationManager notificationsManager) {
    this.issueCache = issueCache;
    this.notifications = new IssueNotifications(notificationsManager);
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    sendNotifications(project);
  }

  private void sendNotifications(Project project) {
    logger.debug("Generating notifications for {}", project.getName());
    Map<String, IssuesBySeverity> newIssuesByAssignee = new HashMap<String, IssuesBySeverity>();
    Map<String, IssuesBySeverity> changedIssuesByAssignee = new HashMap<String, IssuesBySeverity>();
    for (DefaultIssue issue : issueCache.all()) {
      String assignee = issue.assignee();
      if (assignee == null) {
        continue;
      }
      if (issue.isNew() && issue.resolution() == null) {
        IssuesBySeverity newIssuesBySeverity = newIssuesByAssignee.get(assignee);
        if (newIssuesBySeverity == null) {
          newIssuesBySeverity = new IssuesBySeverity();
          newIssuesByAssignee.put(assignee, newIssuesBySeverity);
        }
        newIssuesBySeverity.add(issue);
      } else if (!issue.isNew() && issue.isChanged() && issue.mustSendNotifications()) {
        IssuesBySeverity changedIssuesBySeverity = changedIssuesByAssignee.get(assignee);
        if (changedIssuesBySeverity == null) {
          changedIssuesBySeverity = new IssuesBySeverity();
          changedIssuesByAssignee.put(assignee, changedIssuesBySeverity);
        }
        changedIssuesBySeverity.add(issue);
      }
    }

    logger.debug("Generating {} notifications for new issues.", newIssuesByAssignee.size());
    if (!newIssuesByAssignee.isEmpty()) {
      notifications.sendIssues(project, newIssuesByAssignee, NOTIFICATION_TYPE_NEW);
    }

    logger.debug("Generating {} notifications for changed issues.", changedIssuesByAssignee.size());
    if (!changedIssuesByAssignee.isEmpty()) {
      notifications.sendIssues(project, changedIssuesByAssignee, NOTIFICATION_TYPE_CHANGED);
    }
  }

}
