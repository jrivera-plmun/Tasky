# TASKY Submission Audit

## Verified by source inspection
- Task Management includes text search and filters for category, priority, and status.
- This revision adds a deadline filter with All dates, Overdue, Today, Tomorrow, and Next 7 days.
- Deadline filtering can be combined with the existing category, priority, status, and keyword filters.
- Selecting a specific deadline range switches to All tasks so future matches are not hidden by Today + overdue.
- Manifest declares task reminder receivers and Android notification permissions.
- Kotlin sources include focus-session, scheduling, workload, and database implementation files.
- All XML resources were parsed successfully and the ZIP archive was validated.

## Limitations / manual checks required
- No Android SDK/Gradle build or emulator execution was available for this audit.
- Test all five deadline filter options and combinations on the emulator.
- Test notifications, schedule warnings, task editing, focus sessions, and navigation after this update.
- Authentication and account security should not be represented as production-grade without separate verification.
- The repository contains template unit/instrumentation tests, not comprehensive feature tests.

## Submission scope
- Original layouts and app structure retained except one matching Deadline filter control below the existing filter row.
- Obsolete development update/readme notes removed; essential project README replaced.
