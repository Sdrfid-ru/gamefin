# Analytics events

Events are pseudonymous and have `event_id`, `occurred_at`, `app_version`, `session_id`, `event_name`, minimal properties and consent state. No pet name, free text, contact data, precise age or device identifiers are sent.

Supported events: `app_open`, `task_started`, `task_completed`, `decision_selected`, `decision_result`, `expense_created`, `savings_created`, `purchase_completed`, `goal_completed`, `pet_level_up`, `achievement_unlocked`, `session_return`, `skill_changed`.

AnalyticsService is a domain abstraction. Vendor SDKs are adapters and may not be called from UI or business rules. Retention, lawful basis and export/deletion behaviour require legal approval before release.
