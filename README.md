## Sonar Issue Assign Plugin
==========================

This plugin will automatically assign new issues raised in the current analysis to the SCM author responsible
for the violation.

The master branch will generally be up to date with the latest SonarQube API.

Tested on Subversion and Git.

Issue tracking: http://jira.codehaus.org/browse/SONARPLUGINS/component/16478

## Installation

The plugin can be installed via the Update Center.

## Configuration Options

### Default Assignee

Configure this value to be a valid SonarQube login of a user to whom issues will be assigned if the plugin cannot determine the SonarQube user who is responsible for an issue.  An example of this would be an SCM author who has left your organization and no longer has an account in SonarQube.

### Enabled

The plugin is disabled by default.  It can be enabled or disabled in either the global or project settings.

### Override Assignee

Configure this value to be a valid SonarQube login of a user to whom all issues will be assigned regardless of the SCM author.  Useful to avoid issues being assigned and notifications being sent out to unsuspecting SonarQube users in testing scenarios.

### Assign to Last Committer

Assign issue to the last committer of the file, rather than the author as determined by the SCM metrics.

### Assign 'blameless issues' to Last Committer

Assign blameless issues to the last committer of the file. Blameless issues are issues that don't have an associated line number and therefore cannot be resolved to a particular commit. For example: squid:S00104 'Files should not have too many lines'

### Extract SonarQube Username from SCM Username

Extract the SonarQube username from the SCM username associated with an issue using a given regular expression.

### Issue Cutoff Date

Only assign issues introduced after this date. Use the format dd/MM/yyyy.

### Only Assign New Issues

Only assign new issues raised in the current analysis. Set to false to assign all qualified unassigned issues.

### Severity

Only assign issues with a severity equal to or greater than a configurable value.

### Notifications

Notifications can now be sent when an issue is assigned.  In the top-right corner of the GUI, go to <username> -> My profile -> Overall notifications.  Tick 'New issues assigned to me (batch)' to receive a single notification of all issues assigned to you during the latest analysis.
Notification content is also configurable.  See the options on the plugin settings page.







