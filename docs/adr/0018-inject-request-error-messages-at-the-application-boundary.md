# Inject request error messages at the application boundary

The shared request package classifies failures and delegates message resolution and feedback to the consuming application. Admin uses its current locale and UI component, so locale changes affect subsequent errors.

This keeps request mechanics reusable without coupling shared transport to application localization or presentation.
