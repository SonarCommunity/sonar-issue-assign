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
package org.sonar.plugins.issueassign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.issueassign.exception.IssueAssignPluginException;
import org.sonar.plugins.issueassign.exception.MissingScmMeasureDataException;
import org.sonar.plugins.issueassign.exception.NoUniqueAuthorForLastCommitException;
import org.sonar.plugins.issueassign.exception.ResourceNotFoundException;
import org.sonar.plugins.issueassign.measures.MeasuresFinder;
import org.sonar.plugins.issueassign.measures.ScmMeasures;

import java.util.*;

public class Blame {

  private static final Logger LOG = LoggerFactory.getLogger(Blame.class);
  private Map<String, ScmMeasures> resourceMeasuresMap = new HashMap<>();
  private final ResourceFinder resourceFinder;
  private final MeasuresFinder measuresFinder;
  private final Settings settings;

  public Blame(final ResourceFinder resourceFinder, final MeasuresFinder measuresFinder, final Settings settings) {
    this.resourceFinder = resourceFinder;
    this.measuresFinder = measuresFinder;
    this.settings = settings;
  }

  public String getScmAuthorForIssue(final Issue issue, final boolean assignToLastCommitter) throws IssueAssignPluginException {
    if (assignToLastCommitter) {
      return this.getLastCommitterForResource(issue.componentKey());
    }

    return this.getAuthorForIssueLine(issue);
  }

  public Date getCommitDateForIssue(final Issue issue) throws IssueAssignPluginException {
     Date commitDate;
     if (issue.line() == null) {
         commitDate = getLastCommitDate(issue.componentKey());
         LOG.debug("Commit date for issue {} (file {}) is {}", issue.key(), issue.componentKey(), commitDate.toString());
     } else {
         commitDate = getMeasuresForResource(issue.componentKey()).getLastCommitsByLine().get(issue.line());
         LOG.debug("Commit date for issue {} (file {} line {}) is {}", issue.key(), issue.componentKey(),issue.line(),commitDate.toString());
     }
    return commitDate;
  }

  private String getLastCommitterForResource(final String resourceKey) throws IssueAssignPluginException {
    final Date lastCommitDate = this.getLastCommitDate(resourceKey);
    final List<Integer> linesFromLastCommit = this.getLinesFromLastCommit(resourceKey, lastCommitDate);
    final ScmMeasures scmMeasures = this.getMeasuresForResource(resourceKey);

    String author = null;

    for (final Integer line : linesFromLastCommit) {
      if (author == null) {
        author = scmMeasures.getAuthorsByLine().get(line);
      } else {
        final String nextAuthor = scmMeasures.getAuthorsByLine().get(line);
        if (!nextAuthor.equals(author)) {
          final String msg = "No unique author found for resource [" + resourceKey + "]";
          LOG.error(msg);
          throw new NoUniqueAuthorForLastCommitException(msg);
        }
      }
    }

    LOG.debug("Found last committer {} for resource {}", author, resourceKey);
    return author;
  }

  private String getAuthorForIssueLine(final Issue issue) throws IssueAssignPluginException {

    final Integer issueLine = issue.line();

    if (issueLine == null) {
      LOG.debug("Issue {} from rule {} has no associated source line.", issue.key(), issue.message());

      if (this.assignBlamelessToLastCommitter()) {
        return this.getLastCommitterForResource(issue.componentKey());
      }
      return null;
    }

    LOG.debug("Issue line for issue {} is {}", issue.key(), issueLine);
    final String author =  getMeasuresForResource(issue.componentKey()).getAuthorsByLine().get(issueLine);
    LOG.debug("Found author {} for issue.", author);
    return author;
  }

  private boolean assignBlamelessToLastCommitter() {
    final boolean assignBlameless = this.settings.getBoolean(IssueAssignPlugin.PROPERTY_ASSIGN_BLAMELESS_TO_LAST_COMMITTER);
    LOG.debug("Assign blameless to last committer: {}.", assignBlameless);
    return assignBlameless;
  }

  private ScmMeasures getMeasuresForResource(final String resourceKey) throws IssueAssignPluginException {
    final ScmMeasures scmMeasures = this.getScmMeasuresForResource(resourceKey);
    if (scmMeasures == null) {
      throw new MissingScmMeasureDataException();
    }
    return scmMeasures;
  }

  private Date getLastCommitDate(final String resourceKey) throws IssueAssignPluginException {
    final ScmMeasures scmMeasures = this.getMeasuresForResource(resourceKey);
    final Collection<Date> commitDatesForResource = scmMeasures.getLastCommitsByLine().values();
    final SortedSet<Date> sortedSet = new TreeSet<>(commitDatesForResource);
    return sortedSet.last();
  }

  private List<Integer> getLinesFromLastCommit(final String resourceKey, final Date lastCommitDate) throws IssueAssignPluginException {

    final List<Integer> lines = new ArrayList<>();
    final ScmMeasures scmMeasures = this.getMeasuresForResource(resourceKey);
    final Iterator<Map.Entry<Integer, Date>> lastCommitsIterator =
      scmMeasures.getLastCommitsByLine().entrySet().iterator();

    while (lastCommitsIterator.hasNext()) {
      final Map.Entry<Integer, Date> entry = lastCommitsIterator.next();
      if (entry.getValue().equals(lastCommitDate)) {
        lines.add(entry.getKey());
      }
    }

    return lines;
  }

  private ScmMeasures getScmMeasuresForResource(final String componentKey) throws MissingScmMeasureDataException, ResourceNotFoundException {
    ScmMeasures scmMeasures = this.resourceMeasuresMap.get(componentKey);

    if (scmMeasures != null) {
      return scmMeasures;
    }

    final Resource resource = this.resourceFinder.find(componentKey);
    scmMeasures = this.measuresFinder.getMeasures(resource);
    this.resourceMeasuresMap.put(componentKey, scmMeasures);
    return scmMeasures;
  }
}
