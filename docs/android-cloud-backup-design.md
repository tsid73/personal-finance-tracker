# Android Cloud Backup Design

## Goal

Design a production-safe cloud backup system for the Android app that can restore the complete local database and critical app settings across devices without weakening the local-first model.

## Scope

- Full Android Room database
- Critical DataStore settings
- Versioned backup metadata
- Point-in-time restore
- Multi-device restore for the same user

Not in scope for the first version:

- Real-time bidirectional sync
- Shared multi-user collaboration
- Partial record merge during restore

## Recommended Architecture

Use encrypted backup archives uploaded from Android to object storage through a thin authenticated API.

Components:

- Android app
- Backup API
- Object storage bucket
- Metadata database
- Optional background worker for retention cleanup

## Backup Object Model

Each backup should contain:

- backup id
- app version
- backup format version
- database schema version
- created timestamp
- device id
- compressed payload checksum
- encrypted payload

Payload contents:

- full Room database export
- DataStore settings snapshot
- backup manifest with table counts and integrity hash

## Security Model

- Require authenticated user identity before upload or restore listing.
- Encrypt backup payload on device before upload.
- Use a per-user envelope key stored through Android Keystore backed key material where possible.
- Store only encrypted payloads in object storage.
- Hash every payload and verify hash before restore.
- Keep signed, short-lived download URLs server-side only.

## Restore Model

1. User selects a cloud backup version.
2. App downloads encrypted payload to a temp file.
3. App verifies manifest version, checksum, and schema compatibility.
4. App performs full local snapshot backup before applying restore.
5. App restores into a temporary database first.
6. App runs integrity checks on the temporary database.
7. App swaps the current database only after validation passes.
8. App restores DataStore settings last.

## Consistency Rules

- Backups are full snapshots, not incremental, for the first version.
- Restore is all-or-nothing.
- Only one backup or restore job can run per device at a time.
- Server never mutates backup payloads after upload.
- Keep immutable backup versions.

## Retention

Suggested default:

- keep last 30 daily backups
- keep last 12 monthly backups
- allow manual pinned backups that are never auto-deleted

## Failure Handling

- Interrupted upload does not register as a valid backup.
- Interrupted restore leaves the active local database untouched.
- Corrupt payloads are rejected before any destructive local change.
- If restore validation fails, keep the current live database and surface the failure.

## API Shape

Minimal API endpoints:

- `POST /android/backups` to create an upload session
- `PUT /android/backups/:id` for payload upload
- `GET /android/backups` to list available backups
- `GET /android/backups/:id` to fetch metadata
- `POST /android/backups/:id/download-token` to obtain a short-lived download URL
- `DELETE /android/backups/:id` for manual deletion

## Rollout Plan

1. Stabilize local JSON backup and restore.
2. Add manifest hashing and temp-restore validation locally.
3. Add authenticated backup metadata API.
4. Add encrypted upload and download.
5. Add background scheduled backups with charging and Wi-Fi constraints.
6. Add backup history UI and restore history UI.

## Open Decisions

- Whether encryption keys are user-password-derived, device-key-derived, or server-wrapped.
- Whether to upload raw JSON snapshots or a SQLite export bundle.
- Whether restore should support same-device only for the first release or cross-device immediately.
- Whether to support automatic daily backups or manual-only at launch.
