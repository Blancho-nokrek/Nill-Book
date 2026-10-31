# NillBook (Android)

Community-powered caller directory and smart contact manager for Bangladesh and international numbers, written in Kotlin with Jetpack Compose and Material 3.

## Features

- **Smart Directory Search**:
  - Search by phone number with country code support (+880 Bangladesh, +91 India, +92 Pakistan, +977 Nepal, +94 Sri Lanka, +971 UAE, +966 Saudi Arabia, +60 Malaysia, +65 Singapore, +44 UK, +1 US).
  - Search by name with fuzzy Levenshtein word-matching algorithm.
  - Caller identification with community crowdsource counts ("saved by X people") and verified registration badges.
  - Number privacy masking (`+880 ••••••••00`) with unmasking for mutual friends, public numbers, or owner.

- **My Contacts Manager**:
  - View, filter, add, edit, and delete personal contacts.
  - Contact fields: Name, phone, email, address, and date of birth.
  - Full `.vcf` / vCard parser to import phonebook cards directly.
  - Mandatory caller directory consent controls.

- **Caller Network & Friendships**:
  - Send, accept, and decline connection requests.
  - Connected friends can view each other's full verified phone numbers.
  - Instant dialer integration (`Intent.ACTION_DIAL`).

- **Identity & Privacy Controls**:
  - User profile management (name, phone, gender, birthday).
  - Public caller number visibility toggle to control whether your phone number is exposed in search results.
  - Fast demo user switching and full authentication flows (login and registration with password strength meter).

## Architecture

- **UI Framework**: Modern Jetpack Compose with Material 3 styling and Edge-to-Edge display support.
- **Theme**: NILLBOOK slate and cyan dark theme palette.
- **Language**: 100% Kotlin with Coroutines and StateFlow reactive state management.
