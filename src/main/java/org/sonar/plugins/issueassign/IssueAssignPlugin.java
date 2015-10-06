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
import org.sonar.plugins.issueassign.util.DiagnosticLogger;

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
  public static final String PROPERTY_ASSIGN_TO_LAST_COMMITTER = "sonar.issueassign.assign.to.last.committer";
  public static final String PROPERTY_ASSIGN_BLAMELESS_TO_LAST_COMMITTER = "sonar.issueassign.assign.blameless.to.last.committer";
  public static final String PROPERTY_NEW_ISSUES_NOTIFICATION_SUBJECT = "sonar.issueassign.notification.new.subject";
  public static final String PROPERTY_NEW_ISSUES_NOTIFICATION_CONTENT = "sonar.issueassign.notification.new.content";
  public static final String PROPERTY_CHANGED_ISSUES_NOTIFICATION_SUBJECT = "sonar.issueassign.notification.changed.subject";
  public static final String PROPERTY_CHANGED_ISSUES_NOTIFICATION_CONTENT = "sonar.issueassign.notification.changed.content";
  public static final String PROPERTY_SEVERITY = "sonar.issueassign.severity";
  public static final String PROPERTY_ONLY_ASSIGN_NEW = "sonar.onlyassignnew";
  public static final String PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME = "sonar.extract.sonar.user.from.scm.user";
  public static final String PROPERTY_DIAGNOSTIC_LOGGING = "sonar.diagnostic.logging";

  public static final String CONFIGURATION_CATEGORY = "Issue Assign";
  public static final String CONFIGURATION_SUBCATEGORY_WHEN = "When";
  public static final String CONFIGURATION_SUBCATEGORY_NOTIFY = "Notify";
  public static final String CONFIGURATION_SUBCATEGORY_WHO = "Who";
  public static final String CONFIGURATION_SUBCATEGORY_LOGGING = "Logging";

  public static final String NOTIFICATION_TYPE_NEW = "my-new-issues";
  public static final String NOTIFICATION_TYPE_CHANGED = "my-changed-issues";

  private static final String FALSE = "false";

  public static final List<PropertyDefinition> getLoggingProperties() {
    return ImmutableList
        .of(
            PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_DIAGNOSTIC_LOGGING)
                .name("Enable diagnostic logging")
                .description("Extra log messages for diagnostic purposes will appear at INFO log level.")
                .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
                .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_LOGGING)
                .onQualifiers(Qualifiers.PROJECT)
                .type(PropertyType.BOOLEAN)
                .defaultValue(FALSE)
                .build());
  }

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
        .description("Only assign issues with a severity equal to or greater than the selected value.")
        .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
        .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Severity.ALL)
        .defaultValue(Severity.INFO)
        .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_ENABLED)
          .name("Enabled")
          .description("Enable or disable the Issue Assign plugin.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.BOOLEAN)
          .defaultValue(FALSE)
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_ISSUE_CUTOFF_DATE)
          .name("Issue cutoff date")
          .description("Only assign issues introduced after this date. Use the format " + IssueWrapper.ISSUE_CUTOFF_DATE_FORMAT)
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHEN)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.STRING)
          .defaultValue("")
          .build(),

      PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW)
          .name("Only assign new issues")
          .description("Only assign new issues raised in the current analysis.  Set to false to assign all qualified unassigned issues.")
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
          .builder(IssueAssignPlugin.PROPERTY_ASSIGN_TO_LAST_COMMITTER)
          .name("Assign to last committer")
          .description("Assign issue to the last committer of the file, rather than the author as determined by the SCM metrics.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.BOOLEAN)
          .defaultValue(FALSE)
          .build(),

        PropertyDefinition
          .builder(IssueAssignPlugin.PROPERTY_ASSIGN_BLAMELESS_TO_LAST_COMMITTER)
          .name("Assign 'blameless issues' to last committer")
          .description("Assign blameless issues to the last committer of the file.  Blameless issues are issues that " +
                       "don't have an associated line number and therefore cannot be resolved to a particular commit.  " +
                       "For example: squid:S00104 'Files should not have too many lines'")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.BOOLEAN)
          .defaultValue("true")
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_DEFAULT_ASSIGNEE)
          .name("Default Assignee")
          .description("SonarQube user to whom issues will be assigned if the original SCM author is not available in SonarQube.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .build(),

        PropertyDefinition.builder(IssueAssignPlugin.PROPERTY_EXTRACT_SONAR_USERNAME_FROM_SCM_USERNAME)
          .name("Extract SonarQube Username from SCM Username")
          .description("Extract the SonarQube username from the SCM username associated with an issue using a regular expression.")
          .category(IssueAssignPlugin.CONFIGURATION_CATEGORY)
          .subCategory(IssueAssignPlugin.CONFIGURATION_SUBCATEGORY_WHO)
          .onQualifiers(Qualifiers.PROJECT)
          .type(PropertyType.STRING)
          .build()
      );
  }

  @Override
  public List<Object> getExtensions() {
    List<Object> extensions = new ArrayList<Object>();
    extensions.add(DiagnosticLogger.class);
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
    extensions.addAll(IssueAssignPlugin.getLoggingProperties());
    return extensions;
  }
}
