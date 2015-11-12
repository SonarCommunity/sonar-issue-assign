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
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.issueassign.exception.MissingScmMeasureDataException;

public class MeasuresFinder {

  private static final Logger LOG = LoggerFactory.getLogger(MeasuresFinder.class);
  private SonarIndex sonarIndex;

  public MeasuresFinder(final SonarIndex sonarIndex) {
    this.sonarIndex = sonarIndex;
  }

  public ScmMeasures getMeasures(final Resource resource) throws MissingScmMeasureDataException {
    final String authorsByLineMeasureData = this.getMeasureData(resource, CoreMetrics.SCM_AUTHORS_BY_LINE);
    final String lastCommitByLineMeasureData = this.getMeasureData(resource, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
    final String revisionsByLineMeasureData = this.getMeasureData(resource, CoreMetrics.SCM_REVISIONS_BY_LINE);

    return new ScmMeasures(resource.getEffectiveKey(), authorsByLineMeasureData,
      lastCommitByLineMeasureData, revisionsByLineMeasureData);
  }

  private String getMeasureData(final Resource resource, final Metric<?> metric) throws MissingScmMeasureDataException {
    final Measure<?> measure = this.sonarIndex.getMeasure(resource, metric);
    if (MeasureUtils.hasData(measure)) {  // NOSONAR
      LOG.debug("Found data for metric [" + metric.getKey() + "] on resource [" + resource.getKey() + "]: data: [" + measure.getData() + "]");
      return measure.getData();
    }
    LOG.debug("No measure found for metric [" + metric.getKey() + "] on resource [" + resource.getKey() + "]");
    throw new MissingScmMeasureDataException();
  }
}
