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
package org.sonar.plugins.issueassign.notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MyNewIssuesNotificationDispatcherTest {

  @Mock
  private NotificationManager notifications;

  @Mock
  private NotificationDispatcher.Context context;

  @Mock
  private NotificationChannel emailChannel;

  @Mock
  private NotificationChannel twitterChannel;

  private MyNewIssuesNotificationDispatcher dispatcher;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    dispatcher = new MyNewIssuesNotificationDispatcher(notifications);
  }

  @Test
  public void shouldNotDispatchIfNotNewViolationsNotification() throws Exception {
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void shouldDispatchToUsersWhoHaveSubscribedAndFlaggedProjectAsFavourite() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user1", emailChannel);
    recipients.put("user2", twitterChannel);
    when(notifications.findNotificationSubscribers(dispatcher, "struts")).thenReturn(recipients);

    Notification notification = new Notification("my-new-issues").setFieldValue("projectKey", "struts").setFieldValue("assignee", "user1");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("user1", emailChannel);
    verifyNoMoreInteractions(context);
  }
}
