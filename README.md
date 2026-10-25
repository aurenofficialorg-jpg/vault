# VAULT (prototype v0.3) — security model
- Data file: AES-256-GCM (platform crypto), random IV, tamper-checked. Atomic writes.
- Key hierarchy: hardware Keystore key (StrongBox when available), auth required on EVERY use -> wraps a random data key held in RAM only while unlocked, zeroed on lock.
- Locks on leaving the app and after idle time. Screenshots/recents blocked (FLAG_SECURE).
- No INTERNET permission, no cloud/device backup, cleartext traffic off, logs stripped in release.
- Reveal of identifiers needs fresh authentication and auto-hides after 15 s.
- Recovery key (52 chars, shown once) re-opens the vault after a biometric change. Files you add are individually AES-GCM encrypted; previews decrypt in memory only.
- NOT covered: rooted/compromised OS, malware with screen access, lost recovery (no recovery key yet), shoulder surfing.
- Data in this build is FICTIONAL. Get an independent security review before storing real data.
