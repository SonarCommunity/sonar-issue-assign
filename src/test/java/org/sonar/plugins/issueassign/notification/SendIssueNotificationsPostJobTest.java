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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_CHANGED;
import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_NEW;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.IssuesBySeverity;

@RunWith(MockitoJUnitRunner.class)
public class SendIssueNotificationsPostJobTest {
  @Mock
  Project project;

  @Mock
  IssueCache issueCache;

  @Mock
  IssueNotifications notifications;

  @Mock
  RuleFinder ruleFinder;

  @Mock
  SensorContext sensorContext;

  @Captor
  ArgumentCaptor<Map<String, IssuesBySeverity>> newIssuesArgument;

  @Captor
  ArgumentCaptor<Map<String, IssuesBySeverity>> changedIssuesArgument;

  @Test
  public void should_send_notif_if_new_issues() throws Exception {
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(true).setSeverity("MAJOR").setAssignee("user1"),
      new DefaultIssue().setNew(true).setSeverity("MAJOR").setAssignee("user2"),
      new DefaultIssue().setNew(false).setSeverity("MINOR")
      ));

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications);
    job.executeOn(project, sensorContext);

    verify(notifications).sendIssues(eq(project), newIssuesArgument.capture(), eq(NOTIFICATION_TYPE_NEW));
    assertThat(newIssuesArgument.getValue().size()).isEqualTo(2);
    for (IssuesBySeverity issues : newIssuesArgument.getValue().values()) {
      assertThat(issues.size()).isEqualTo(1);
    }
  }

  @Test
  public void should_send_notif_if_changed_issues() throws Exception {
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(false).setSeverity("MAJOR").setAssignee("user1").setChanged(true).setSendNotifications(true),
      new DefaultIssue().setNew(false).setSeverity("MINOR").setAssignee("user1").setChanged(false).setSendNotifications(true),
      new DefaultIssue().setNew(false).setSeverity("MAJOR").setAssignee("user2").setChanged(true).setSendNotifications(true),
      new DefaultIssue().setNew(true).setSeverity("MINOR").setSendNotifications(true)
      ));

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications);
    job.executeOn(project, sensorContext);

    verify(notifications).sendIssues(eq(project), changedIssuesArgument.capture(), eq(NOTIFICATION_TYPE_CHANGED));
    assertThat(changedIssuesArgument.getValue().size()).isEqualTo(2);
    for (IssuesBySeverity issues : changedIssuesArgument.getValue().values()) {
      assertThat(issues.size()).isEqualTo(1);
    }
  }

  @Test
  public void should_not_send_notif_if_no_new_issues() throws Exception {
    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(issueCache.all()).thenReturn(Arrays.asList(
      new DefaultIssue().setNew(false)
      ));

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(issueCache, notifications);
    job.executeOn(project, sensorContext);

    verifyZeroInteractions(notifications);
  }

}
