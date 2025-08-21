📱 NFC Keyring – Android MVP Roadmap
1. Foundations & Research

✅ Confirm target use cases:

General NFC tags (NDEF, WiFi, URLs, vCards).

Simple MIFARE Classic/Ultralight tags (if allowed).

Exclude banking/secure transit cards (requires secure element).

✅ Review legal constraints in your jurisdiction (to avoid cloning protected cards).

✅ Decide on distribution model: personal use vs Play Store app.

2. Tech Stack

Frontend (Mobile App): Android (Kotlin preferred, or Java).

NFC APIs: android.nfc.* for reading/writing; android.nfc.cardemulation.HostApduService for HCE.

Local Storage: Encrypted DB (SQLCipher or Android EncryptedSharedPreferences).

Security: Biometric unlock (Fingerprint/Face ID).

Optional Cloud Backup: Firebase / Supabase (encrypt before upload).

3. Core Features (MVP Scope)
A. Tag Reading

Detect NFC tag when phone is tapped.

Extract: UID + payload (if NDEF or supported format).

Show preview (e.g. “WiFi Tag”, “vCard”, “Custom Data”).

B. Storage

Save tag data locally in encrypted DB.

Allow user to rename/tag entries (e.g. “Office Door”, “Gym Locker”).

Biometric gate before access.

C. Simulation / Emulation

Use HCE (Host Card Emulation) to emulate supported tag types.

User selects a saved tag → phone acts as card until stopped.

Implement “tap to stop” safety.

D. UI/UX

Minimal clean design:

Home = list of saved tags.

+ Add Tag = scan new.

Details Screen = view/edit/activate emulation.

4. Architecture

Layers:

UI Layer → Jetpack Compose / XML UI.

NFC Layer → Handles NfcAdapter events.

Storage Layer → Room DB (encrypted).

Emulation Layer → HostApduService implementation.

Security Flow: App launch → biometric check → unlock DB → show tags.

5. Development Milestones

Project Setup

Android project, NFC permissions in manifest.

Test simple NFC read event.

NFC Tag Reader

Implement foreground dispatch to capture NFC intent.

Parse NDEF messages.

Display raw + formatted info.

Secure Storage

Add Room DB with encryption.

Store tag ID + payload + label.

Add biometric lock.

Tag Emulation (HCE)

Implement HostApduService.

Hardcode a test AID + APDU response first.

Then map stored tag data to APDU response.

UI

Build list of saved tags.

Add “Tap to Emulate” action.

Add simple visual indicator (active tag highlighted).

Testing

Test reading/writing cheap NFC tags.

Test HCE with another NFC reader (Android phone or USB NFC reader).

6. Stretch Goals (Post-MVP)

🔄 Cloud Backup (encrypted).

📡 Share Tag (QR code / export).

🎨 Better UI/UX (tag icons, categories).

🔒 Advanced security (passcode fallback, secure enclave).

📊 Usage logs (“last tapped at…”)

7. Risks & Mitigation

⚠️ iOS Emulation Not Possible → Keep iOS as “read/store only.”

⚠️ Protected Tags → Some tags won’t be clonable (secure element required).

⚠️ Legal Restrictions → Must market as “for personal NFC tags” (not cloning bank/transit cards).

✅ By the end of MVP, you’ll have:

An Android app that can read, store, and emulate supported NFC tags.

A secure digital keyring experience for generic tags.