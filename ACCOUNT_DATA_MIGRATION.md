# Account data isolation

All tasks now carry an `owner_user_id`. Existing tasks without an owner are assigned to the oldest registered local account. If no account exists yet, they are assigned when the first account is registered. This preserves legacy tasks, but cannot reconstruct ownership of tasks created by multiple accounts before this update. Review legacy data manually if multiple people used the same installation.

Focus sessions, breaks, and distractions are linked to owned tasks and are filtered through task ownership. Signed-out sessions cannot read task data. Reminder scheduling is refreshed when accounts change.

Test on an emulator with two accounts before submission. No full Gradle build was performed here.
