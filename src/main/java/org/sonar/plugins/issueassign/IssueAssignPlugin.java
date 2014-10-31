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

import com.google.common.collect.ImmutableList;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.issueassign.notification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Main plugin class
 */
public final class IssueAssignPlugin extends SonarPlugin {

  public static final String PROPERTY_DEFAULT_ASSIGNEE = "sonar.issueassign.default.assignee";
  public static final String PROPERTY_OVERRIDE_ASSIGNEE = "sonar.issueassign.override.assignee";
  public static final String PROPERTY_ENABLED = "sonar.issueassign.enabled";
  public static final String PROPERTY_ISSUE_CUTOFF_DATE = "sonar.issueassign.issue.cutoff";
  public static final String PROPERTY_EMAIL_START_CHAR = "sonar.issueassign.email.start.char";
  public static final String PROPERTY_EMAIL_END_CHAR = "sonar.issueassign.email.end.char";
  public static final String PROPERTY_ASSIGN_TO_AUTHOR = "sonar.issueassign.assign.to.last.committer";
  public static final String PROPERTY_NEW_ISSUES_NOTIFICATION_SUBJECT = "sonar.issueassign.notification.new.subject";
  public static final String PROPERTY_NEW_ISSUES_NOTIFICATION_CONTENT = "sonar.issueassign.notification.new.content";
  public static final String PROPERTY_CHANGED_ISSUES_NOTIFICATION_SUBJECT = "sonar.issueassign.notification.changed.subject";
  public static final String PROPERTY_CHANGED_ISSUES_NOTIFICATION_CONTENT = "sonar.issueassign.notification.changed.content";
  public static final String PROPERTY_SEVERITY = "sonar.issueassign.severity";
  public static final String PROPERTY_ONLY_ASSIGN_NEW = "sonar.onlyassignnew";

  public static final String CONFIGURATION_CATEGORY = "Issue Assign";
  public static final String CONFIGURATION_SUBCATEGORY_WHEN = "when";
  public static final String CONFIGURATION_SUBCATEGORY_NOTIFY = "notify";
  public static final String CONFIGURATION_SUBCATEGORY_WHO = "who";

  public static final String NOTIFICATION_TYPE_NEW = "my-new-issues";
  public static final String NOTIFICATION_TYPE_CHANGED = "my-changed-issues";

  public static List<PropertyDefinition> getNotificationProperties() {
    return ImmutableList
      .of(
        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_NEW_ISSUES_NOTIFICATION_SUBJECT)
          .name("\"New Issues\" email notification subject")
          .description("Subject for the \"New Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_NOTIFY)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.STRING)
          .defaultValue("${projectName}: new issues assigned to you")
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_NEW_ISSUES_NOTIFICATION_CONTENT)
          .name("\"New Issues\" email notification content")
          .description("Content for the \"New Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_NOTIFY)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.TEXT)
          .defaultValue("Project: ${projectName}\n\n" +
            "${count} new issues\n\n" +
            "   ${countBySeverity}\n\n" +
            "See it in SonarQube: ${url}\n")
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_CHANGED_ISSUES_NOTIFICATION_SUBJECT)
          .name("\"Changed Issues\" email notification subject")
          .description("Subject for the \"Changed Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_NOTIFY)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.STRING)
          .defaultValue("${projectName}: changed issues assigned to you")
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_CHANGED_ISSUES_NOTIFICATION_CONTENT)
          .name("\"Changed Issues\" email notification content")
          .description("Content for the \"Changed Issues\" notification email. Available variables: ${projectName}, ${date}, ${count}, ${countBySeverity}, ${url}")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_NOTIFY)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.TEXT)
          .defaultValue("Project: ${projectName}\n\n" +
            "${count} changed issues\n\n" +
            "   ${countBySeverity}\n\n" +
            "See it in SonarQube: ${url}\n")
          .build());
  }

  public static List<PropertyDefinition> getWhenProperties() {
    return ImmutableList
      .of(PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_SEVERITY)
        .name("Severity")
        .description("Only auto-assign issues with a severity equal to or greater than the selected value.")
        .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
        .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Severity.ALL)
        .defaultValue(Severity.INFO.toString())
        .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_ENABLED)
          .name("Enabled")
          .description("Enable or disable the Issue Assign plugin.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.BOOLEAN)
          .defaultValue("false")
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_ISSUE_CUTOFF_DATE)
          .name("Issue cutoff date")
          .description("Only auto-assign issues introduced after this date. Use the format " + IssueWrapper.ISSUE_CUTOFF_DATE_FORMAT)
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.STRING)
          .defaultValue("")
          .build(),

      PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW)
          .name("Only assign new issues")
          .description("Only auto-assign issues that are new.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.BOOLEAN)
          .defaultValue("true")
          .build());
  }

  public static List<PropertyDefinition> getWhoProperties() {
    return ImmutableList
      .of(
        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_OVERRIDE_ASSIGNEE)
          .name("Override Assignee")
          .description("Sonar user to whom all issues will be assigned, if configured.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .build(),

        PropertyDefinition
          .builder(IssueAssignPlugin.PROPERTY_ASSIGN_TO_AUTHOR)
          .name("Always assign to Author")
          .description(
                  "Set to true if you want to always assign to the defect author, set to false if you want to assign to the last committer on the file if they are different from the author.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.BOOLEAN)
          .defaultValue("false")
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE)
          .name("Default Assignee")
          .description("Sonar user to whom issues will be assigned if the original SCM author is not available in SonarQube.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .build()
      );
  }

  public List<Object> getExtensions() {
    List<Object> extensions = new ArrayList<Object>();
    extensions.add(IssueAssigner.class);
    extensions.add(SendIssueNotificationsPostJob.class);
    extensions.add(MyNewIssuesEmailTemplate.class);
    extensions.add(MyNewIssuesNotificationDispatcher.class);
    extensions.add(MyNewIssuesNotificationDispatcher.newMetadata());
    extensions.add(MyChangedIssuesEmailTemplate.class);
    extensions.add(MyChangedIssuesNotificationDispatcher.class);
    extensions.add(MyChangedIssuesNotificationDispatcher.newMetadata());
    extensions.addAll(IssueAssignPlugin.getWhoProperties());
    extensions.addAll(IssueAssignPlugin.getWhenProperties());
    extensions.addAll(IssueAssignPlugin.getNotificationProperties());
    return extensions;
  }
}
