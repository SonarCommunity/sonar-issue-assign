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

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.issueassign.notification.MyChangedIssuesEmailTemplate;
import org.sonar.plugins.issueassign.notification.MyChangedIssuesNotificationDispatcher;
import org.sonar.plugins.issueassign.notification.MyNewIssuesEmailTemplate;
import org.sonar.plugins.issueassign.notification.MyNewIssuesNotificationDispatcher;
import org.sonar.plugins.issueassign.notification.SendIssueNotificationsPostJob;

import java.util.Arrays;
import java.util.List;

/**
 * Main plugin class
 */
@Properties({
    @Property(key = IssueAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE,
        name = "Default Assignee",
        description = "Sonar user to whom issues will be assigned if the original " +
            "SCM author is not available in SonarQube.",
        project = true,
        type = PropertyType.STRING),
    @Property(key = IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE,
        name = "Override Assignee",
        description = "Sonar user to whom all issues will be assigned, if configured.",
        project = true,
        type = PropertyType.STRING),
    @Property(key = IssueAssignPlugin.PROPERTY_ENABLED,
        name = "Enabled",
        description = "Enable or disable the Issue Assign plugin.",
        project = true,
        type = PropertyType.BOOLEAN,
        defaultValue = "false"),
    @Property(key = IssueAssignPlugin.PROPERTY_DEFECT_INTRODUCED_DATE,
        name = "Defect introduced date",
        description = "Any defects introduced or updated after this date are auto assigned, and any defects before will be ignored. Use the format " + IssueAssigner.DEFECT_INTRODUCED_DATE_FORMAT,
        project = true,
        type = PropertyType.STRING,
        defaultValue = ""),
    @Property(key = IssueAssignPlugin.PROPERTY_EMAIL_START_CHAR,
        name = "SCM Author email start character",
        description = "Use to identify an email address embedded into an SCM username.  For example, a Git username such as: GitUser<gituser@domain.com>.",
        project = true,
        type = PropertyType.STRING,
        defaultValue = ""),
    @Property(key = IssueAssignPlugin.PROPERTY_EMAIL_END_CHAR,
        name = "SCM Author email end character",
        description = "Use to identify an email address embedded into an SCM username.  For example, a Git username such as: GitUser<gituser@domain.com>.",
        project = true,
        type = PropertyType.STRING,
        defaultValue = ""),
    @Property(key = IssueAssignPlugin.PROPERTY_ASSIGN_TO_AUTHOR,
        name = "Always assign to Author",
        description = "Set to true if you want to always assign to the defect author, set to false if you want to assign to the last committer on the file if they are different from the author.",
        project = true,
        type = PropertyType.BOOLEAN,
        defaultValue = "false"),
    @Property(key = IssueAssignPlugin.PROPERTY_NEW_ISSUES_NOTIFICATION_SUBJECT,
        name = "\"New Issues\" email notification subject",
        description = "Subject for the \"New Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}",
        project = true,
        type = PropertyType.STRING,
        defaultValue = "${projectName}: new issues assigned to you"),
    @Property(key = IssueAssignPlugin.PROPERTY_NEW_ISSUES_NOTIFICATION_CONTENT,
        name = "\"New Issues\" email notification content",
        description = "Content for the \"New Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}",
        project = true,
        type = PropertyType.TEXT,
        defaultValue = "Project: ${projectName}\n\n" +
            "${count} new issues\n\n" +
            "   ${countBySeverity}\n\n" +
            "See it in SonarQube: ${url}\n"),
    @Property(key = IssueAssignPlugin.PROPERTY_CHANGED_ISSUES_NOTIFICATION_SUBJECT,
        name = "\"Changed Issues\" email notification subject",
        description = "Subject for the \"Changed Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}",
        project = true,
        type = PropertyType.STRING,
        defaultValue = "${projectName}: changed issues assigned to you"),
    @Property(key = IssueAssignPlugin.PROPERTY_CHANGED_ISSUES_NOTIFICATION_CONTENT,
        name = "\"Changed Issues\" email notification content",
        description = "Content for the \"Changed Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}",
        project = true,
        type = PropertyType.TEXT,
        defaultValue = "Project: ${projectName}\n\n" +
            "${count} changed issues\n\n" +
            "   ${countBySeverity}\n\n" +
            "See it in SonarQube: ${url}\n")
})
public final class IssueAssignPlugin extends SonarPlugin {

  public static final String PROPERTY_DEFAULT_ASSIGNEE = "default.assignee";
  public static final String PROPERTY_OVERRIDE_ASSIGNEE = "override.assignee";
  public static final String PROPERTY_ENABLED = "issueassignplugin.enabled";
  public static final String NOTIFICATION_TYPE_NEW = "my-new-issues";
  public static final String NOTIFICATION_TYPE_CHANGED = "my-changed-issues";
  public static final String PROPERTY_DEFECT_INTRODUCED_DATE = "defect.introduced";
  public static final String PROPERTY_EMAIL_START_CHAR = "email.start.char";
  public static final String PROPERTY_EMAIL_END_CHAR = "email.end.char";
  public static final String PROPERTY_ASSIGN_TO_AUTHOR = "assign.to.last.committer";
  public static final String PROPERTY_NEW_ISSUES_NOTIFICATION_SUBJECT = "sonar.issueassign.notification.new.subject";
  public static final String PROPERTY_NEW_ISSUES_NOTIFICATION_CONTENT = "sonar.issueassign.notification.new.content";
  public static final String PROPERTY_CHANGED_ISSUES_NOTIFICATION_SUBJECT = "sonar.issueassign.notification.changed.subject";
  public static final String PROPERTY_CHANGED_ISSUES_NOTIFICATION_CONTENT = "sonar.issueassign.notification.changed.content";

  public List<Object> getExtensions() {
    return Arrays.asList(
        IssueAssigner.class,
        SendIssueNotificationsPostJob.class,
        MyNewIssuesEmailTemplate.class,
        MyNewIssuesNotificationDispatcher.class,
        MyNewIssuesNotificationDispatcher.newMetadata(),
        MyChangedIssuesEmailTemplate.class,
        MyChangedIssuesNotificationDispatcher.class,
        MyChangedIssuesNotificationDispatcher.newMetadata()
    );
  }
}
