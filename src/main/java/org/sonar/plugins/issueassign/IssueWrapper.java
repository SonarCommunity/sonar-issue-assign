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
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.issueassign.exception.IssueAssignPluginException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class IssueWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(IssueAssigner.class);
    protected static final String ISSUE_CUTOFF_DATE_FORMAT = "dd/MM/yyyy";
    private Issue sonarIssue;
    private Settings settings;
    private Blame blame;
    private NoAssignReason noAssignReason;

    public IssueWrapper(final Issue sonarIssue, final Settings settings, final Blame blame) {
        this.sonarIssue = sonarIssue;
        this.settings = settings;
        this.blame = blame;
    }

    public NoAssignReason getNoAssignReason() {
        return noAssignReason;
    }

    public boolean isAssignable() throws IssueAssignPluginException {
        return this.isNewEnough() &&
               this.isUnassigned() &&
               this.isSevereEnough() &&
               this.issueCreatedAfterCutoffDate();
    }

    protected boolean isNewEnough() {
        final boolean isNew = this.sonarIssue.isNew();
        final boolean onlyAssignNew = this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ONLY_ASSIGN_NEW);

        if (!onlyAssignNew) {
            // we can assign new and existing issues
            return true;
        }

        // only assign if new
        return this.noAssignReason(isNew, NoAssignReason.NOT_NEW);
    }

    private boolean isUnassigned() {
      return this.noAssignReason(this.sonarIssue.assignee() == null, NoAssignReason.ALREADY_ASSIGNED);
    }

    private boolean isSevereEnough() {
        final String configuredSeverity = this.settings.getString(IssueAssignPlugin.PROPERTY_SEVERITY);
        final String issueSeverity = this.sonarIssue.severity();

        LOG.debug("Configured auto-assign severity: {}", configuredSeverity);
        LOG.debug("Issue {} severity: {}", this.sonarIssue.key(), issueSeverity);

        final List<String> severities = Severity.ALL;

        final int configuredSeverityIndex = severities.indexOf(configuredSeverity);
        final int issueSeverityIndex = severities.indexOf(issueSeverity);
        final boolean isSevereEnough = issueSeverityIndex >= configuredSeverityIndex;

        LOG.debug("Issue {} severe enough to auto-assign: {}", sonarIssue.key(), isSevereEnough);

        return this.noAssignReason(isSevereEnough, NoAssignReason.INSUFFICIENT_SEVERITY);
    }

    private boolean issueCreatedAfterCutoffDate() throws IssueAssignPluginException {

        boolean result = true;
        final Date issueCreatedDate = this.blame.getCommitDateForIssue(sonarIssue);
        final String issueCutoffDatePref = this.settings.getString(IssueAssignPlugin.PROPERTY_ISSUE_CUTOFF_DATE);
        final DateFormat df = new SimpleDateFormat(ISSUE_CUTOFF_DATE_FORMAT);

        try {
            if (issueCutoffDatePref != null) {
                final Date cutoffDate = df.parse(issueCutoffDatePref);

                LOG.debug("Issue cutoff date is {}", cutoffDate);
                result = this.createdAfterCutoffDate(sonarIssue, cutoffDate, issueCreatedDate);
            }
        } catch (ParseException e) {
            LOG.error("Unable to parse date: " + issueCutoffDatePref);
        }

        return this.noAssignReason(result, NoAssignReason.BEFORE_CUTOFF_DATE);
    }

    private boolean createdAfterCutoffDate(final Issue issue, final Date cutoffDate, final Date issueCreatedDate)
            throws IssueAssignPluginException {
        boolean createdAfter = issueCreatedDate.after(cutoffDate);

        if (createdAfter) {
            LOG.debug("Issue {} created after cutoff date, will attempt to assign.", issue.key());
        } else {
            LOG.debug("Issue {} created before cutoff date and will not attempt to assign.", issue.key());
        }

        return createdAfter;
    }

    private boolean noAssignReason(final boolean isAssignable, final NoAssignReason reason) {
        if (!isAssignable) {
            this.noAssignReason = reason;
            return false;
        }
        return true;
    }
}
