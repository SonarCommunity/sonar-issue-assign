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
package org.sonar.plugins.issueassign.measures;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.issueassign.exception.MissingScmMeasureDataException;

@RunWith(MockitoJUnitRunner.class)
public class MeasuresFinderTest {

  @Mock
  private Resource resource;
  @Mock
  private SonarIndex sonarIndex;

  @Mock
  private Measure scmAuthorsByLineMeasure;
  @Mock
  private Measure scmLastCommitDateTimesByLineMeasure;
  @Mock
  private Measure scmRevisionsByLineMeasure;

  private static final String RESOURCE_EFFECTIVE_KEY = "RESOURCE_EFFECTIVE_KEY";

  // not 'real' data
  private static final String SCM_AUTHOR_BY_LINE_DATA = "SCM_AUTHOR_BY_LINE_DATA";
  private static final String SCM_LAST_COMMIT_DATA = "SCM_LAST_COMMIT_DATA";
  private static final String SCM_REVISIONS_BY_LINE_DATA = "SCM_REVISIONS_BY_LINE_DATA";

  @Test
  public void testGetMeasures() throws Exception {

    when(this.sonarIndex.getMeasure(resource, CoreMetrics.SCM_AUTHORS_BY_LINE)).thenReturn(scmAuthorsByLineMeasure);
    when(this.sonarIndex.getMeasure(resource, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE)).thenReturn(scmLastCommitDateTimesByLineMeasure);
    when(this.sonarIndex.getMeasure(resource, CoreMetrics.SCM_REVISIONS_BY_LINE)).thenReturn(scmRevisionsByLineMeasure);

    when(this.scmAuthorsByLineMeasure.getData()).thenReturn(SCM_AUTHOR_BY_LINE_DATA);
    when(this.scmLastCommitDateTimesByLineMeasure.getData()).thenReturn(SCM_LAST_COMMIT_DATA);
    when(this.scmRevisionsByLineMeasure.getData()).thenReturn(SCM_REVISIONS_BY_LINE_DATA);

    when(this.resource.getEffectiveKey()).thenReturn(RESOURCE_EFFECTIVE_KEY);

    final MeasuresFinder classUnderTest = new MeasuresFinder(sonarIndex);
    final ScmMeasures scmMeasure = classUnderTest.getMeasures(resource);

    assertThat(scmMeasure).isNotNull();
    assertThat(scmMeasure.getKey()).isEqualTo(RESOURCE_EFFECTIVE_KEY);
  }

  @Test(expected = MissingScmMeasureDataException.class)
  public void testGetMeasuresWithNoData() throws Exception {

    when(this.sonarIndex.getMeasure(resource, CoreMetrics.SCM_AUTHORS_BY_LINE)).thenReturn(scmAuthorsByLineMeasure);
    when(this.scmAuthorsByLineMeasure.getData()).thenReturn(null);
    when(this.resource.getEffectiveKey()).thenReturn(RESOURCE_EFFECTIVE_KEY);

    final MeasuresFinder classUnderTest = new MeasuresFinder(sonarIndex);
    classUnderTest.getMeasures(resource);
  }
}
