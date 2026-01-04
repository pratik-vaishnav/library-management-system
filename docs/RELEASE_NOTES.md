# Release Notes

## EduShelf 0.1.0 (Initial Release)

### Highlights
- Offline-first desktop app (JavaFX + SQLite).
- Role-based login (Admin, Operator, Viewer).
- Book, member, and loan workflows with due dates and fines.
- Sync queue with conflict and failure visibility.
- Reports export:
  - Books PDF with school branding.
  - Loans Excel using the approved template format.
- Analytics charts: monthly issues, daily status, top books.
- AI daily summary using local Ollama.
- Quick Add for fast book/member entry.

### Improvements
- Polished UI with branded theme, hero banner, and active nav.
- Autocomplete on ISBN, member, and loan selection.
- Background sync with status indicators.

### Known limitations
- AI summary requires a local Ollama model configured in Settings.
- Sync requires the server to be reachable; offline changes remain queued until sync.
- PDF templates are branded but limited to current layout (multi-page templates planned).

### Coming next
- Conflict resolution drawer with per-field merge guidance.
- Branded report templates with pagination options.
- Additional accessibility options and multi-language support.
