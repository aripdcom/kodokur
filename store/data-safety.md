# Data safety form (Play Console → App content → Data safety)

**Does your app collect or share any of the required user data types?** → **No.**

Reasoning (if the form asks for an explanation):

- The manifest has no `INTERNET` permission; the app cannot send data to any server.
  In Play's definition, "collection" means data leaving the device. Here no data
  leaves the device.
- **Camera:** frames are decoded in memory and discarded; no photo or video is stored.
- **Reading from the gallery:** the system photo picker hands over only the one image
  the user selects; there is no storage permission.
- **History:** the scanned content, format and time stay in the app's private storage.
  They are excluded from cloud backup and device-to-device transfer
  (`data_extraction_rules.xml`).
- **Sharing and export** are user-initiated transfers through the system share sheet.
  Play does not count them as "sharing": the user sends the data to another app
  themselves.
- **External links** (Open Library, Open Food Facts, web search, opening a link) are
  handed to the browser with an intent; Kodokur does not make that request.

With this answer the form is published as "No data collected" and "No data shared
with third parties". The encryption and deletion request questions are not asked.

## Permissions

| Permission | Why | Declaration needed? |
|---|---|---|
| `CAMERA` | reading codes | No (not on the sensitive permissions declaration list) |

`release.yml` enforces this: if the signed APK carries any permission other than
the camera, the release is not published.
