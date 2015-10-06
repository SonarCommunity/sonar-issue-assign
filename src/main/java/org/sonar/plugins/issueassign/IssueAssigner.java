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
import org.sonar.plugins.issueassign.util.DiagnosticLogger;

public class IssueAssigner implements IssueHandler {

  private static final Logger LOG = LoggerFactory.getLogger(IssueAssigner.class);
  private final Settings settings;
  private final Blame blame;
  private final Assign assign;
  private final DiagnosticLogger logger;

  public IssueAssigner(final Settings settings, final UserFinder userFinder,
                       final SonarIndex sonarIndex, final DiagnosticLogger logger) {
    this.blame = new Blame(new ResourceFinder(sonarIndex), new MeasuresFinder(sonarIndex), settings);
    this.assign = new Assign(settings, userFinder);
    this.settings = settings;
    this.logger = logger;
  }

  @Override
  public void onIssue(final Context context) {

    if (!isPluginEnabled()) {
      return;
    }

    final Issue issue = context.issue();
    LOG.debug("Found new issue: ", issue.key());

    try {
      final IssueWrapper issueWrapper = new IssueWrapper(issue, this.settings, this.blame);
      if (issueWrapper.isAssignable()) {
        this.assignIssue(context, issue);
      } else {
          logger.logReason(issueWrapper);
      }
    } catch (final IssueAssignPluginException pluginException) {
      LOG.warn("Unable to assign issue: {}", issue.key());
    } catch (final Exception e) {
      LOG.error("Error assigning issue: {}", issue.key(), e);
    }
  }

  private void assignIssue(final Context context, final Issue issue) throws IssueAssignPluginException {

    final boolean assignToLastCommitter = this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ASSIGN_TO_LAST_COMMITTER);
    final String author = blame.getScmAuthorForIssue(issue, assignToLastCommitter);
    final User assignee;

    if (author == null) {
      LOG.debug("No author found for issue {} component {}", issue.key(), issue.componentKey());
      assignee = assign.getAssignee();
    } else {
      LOG.debug("Found SCM author [" + author + "]");
      assignee = assign.getAssignee(author);
    }

    this.logger.logAssign(issue.key(), assignee.login());
    context.assign(assignee);
  }

  private boolean isPluginEnabled() {
    return this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ENABLED);
  }

}
