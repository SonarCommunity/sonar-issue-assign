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
  private Map<String, ScmMeasures> resourceMeasuresMap = new HashMap<String, ScmMeasures>();
  private final ResourceFinder resourceFinder;
  private final MeasuresFinder measuresFinder;

  public Blame(final ResourceFinder resourceFinder, final MeasuresFinder measuresFinder) {
    this.resourceFinder = resourceFinder;
    this.measuresFinder = measuresFinder;
  }

  public String getScmAuthorForIssue(final Issue issue, final boolean assignToAuthor) throws IssueAssignPluginException {

    final String authorForIssueLine = this.getAuthorForIssueLine(issue);
    final String lastCommitterForResource = getLastCommitterForResource(issue.componentKey());

    if (assignToAuthor || lastCommitterForResource.equals(authorForIssueLine)) {
      LOG.debug("Author {} is also the last committer.", authorForIssueLine);
      return authorForIssueLine;
    }

    LOG.debug("Last committer differs from author, assigning to last committer {}", lastCommitterForResource);
    return lastCommitterForResource;
  }

  public Date getCommitDateForIssue(final Issue issue) throws IssueAssignPluginException {
     Date commitDate;
     if (issue.line() == null){
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
    return getMeasuresForResource(issue.componentKey()).getAuthorsByLine().get(issue.line());
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
    final SortedSet<Date> sortedSet = new TreeSet(commitDatesForResource);
    return sortedSet.last();
  }

  private List<Integer> getLinesFromLastCommit(final String resourceKey, final Date lastCommitDate) throws IssueAssignPluginException {

    final List<Integer> lines = new ArrayList<Integer>();
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
