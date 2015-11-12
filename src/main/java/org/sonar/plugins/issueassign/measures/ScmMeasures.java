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
package org.sonar.plugins.issueassign.measures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Date;
import java.util.Map;

public class ScmMeasures {

  private static final Logger LOG = LoggerFactory.getLogger(ScmMeasures.class);
  private String resourceKey;

  private final String authorsByLineMeasure;
  private final String lastCommitsByLineMeasure;
  private final String revisionsByLineMeasure;

  private Map<Integer, String> authorsByLine;
  private Map<Integer, Date> lastCommitsByLine;
  private Map<Integer, String> revisionsByLine;

  public ScmMeasures(final String resourceKey, final String authorsByLineMeasure,
    final String lastCommitsByLineMeasure, final String revisionsByLineMeasure) {
    this.resourceKey = resourceKey;
    this.authorsByLineMeasure = authorsByLineMeasure;
    this.lastCommitsByLineMeasure = lastCommitsByLineMeasure;
    this.revisionsByLineMeasure = revisionsByLineMeasure;
  }

  public Map<Integer, String> getAuthorsByLine() {
    if (this.authorsByLine == null) {
      this.authorsByLine = this.parseIntString(CoreMetrics.SCM_AUTHORS_BY_LINE, this.authorsByLineMeasure);
    }
    return this.authorsByLine;
  }

  public Map<Integer, Date> getLastCommitsByLine() {
    if (this.lastCommitsByLine == null) {
      this.lastCommitsByLine = this.parseIntDateTime(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, this.lastCommitsByLineMeasure);
    }
    return this.lastCommitsByLine;
  }

  public Map<Integer, String> getRevisionsByLine() {
    if (this.revisionsByLine == null) {
      this.revisionsByLine = this.parseIntString(CoreMetrics.SCM_REVISIONS_BY_LINE, this.revisionsByLineMeasure);
    }
    return revisionsByLine;
  }

  public String getKey() {
    return resourceKey;
  }

  private Map<Integer, String> parseIntString(final Metric<?> metric, final String measure) {
    this.logMeasureData(metric, measure);
    return KeyValueFormat.parseIntString(measure);
  }

  private Map<Integer, Date> parseIntDateTime(final Metric<?> metric, final String measure) {
    this.logMeasureData(metric, measure);
    return KeyValueFormat.parseIntDateTime(measure);
  }

  private void logMeasureData(final Metric<?> metric, final String measure) {
    LOG.debug(metric.getName() + ": [" + measure + "]");
  }
}
