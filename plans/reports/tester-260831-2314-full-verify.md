# Full Verification Report — Uncommitted Changes
**Date:** 2026-08-31 23:24  
**Scope:** All uncommitted changes in web/ and android/

---

## Web Verification Results

### 1. Unit Tests (vitest)
- **Command:** `npx vitest run`
- **Status:** ✓ PASS
- **Result:** 141 tests passed, 0 failed
  - Test files: 10
  - Duration: 2.53s

### 2. Linting (eslint)
- **Command:** `npx eslint .`
- **Status:** ✓ PASS
- **Errors:** 0

### 3. Type Checking (svelte-check)
- **Command:** `npx svelte-check --tsconfig ./jsconfig.json`
- **Status:** ✓ PASS
- **Checked:** 429 files
- **Errors:** 0
- **Warnings:** 0

### 4. Build (BUILD_PROFILE=gh)
- **Command:** `BUILD_PROFILE=gh npx vite build`
- **Status:** ✓ PASS
- **Duration:** 2.20s
- **PWA Build Check:** ✓ PASS (base "/loto", 13 client JS files, 92 precached audio entries)

### 5. Build (default)
- **Command:** `npx vite build`
- **Status:** ✓ PASS
- **Duration:** 2.18s
- **PWA Build Check:** ✓ PASS (base "", 13 client JS files, 92 precached audio entries)

---

## Android Verification Results

### 1. Unit Tests (testDebugUnitTest)
- **Command:** `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew :app:testDebugUnitTest`
- **Status:** ✓ PASS
- **Result:** 120 tests passed, 0 failed
  - Packages: 6
  - Test classes: 12
  - Duration: 4.629s
  - Success rate: 100%

**Test Breakdown by Package:**
- com.miti99.loto.audio: 13 tests (0.032s)
- com.miti99.loto.game: 39 tests (4.240s)
- com.miti99.loto.settings: 19 tests (0.125s)
- com.miti99.loto.state: 41 tests (0.231s)
- com.miti99.loto.ui: 3 tests (0s)
- com.miti99.loto.ui.master: 5 tests (0.001s)

### 2. Lint Check (lintDebug)
- **Command:** `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew :app:lintDebug`
- **Status:** ✓ PASS
- **Build Result:** BUILD SUCCESSFUL in 7s
- **Errors:** 0
- **Warnings:** 14 (all non-blocking)

**Warning Summary:**
- 1 API level compatibility warning (enableOnBackInvokedCallback @ API 33+)
- 10 dependency version upgrade suggestions (AGP, androidx libs, Kotlin)
- 2 plurals candidate warnings (Vietnamese string formatting)
- 1 typo detection (cosmetic)

---

## Summary

| Component | Status | Details |
|-----------|--------|---------|
| **Web Tests** | ✅ PASS | 141/141 tests |
| **Web Lint** | ✅ PASS | 0 errors |
| **Web Types** | ✅ PASS | 0 errors (429 files checked) |
| **Web Build (gh)** | ✅ PASS | PWA verified |
| **Web Build (default)** | ✅ PASS | PWA verified |
| **Android Tests** | ✅ PASS | 120/120 tests |
| **Android Lint** | ✅ PASS | 0 errors (14 warnings only) |

**Total:** 261 unit tests passing (141 web + 120 android)  
**Build Status:** Both web and android build successfully  
**Code Quality:** No errors, no blocking issues

---

Status: DONE
Summary: All verification checks passed — 141 web tests, 120 android tests, builds succeed, no errors detected.
Concerns/Blockers: None
