# Device test protocol

Unit tests exercise the decoder, the parsers and the link checks with generated codes
(`./gradlew :core:test`). Real-world codes (glossy covers, curved boxes, small print)
and camera behavior only show up on a phone. Before every release, run this list with
the **signed release build** and log the result in the table at the bottom.

Setup: if an older version is on the phone, install over it (history must survive).
Turn on TalkBack for the screen reader items.

## 1. Installation and permission

- [ ] First launch: the permission dialog appears; the button behind it says "Allow camera"
- [ ] Deny twice → the button becomes "Open app settings"; grant the permission in
      settings, return to the app → the camera opens
- [ ] "From photo" works without the permission

## 2. Camera

- [ ] Book barcode (EAN-13 + 5-digit price add-on): up close and from 30 cm; landscape
      and portrait
- [ ] Medicine box code (GS1 DataMatrix): expiry, batch and serial are correct; an
      expired box shows a red warning
- [ ] Grocery product (EAN-13, EAN-8 or UPC)
- [ ] Glossy / curved packaging: does tap-to-focus help
- [ ] Small barcode: zoom button 1×→2×→4×→1×, pinch, double tap
- [ ] Flashlight in the dark
- [ ] Inverted QR (light on dark instead of dark on light) — from the gallery

## 3. Content types

- [ ] Wi-Fi QR: password hidden, "Show" works, "Connect" opens the system dialog
      (cancel without saving)
- [ ] Link QR: domain in the title; links with `@`, Cyrillic letters or an `http://`
      IP address show a warning card, and "Open" → confirmation dialog
- [ ] Contact card QR (vCard / MECARD) → the "Add to contacts" form opens pre-filled
- [ ] Email, phone, SMS and location QRs open the matching app

## 4. Batch scanning and history

- [ ] Batch mode: 5 books in a row; scanning the same book again says "already there"
      and the counter does not go up
- [ ] History: delete one entry → "Undo" restores it; "Clear history" asks for
      confirmation
- [ ] CSV export: file appears in the share sheet; in a spreadsheet the Turkish
      characters and the `isbn13` column are correct

## 5. Language and accessibility

- [ ] About → Language: switch between Turkish, English and Arabic (right-to-left)
- [ ] TalkBack: the four scanner buttons are read out (From photo, Flashlight, Zoom
      ratio, Batch); after a scan the type and content are announced
- [ ] TalkBack: on the result screen, the title and copy buttons say what they copy
- [ ] Largest font size: the result and history screens do not overflow

## 6. About

- [ ] The privacy policy link opens `kodokur.aripd.com/privacy.html` in the app's
      language
- [ ] Version number is correct

## Log

| Date | Version | Device | Result | Notes |
|---|---|---|---|---|
| 2026-09-26 | 1.1.0 (debug) | Galaxy A51, Android 13 | 3, 5 (partial) | From the gallery: GS1 DataMatrix (valid and expired), links with `@` and Cyrillic letters, confirmation dialog (IP address link only in unit tests); zoom button 1→2→4→1; label check with uiautomator is clean. Camera and sections 1, 2, 4 still to be run with a user. |
| 2026-09-26 | 1.1.0 (debug) | Galaxy A51, Android 13 | 2 (partial), 3, 6 | Camera opens (CameraX id 0 OPEN), flashlight shows TORCH in the camera service, zoom ratio comes from CameraX; reading real codes with the camera was verified by the user in v1.0, and the decoder has not changed. From the gallery in the English UI: medicine code (expiry warning), `@` link, Wi-Fi, book, vCard 3 contact card (title, address): "Add to contacts" opens the contacts form with every field filled; exited without saving (no entry in contacts). Status bar icons on the scanner are light-colored. Store screenshots are in `store/screenshots/en/`. |
