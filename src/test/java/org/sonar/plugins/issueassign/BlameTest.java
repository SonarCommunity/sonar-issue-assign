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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.issueassign.exception.MissingScmMeasureDataException;
import org.sonar.plugins.issueassign.exception.NoUniqueAuthorForLastCommitException;
import org.sonar.plugins.issueassign.measures.MeasuresFinder;
import org.sonar.plugins.issueassign.measures.ScmMeasures;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlameTest {

  @Mock
  private Issue issue;
  @Mock
  private ScmMeasures scmMeasures;
  @Mock
  private Map<Integer, String> authorMap;
  @Mock
  private ResourceFinder resourceFinder;
  @Mock
  private MeasuresFinder measuresFinder;
  @Mock
  private Resource resource;
  @Mock
  private Settings settings;

  @InjectMocks
  private Blame testSubject;

  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
  private static final String DATE1_STRING = "2013-01-31T12:12:12-0800";
  private static final String DATE2_STRING = "2011-02-01T12:12:12-0800";
  private static final String DATE3_STRING = "2014-01-01T12:12:12-0800";

  private static Date DATE1;
  private static Date DATE2;
  private static Date DATE3;

  private static final String AUTHOR1 = "author1";
  private static final String AUTHOR2 = "author2";
  private static final String AUTHOR3 = "author3";

  private static final String COMPONENT_KEY = "COMPONENT_KEY";

  @Before
  public void beforeTest() throws ParseException {
    final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT);

    DATE1 = SIMPLE_DATE_FORMAT.parse(DATE1_STRING);
    DATE2 = SIMPLE_DATE_FORMAT.parse(DATE2_STRING);
    DATE3 = SIMPLE_DATE_FORMAT.parse(DATE3_STRING);
  }

  @Test
  public void testGetAuthorSameAsLastCommitter() throws Exception {

    final Map<Integer, String> authorMap = new HashMap<>();
    authorMap.put(1, AUTHOR1);
    authorMap.put(2, AUTHOR1);
    authorMap.put(3, AUTHOR1);
    authorMap.put(4, AUTHOR1);

    final Map<Integer, Date> lastCommitDateMap = new HashMap<>();
    lastCommitDateMap.put(1, DATE1);
    lastCommitDateMap.put(2, DATE2);
    lastCommitDateMap.put(3, DATE3);
    lastCommitDateMap.put(4, DATE3);

    when(resourceFinder.find(COMPONENT_KEY)).thenReturn(resource);
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getAuthorsByLine()).thenReturn(authorMap);
    when(scmMeasures.getLastCommitsByLine()).thenReturn(lastCommitDateMap);

    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.line()).thenReturn(1);

    final String author = testSubject.getScmAuthorForIssue(issue, false);
    assertThat(author).isEqualTo(AUTHOR1);
  }

  @Test
  public void getCommitDateForIssueWithLineNumber() throws Exception {

    final int issueLineNumber = 1;

    final Map<Integer, Date> lastCommitDateMap = new HashMap<>();
    lastCommitDateMap.put(1, DATE1);
    lastCommitDateMap.put(2, DATE2);
    lastCommitDateMap.put(3, DATE3);
    lastCommitDateMap.put(4, DATE3);

    when(issue.line()).thenReturn(issueLineNumber);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(resourceFinder.find(COMPONENT_KEY)).thenReturn(resource);
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getLastCommitsByLine()).thenReturn(lastCommitDateMap);

    final Date commitDate = testSubject.getCommitDateForIssue(issue);
    assertThat(commitDate).isEqualTo(DATE1);
  }

  @Test
  public void getCommitDateForIssueWithNoLineNumber() throws Exception {

    final Map<Integer, Date> lastCommitDateMap = new HashMap<>();
    lastCommitDateMap.put(1, DATE1);
    lastCommitDateMap.put(2, DATE2);
    lastCommitDateMap.put(3, DATE3);
    lastCommitDateMap.put(4, DATE3);

    when(issue.line()).thenReturn(null);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(resourceFinder.find(COMPONENT_KEY)).thenReturn(resource);
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getLastCommitsByLine()).thenReturn(lastCommitDateMap);

    final Date commitDate = testSubject.getCommitDateForIssue(issue);
    assertThat(commitDate).isEqualTo(DATE3);
  }

  @Test
  public void getScmAuthorForIssueWithNoLineNumber() throws Exception {
    when(issue.line()).thenReturn(null);
    when(settings.getBoolean(IssueAssignPlugin.PROPERTY_ASSIGN_BLAMELESS_TO_LAST_COMMITTER)).thenReturn(false);

    final String author = testSubject.getScmAuthorForIssue(issue, false);
    assertThat(author).isNull();
  }

  @Test
  public void assignBlamelessToLastCommitter() throws Exception {

    final Map<Integer, Date> lastCommitDateMap = new HashMap<>();
    lastCommitDateMap.put(1, DATE1);
    lastCommitDateMap.put(2, DATE2);
    lastCommitDateMap.put(3, DATE3);

    when(issue.line()).thenReturn(null);
    when(settings.getBoolean(IssueAssignPlugin.PROPERTY_ASSIGN_BLAMELESS_TO_LAST_COMMITTER)).thenReturn(true);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(resourceFinder.find(COMPONENT_KEY)).thenReturn(resource);
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getLastCommitsByLine()).thenReturn(lastCommitDateMap);
    when(scmMeasures.getAuthorsByLine()).thenReturn(authorMap);
    when(authorMap.get(3)).thenReturn(AUTHOR3);

    final String author = testSubject.getScmAuthorForIssue(issue, false);
    assertThat(author).isEqualTo(AUTHOR3);
  }

  @Test
  public void testGetAuthorIsLastCommitter() throws Exception {

    final Map<Integer, String> authorMap = new HashMap<>();
    authorMap.put(1, AUTHOR1);
    authorMap.put(2, AUTHOR2);
    authorMap.put(3, AUTHOR3);

    final Map<Integer, Date> lastCommitDateMap = new HashMap<>();
    lastCommitDateMap.put(1, DATE1);
    lastCommitDateMap.put(2, DATE2);
    lastCommitDateMap.put(3, DATE3);

    when(resourceFinder.find(COMPONENT_KEY)).thenReturn(resource);
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getAuthorsByLine()).thenReturn(authorMap);
    when(scmMeasures.getLastCommitsByLine()).thenReturn(lastCommitDateMap);

    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.line()).thenReturn(1);

    final String author = testSubject.getScmAuthorForIssue(issue, true);
    assertThat(author).isEqualTo(AUTHOR3);
  }

  @Test(expected = MissingScmMeasureDataException.class)
  public void testGetAuthorWithMissingMeasures() throws Exception {
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getAuthorsByLine()).thenReturn(null);

    testSubject.getScmAuthorForIssue(issue, false);
  }

  @Test(expected = NoUniqueAuthorForLastCommitException.class)
  public void testGetAuthorNoUniqueAuthorForLastCommit() throws Exception {

    final Map<Integer, String> authorMap = new HashMap<>();
    authorMap.put(1, AUTHOR1);
    authorMap.put(2, AUTHOR2);

    final Map<Integer, Date> lastCommitDateMap = new HashMap<>();
    lastCommitDateMap.put(1, DATE1);
    lastCommitDateMap.put(2, DATE1);

    when(resourceFinder.find(COMPONENT_KEY)).thenReturn(resource);
    when(measuresFinder.getMeasures(resource)).thenReturn(scmMeasures);
    when(scmMeasures.getAuthorsByLine()).thenReturn(authorMap);
    when(scmMeasures.getLastCommitsByLine()).thenReturn(lastCommitDateMap);

    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.line()).thenReturn(1);

    final String author = testSubject.getScmAuthorForIssue(issue, true);
    assertThat(author).isEqualTo(AUTHOR3);
  }
}
