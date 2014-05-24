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

  static final String DEFECT_INTRODUCED_DATE_FORMAT = "MM/dd/yyyy";
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
    
    //Make sure the issue is only assigned if new or if it was introduced after the introduced date.
    if (issue.isNew() || (issueAfterDefectIntroducedDate(issue) && issue.assignee() == null)) {
      LOG.debug("Found new issue [" + issue.key() + "]");
      try {
        this.assignIssue(context, issue);
      } catch (final IssueAssignPluginException pluginException) {
        LOG.warn("Unable to assign issue [" + issue.key() + "]");
      } catch (final Exception e) {
        LOG.error("Error assigning issue [" + issue.key() + "]", e);
      }
    } 
  }
  
  private boolean issueAfterDefectIntroducedDate(final Issue issue) {

      boolean result = false;
      String defectIntroducedDatePref = this.settings.getString(IssueAssignPlugin.PROPERTY_DEFECT_INTRODUCED_DATE);
      DateFormat df = new SimpleDateFormat(DEFECT_INTRODUCED_DATE_FORMAT);
      Date introducedDate;

      try {
          if (defectIntroducedDatePref != null) {
              introducedDate = df.parse(defectIntroducedDatePref);

              Date creationDate = issue.creationDate();
              Date updateDate = issue.updateDate();
              if (introducedDate != null) {
                  result = this.createdOrUpdatedAfterIntroductionDate(creationDate, updateDate, introducedDate);
              }
          }
      } catch (ParseException e) {
          LOG.error("Unable to parse date: " + defectIntroducedDatePref);
      }

      return result;
  }

  private boolean createdOrUpdatedAfterIntroductionDate(final Date creationDate, final Date updateDate, final Date introducedDate) {
      boolean problemCreatedAfterIntroducedDate = creationDate != null && introducedDate.before(creationDate);
      boolean problemUpdatedAfterIntroducedDate = updateDate != null && introducedDate.before(updateDate);

      if (problemCreatedAfterIntroducedDate || problemUpdatedAfterIntroducedDate) {
          return true;
      }
      return false;
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
