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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.DefaultIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_CHANGED;
import static org.sonar.plugins.issueassign.IssueAssignPlugin.NOTIFICATION_TYPE_NEW;

@RunWith(MockitoJUnitRunner.class)
public class SendIssueNotificationsPostJobTest {
  @Mock
  Project project;

  @Mock
  ProjectIssues projectIssues;

  @Mock
  IssueNotifications notifications;

  @Mock
  RuleFinder ruleFinder;

  @Mock
  SensorContext sensorContext;

  @Captor
  ArgumentCaptor<Map<String, List<Issue>>> newIssuesArgument;

  @Captor
  ArgumentCaptor<Map<String, List<Issue>>> changedIssuesArgument;

  @Test
  public void should_send_notif_if_new_issues() throws Exception {

    Issue issue1 = new DefaultIssue().setNew(true).setSeverity("MAJOR").setAssignee("user1");
    Issue issue2 = new DefaultIssue().setNew(true).setSeverity("MAJOR").setAssignee("user2");
    Issue issue3 = new DefaultIssue().setNew(false).setSeverity("MINOR");

    final List<Issue> issueList = new ArrayList<>();
    issueList.add( issue1 );
    issueList.add( issue2 );
    issueList.add( issue3 );

    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(this.projectIssues.issues()).thenReturn(issueList);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(this.projectIssues, notifications);
    job.executeOn(project, sensorContext);

    verify(notifications).sendIssues(eq(project), newIssuesArgument.capture(), eq(NOTIFICATION_TYPE_NEW));
    assertThat(newIssuesArgument.getValue().size()).isEqualTo(2);

    for (List<Issue> issues : newIssuesArgument.getValue().values()) {
      assertThat(issues.size()).isEqualTo(1);
    }
  }

  @Test
  public void should_send_notif_if_changed_issues() throws Exception {

    Issue issue1 = new DefaultIssue().setNew(false).setSeverity("MAJOR").setAssignee("user1").setChanged(
      true).setSendNotifications(true);
    Issue issue2 = new DefaultIssue().setNew(false).setSeverity("MINOR").setAssignee("user1").setChanged(
      false).setSendNotifications(true);
    Issue issue3 = new DefaultIssue().setNew(false).setSeverity("MAJOR").setAssignee("user2").setChanged(
      true).setSendNotifications(true);
    Issue issue4 = new DefaultIssue().setNew(true).setSeverity("MINOR").setSendNotifications(true);

    final List<Issue> issueList = new ArrayList<>();
    issueList.add(issue1);
    issueList.add(issue2);
    issueList.add(issue3);
    issueList.add(issue4);

    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(this.projectIssues.issues()).thenReturn(issueList);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(this.projectIssues, notifications);
    job.executeOn(project, sensorContext);

    verify(notifications).sendIssues(eq(project), changedIssuesArgument.capture(), eq(NOTIFICATION_TYPE_CHANGED));
    assertThat(changedIssuesArgument.getValue().size()).isEqualTo(2);
    for (List<Issue> issues : changedIssuesArgument.getValue().values()) {
      assertThat(issues.size()).isEqualTo(1);
    }
  }

  @Test
  public void should_not_send_notif_if_no_new_issues() throws Exception {

    Issue issue1 = new DefaultIssue().setNew(false);
    final List<Issue> issueList = new ArrayList<>();
    issueList.add(issue1);

    when(project.getAnalysisDate()).thenReturn(DateUtils.parseDate("2013-05-18"));
    when(projectIssues.issues()).thenReturn(issueList);

    SendIssueNotificationsPostJob job = new SendIssueNotificationsPostJob(projectIssues, notifications);
    job.executeOn(project, sensorContext);

    verifyZeroInteractions(notifications);
  }

}
