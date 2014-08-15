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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueHandler;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.plugins.issueassign.exception.IssueAssignPluginException;
import org.sonar.plugins.issueassign.measures.MeasuresFinder;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IssueAssigner implements IssueHandler {

  static final String ISSUE_CUTOFF_DATE_FORMAT = "dd/MM/yyyy";
  private static final Logger LOG = LoggerFactory.getLogger(IssueAssigner.class);
  private final Settings settings;
  private final Blame blame;
  private final Assign assign;

  public IssueAssigner(final Settings settings, final UserFinder userFinder, final SonarIndex sonarIndex) {
    this.blame = new Blame(new ResourceFinder(sonarIndex), new MeasuresFinder(sonarIndex));
    this.assign = new Assign(settings, userFinder);
    this.settings = settings;
  }

  public void onIssue(final Context context) {

    if (!shouldExecute()) {
      return;
    }

    final Issue issue = context.issue();
    LOG.debug("Found new issue [" + issue.key() + "]");

    try {
      if (this.shouldAssign(issue)) {
        this.assignIssue(context, issue);
      }
    } catch (final IssueAssignPluginException pluginException) {
      LOG.warn("Unable to assign issue [" + issue.key() + "]");
    } catch (final Exception e) {
      LOG.error("Error assigning issue [" + issue.key() + "]", e);
    }
  }

  private boolean shouldAssign(final Issue issue) throws IssueAssignPluginException {
    return (issueCreatedAfterCutoffDate(issue) && issue.assignee() == null);
  }

  private boolean issueCreatedAfterCutoffDate(final Issue issue) throws IssueAssignPluginException {

    boolean result = false;
    final String issueCutoffDatePref = this.settings.getString(IssueAssignPlugin.PROPERTY_ISSUE_CUTOFF_DATE);
    final DateFormat df = new SimpleDateFormat(ISSUE_CUTOFF_DATE_FORMAT);

    try {
      if (issueCutoffDatePref != null) {
        final Date cutoffDate = df.parse(issueCutoffDatePref);

        if (cutoffDate != null) {
          LOG.debug("Issue cutoff date is {}", cutoffDate);
          result = this.createdAfterCutoffDate(issue, cutoffDate);
        }
      }
    } catch (ParseException e) {
      LOG.error("Unable to parse date: " + issueCutoffDatePref);
    }

    return result;
  }

  private boolean createdAfterCutoffDate(final Issue issue, final Date cutoffDate)
    throws IssueAssignPluginException {
    Date issueCreatedDate = this.blame.getCommitDateForIssueLine(issue);
    boolean createdAfter = issueCreatedDate.after(cutoffDate);

    if (createdAfter) {
      LOG.debug("Issue {} created after cutoff date, will attempt to assign.", issue.key());
    }
    else {
      LOG.debug("Issue {} created before cutoff date and will not attempt to assign.", issue.key());
    }

    return createdAfter;
  }

  private void assignIssue(final Context context, final Issue issue) throws IssueAssignPluginException {

    final boolean assignToAuthor = this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ASSIGN_TO_AUTHOR);
    final String author = blame.getScmAuthorForIssue(issue, assignToAuthor);
    final User assignee;

    if (author == null) {
      LOG.debug("No author found for issue [" + issue.key() + " component [" + issue.componentKey() + "]");
      assignee = assign.getAssignee();
    } else {
      LOG.debug("Found SCM author [" + author + "]");
      assignee = assign.getAssignee(author);
    }

    LOG.info("Assigning issue [" + issue.key() + "] to assignee [" + assignee.login() + "]");
    context.assign(assignee);
  }

  private boolean shouldExecute() {
    return this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ENABLED);
  }
}
