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
package org.sonar.plugins.issueassign.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.plugins.issueassign.IssueAssignPlugin;
import org.sonar.plugins.issueassign.IssueWrapper;
import org.sonar.plugins.issueassign.NoAssignReason;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticLoggerTest {

    private static final String KEY = "KEY";
    private static final String ASSIGNEE = "ASSIGNEE";
    private static final NoAssignReason NO_ASSIGN_REASON = NoAssignReason.NOT_NEW;

    @Mock
    private Settings settings;

    @Mock
    private IssueWrapper issueWrapper;

    @InjectMocks
    private DiagnosticLogger testSubject;

    @Test
    public void log_reason_diagnostic_disabled() {
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_DIAGNOSTIC_LOGGING)).thenReturn(false);
        this.testSubject.logReason(this.issueWrapper);
        verifyZeroInteractions(this.issueWrapper);
    }

    @Test
    public void log_reason_diagnostic_enabled() {
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_DIAGNOSTIC_LOGGING)).thenReturn(true);
        when(this.issueWrapper.getKey()).thenReturn(KEY);
        when(this.issueWrapper.getNoAssignReason()).thenReturn(NO_ASSIGN_REASON);

        this.testSubject.logReason(this.issueWrapper);
    }

    @Test
    public void log_assign_diagnostic_disabled() {
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_DIAGNOSTIC_LOGGING)).thenReturn(false);
        this.testSubject.logAssign(KEY, ASSIGNEE);
        verifyZeroInteractions(this.issueWrapper);
    }

    @Test
    public void log_assign_diagnostic_enabled() {
        when(this.settings.getBoolean(IssueAssignPlugin.PROPERTY_DIAGNOSTIC_LOGGING)).thenReturn(true);
        this.testSubject.logAssign(KEY, ASSIGNEE);
    }
}