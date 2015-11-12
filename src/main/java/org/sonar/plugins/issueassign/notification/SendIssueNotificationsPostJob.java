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
package org.sonar.plugins.issueassign.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.notification.NotificationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_CHANGED;
import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_NEW;

/**
 * Creates a "my-new-issues" notification for new issues assigned to a developer, and a "my-changed-issues" notificationper assignee for changed issues.
 */
public class SendIssueNotificationsPostJob implements PostJob {

  private static final Logger LOG = LoggerFactory.getLogger(SendIssueNotificationsPostJob.class);
  private final ProjectIssues projectIssues;
  private final IssueNotifications notifications;

  protected SendIssueNotificationsPostJob(ProjectIssues projectIssues, IssueNotifications notifications) {
    this.projectIssues = projectIssues;
    this.notifications = notifications;
  }

  public SendIssueNotificationsPostJob(ProjectIssues projectIssues, NotificationManager notificationsManager) {
    this.projectIssues = projectIssues;
    this.notifications = new IssueNotifications(notificationsManager);
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    sendNotifications(project);
  }

  private void sendNotifications(Project project) {
    LOG.debug("Generating notifications for {}", project.getName());
    Map<String, List<Issue>> newIssuesByAssignee = new HashMap<>();
    Map<String, List<Issue>> changedIssuesByAssignee = new HashMap<>();

    for (Issue issue : projectIssues.issues()) {

      DefaultIssue defaultIssue = (DefaultIssue)issue;
      String assignee = defaultIssue.assignee();

      if (assignee == null) {
        continue;
      }

      if (defaultIssue.isNew() && defaultIssue.resolution() == null) {
          List<Issue> newIssuesBySeverity = newIssuesByAssignee.get(assignee);
      if (newIssuesBySeverity == null || newIssuesBySeverity.isEmpty()) {
          newIssuesBySeverity = new ArrayList<>();
          newIssuesByAssignee.put(assignee, newIssuesBySeverity);
        }
        newIssuesBySeverity.add(issue);
      } else if (!defaultIssue.isNew() && defaultIssue.isChanged() && defaultIssue.mustSendNotifications()) {
          List<Issue> changedIssuesBySeverity = changedIssuesByAssignee.get(assignee);
        if (changedIssuesBySeverity == null) {
          changedIssuesBySeverity = new ArrayList<>();
          changedIssuesByAssignee.put(assignee, changedIssuesBySeverity);
        }
        changedIssuesBySeverity.add(issue);
      }
    }

    LOG.debug("Generating {} notifications for new issues.", newIssuesByAssignee.size());
    if (!newIssuesByAssignee.isEmpty()) {
      notifications.sendIssues(project, newIssuesByAssignee, NOTIFICATION_TYPE_NEW);
    }

    LOG.debug("Generating {} notifications for changed issues.", changedIssuesByAssignee.size());
    if (!changedIssuesByAssignee.isEmpty()) {
      notifications.sendIssues(project, changedIssuesByAssignee, NOTIFICATION_TYPE_CHANGED);
    }
  }

}
