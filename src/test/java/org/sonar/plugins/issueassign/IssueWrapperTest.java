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
package org.sonar.plugins.issueassign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.plugins.issueassign.exception.IssueAssignPluginException;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueWrapperTest {

    @Mock
    private Settings settings;

    @Mock
    private Blame blame;

    @Mock
    private Issue issue;

    @InjectMocks
    private IssueWrapper testSubject;

    @Test
    public void test_issue_already_assigned() throws IssueAssignPluginException {
        when(this.issue.assignee()).thenReturn("some_guy");
        assertThat(this.testSubject.isAssignable()).isFalse();
    }

    @Test
    public void test_issue_not_assigned() throws IssueAssignPluginException {
        when(this.issue.assignee()).thenReturn(null);
        assertThat(this.testSubject.isAssignable()).isTrue();
    }

    @Test
    public void test_new_issue_and_assign_only_new_configured() throws Exception {
      when(this.issue.isNew()).thenReturn(true);
      when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW)).thenReturn(true);

      assertThat(this.testSubject.isAssignable()).isTrue();
    }

    @Test
    public void test_new_issue_and_assign_all() throws Exception {
        when(this.issue.isNew()).thenReturn(true);
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW)).thenReturn(false);

        assertThat(this.testSubject.isAssignable()).isTrue();
    }

    @Test
    public void test_existing_issue_and_assign_only_new_configured() throws Exception {
        when(this.issue.isNew()).thenReturn(false);
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW)).thenReturn(true);

        assertThat(this.testSubject.isAssignable()).isFalse();
    }

    @Test
    public void test_existing_issue_and_assign_all() throws Exception {
        when(this.issue.isNew()).thenReturn(false);
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW)).thenReturn(false);

        assertThat(this.testSubject.isAssignable()).isTrue();
    }

    @Test
    public void creation_date_after_issue_cutoff_date() throws Exception {

        String issueCreationDateText = "03/04/2014";
        String cutoffDateText = "02/04/2014";

        SimpleDateFormat df = new SimpleDateFormat(IssueWrapper.ISSUE_CUTOFF_DATE_FORMAT);
        Date issueCreationDate = df.parse(issueCreationDateText);

        when(settings.getString(IssueAssignPlugin.PROPERTY_ISSUE_CUTOFF_DATE)).thenReturn(cutoffDateText);
        when(blame.getCommitDateForIssue(issue)).thenReturn(issueCreationDate);
        when(settings.getBoolean(IssueAssignPlugin.PROPERTY_ASSIGN_TO_AUTHOR)).thenReturn(true);
        when(issue.creationDate()).thenReturn(issueCreationDate);

        assertThat(this.testSubject.isAssignable()).isTrue();
    }

    @Test
    public void creation_date_before_issue_cutoff_date() throws Exception {

      String issueCreationDateText = "01/04/2014";
      String cutoffDateText = "02/04/2014";

      SimpleDateFormat df = new SimpleDateFormat(IssueWrapper.ISSUE_CUTOFF_DATE_FORMAT);
      Date issueCreationDate = df.parse(issueCreationDateText);

      when(settings.getString(IssueAssignPlugin.PROPERTY_ISSUE_CUTOFF_DATE)).thenReturn(cutoffDateText);
      when(blame.getCommitDateForIssue(issue)).thenReturn(issueCreationDate);
      assertThat(this.testSubject.isAssignable()).isFalse();
    }

    @Test
    public void issue_not_severe_enough() throws Exception {
        when(settings.getString(IssueAssignPlugin.PROPERTY_SEVERITY)).thenReturn("MAJOR");
        when(issue.severity()).thenReturn("MINOR");

        assertThat(this.testSubject.isAssignable()).isFalse();
    }

    @Test
    public void is_severe_enough_equal_to() throws IssueAssignPluginException {
        when(settings.getString(IssueAssignPlugin.PROPERTY_SEVERITY)).thenReturn("MAJOR");
        when(issue.severity()).thenReturn("MAJOR");
        assertThat(this.testSubject.isAssignable()).isTrue();
    }

    @Test
    public void is_severe_enough_greater_than() throws IssueAssignPluginException {
        when(settings.getString(IssueAssignPlugin.PROPERTY_SEVERITY)).thenReturn("MAJOR");
        when(issue.severity()).thenReturn("CRITICAL");

        assertThat(this.testSubject.isAssignable()).isTrue();
    }
}