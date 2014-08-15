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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;

@RunWith(MockitoJUnitRunner.class)
public class ScmMeasuresTest {

  @Mock
  private DecoratorContext decoratorContext;
  @Mock
  private Measure authorPerLineMeasure;
  @Mock
  private Measure lastCommitDateTimeByLine;
  @Mock
  private Measure revisionsByLine;

  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
  private static final String DATE1 = "2013-01-31T12:12:12-0800";
  private static final String DATE2 = "2011-02-01T12:12:12-0800";
  private static final String DATE3 = "2014-01-01T12:12:12-0800";

  private static final String AUTHOR1 = "author1";
  private static final String AUTHOR2 = "author2";
  private static final String AUTHOR3 = "author3";

  private static final String REVISION1 = "1";
  private static final String REVISION2 = "2";
  private static final String REVISION3 = "3";

  private static final String RESOURCE_KEY = "RESOURCE_KEY";
  private static final String AUTHOR_DATA = "1=" + AUTHOR1 + ";2=" + AUTHOR2 + ";3=" + AUTHOR3;
  private static final String COMMIT_DATA = "1=" + DATE1 + ";2=" + DATE2 + ";3=" + DATE3;
  private static final String REVISION_DATA = "1=" + REVISION1 + ";2=" + REVISION2 + ";3=" + REVISION3;

  private ScmMeasures classUnderTest;

  @Before
  public void beforeTest() {
    when(this.decoratorContext.getMeasure(CoreMetrics.SCM_AUTHORS_BY_LINE)).thenReturn(authorPerLineMeasure);
    when(this.decoratorContext.getMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE)).thenReturn(lastCommitDateTimeByLine);
    when(this.decoratorContext.getMeasure(CoreMetrics.SCM_REVISIONS_BY_LINE)).thenReturn(revisionsByLine);

    when(this.authorPerLineMeasure.getData()).thenReturn(AUTHOR_DATA);
    when(this.lastCommitDateTimeByLine.getData()).thenReturn(COMMIT_DATA);
    when(this.revisionsByLine.getData()).thenReturn(REVISION_DATA);

    this.classUnderTest = new ScmMeasures(RESOURCE_KEY, AUTHOR_DATA, COMMIT_DATA, REVISION_DATA);
  }

  @Test
  public void testGetAuthorsByLine() throws Exception {
    Map<Integer, String> mapData = this.classUnderTest.getAuthorsByLine();
    assertThat(mapData).hasSize(3)
      .containsKey(1).containsValue(AUTHOR1)
      .containsKey(2).containsValue(AUTHOR2)
      .containsKey(3).containsValue(AUTHOR3);

    assertThat(mapData.get(1)).isEqualTo(AUTHOR1);
    assertThat(mapData.get(2)).isEqualTo(AUTHOR2);
    assertThat(mapData.get(3)).isEqualTo(AUTHOR3);

    // repeat to test caching

    mapData = this.classUnderTest.getAuthorsByLine();

    assertThat(mapData).hasSize(3)
      .containsKey(1).containsValue(AUTHOR1)
      .containsKey(2).containsValue(AUTHOR2)
      .containsKey(3).containsValue(AUTHOR3);

    assertThat(mapData.get(1)).isEqualTo(AUTHOR1);
    assertThat(mapData.get(2)).isEqualTo(AUTHOR2);
    assertThat(mapData.get(3)).isEqualTo(AUTHOR3);
  }

  @Test
  public void testGetLastCommitsByLine() throws Exception {
    Map<Integer, Date> mapData = this.classUnderTest.getLastCommitsByLine();

    final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    final Date date1 = dateFormat.parse(DATE1);
    final Date date2 = dateFormat.parse(DATE2);
    final Date date3 = dateFormat.parse(DATE3);

    assertThat(mapData).hasSize(3)
      .containsKey(1).containsValue(date1)
      .containsKey(2).containsValue(date2)
      .containsKey(3).containsValue(date3);

    assertThat(mapData.get(1)).isEqualTo(date1);
    assertThat(mapData.get(2)).isEqualTo(date2);
    assertThat(mapData.get(3)).isEqualTo(date3);

    // repeat to test caching

    mapData = this.classUnderTest.getLastCommitsByLine();

    assertThat(mapData).hasSize(3)
      .containsKey(1).containsValue(date1)
      .containsKey(2).containsValue(date2)
      .containsKey(3).containsValue(date3);

    assertThat(mapData.get(1)).isEqualTo(date1);
    assertThat(mapData.get(2)).isEqualTo(date2);
    assertThat(mapData.get(3)).isEqualTo(date3);
  }

  @Test
  public void testGetRevisionsByLine() throws Exception {
    Map<Integer, String> mapData = this.classUnderTest.getRevisionsByLine();

    assertThat(mapData).hasSize(3)
      .containsKey(1).containsValue(REVISION1)
      .containsKey(2).containsValue(REVISION2)
      .containsKey(3).containsValue(REVISION3);

    assertThat(mapData.get(1)).isEqualTo(REVISION1);
    assertThat(mapData.get(2)).isEqualTo(REVISION2);
    assertThat(mapData.get(3)).isEqualTo(REVISION3);

    // repeat to test caching

    mapData = this.classUnderTest.getRevisionsByLine();

    assertThat(mapData).hasSize(3)
      .containsKey(1).containsValue(REVISION1)
      .containsKey(2).containsValue(REVISION2)
      .containsKey(3).containsValue(REVISION3);

    assertThat(mapData.get(1)).isEqualTo(REVISION1);
    assertThat(mapData.get(2)).isEqualTo(REVISION2);
    assertThat(mapData.get(3)).isEqualTo(REVISION3);
  }

  @Test
  public void testGetKey() throws Exception {
    assertThat(this.classUnderTest.getKey()).isEqualTo(RESOURCE_KEY);
  }
}
