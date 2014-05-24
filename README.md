Sonar Issue Assign Plugin
==========================

This plugin will automatically assign new issues raised in the current analysis to the SCM author responsible
for the violation.  The out-of-the-box SonarQube notification framework will then automatically notify the assignee,
if configured.

If the author is not registered in SonarQube the issue will be assigned to a configurable default assignee.
  
The plugin can handle scenarios where the violator is not the original author of the code in which
the issue is raised, but rather the last committer.  For example, in metrics where the length of a
method has exceeded the maximum threshold.  In this case the issue will be assigned to the last committer.

For Git users, the SCM author is an email address.  The plugin can map this email address to a Sonar user,
provided the email address is the same for the SCM and SonarQube accounts.


The plugin is configurable on a project level.  Configurable items include: enable/disable the plugin, default assignee if SCM author doesn't exist in SonarQube, an 'override' assignee that is useful for testing so that other users are not spammed with notifications.

The master branch will generally be up to date with the latest SonarQube API.  A compatibility matrix is available on the project hompage.

Tested on Subversion 1.7 and Git.

Project homepage: http://docs.codehaus.org/display/SONAR/Issue+Assign+Plugin

Issue tracking: http://jira.codehaus.org/browse/SONARPLUGINS/component/16478


Future plans:

associate issue with a configurable action plan (SonarQube v4.3+)





