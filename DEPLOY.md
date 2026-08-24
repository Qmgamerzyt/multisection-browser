# GitHub Deployment Guide — MultiSection Browser

**Repository:** `multisection-browser`  
**Workflow:** `.github/workflows/build.yml`  
**Artifact:** `app-debug.apk`

---

## STEP 1 — CREATE THE REPO ON GITHUB (one time)

1. Open **github.com** in your phone browser → Sign in
2. Tap **+** (top right) → **New repository**
3. Repository name: `multisection-browser`
4. Visibility: **Public** (or Private — Actions works on both)
5. **Do NOT** check "Add a README", ".gitignore", or "License" — we already have them
6. Tap **Create repository**
7. Copy the repo URL shown (looks like `https://github.com/YOURUSER/multisection-browser.git`)

---

## STEP 2 — PUSH FROM THIS DEVICE (run once)

Run these commands in Termux / terminal on this device:

```bash
cd /root/multisection-browser
git remote add origin https://github.com/YOURUSER/multisection-browser.git
git branch -M main
git push -u origin main
```

Replace `YOURUSER` with your GitHub username.

> If prompted for credentials: use your GitHub username + a **Personal Access Token (classic)** with `repo` scope as password.  
> Settings → Developer settings → Personal access tokens → Generate new token (classic).

---

## STEP 3 — WATCH THE BUILD RUN

1. Open the repo on GitHub: `https://github.com/YOURUSER/multisection-browser`
2. Tap **Actions** tab (top menu)
3. You'll see **"Build APK"** workflow running (triggered by your push)
4. Tap the run → watch the steps:
   - `Set up JDK 17` ✓
   - `Setup Android SDK` ✓
   - `Cache Gradle` ✓
   - `Build Debug APK` ← this takes 3–6 minutes
   - `Upload APK artifact` ✓
5. Wait for **green checkmark** ✅ on the workflow

---

## STEP 4 — DOWNLOAD THE APK

When the run shows **green checkmark**:

1. In the Actions run page, scroll to bottom → **Artifacts**
2. Tap **app-debug** (expands)
3. Tap **app-debug.apk** → downloads to your phone
   - File name: `app-debug.apk` (~60–80 MB)
   - Save location: usually `Download/` folder

---

## STEP 5 — INSTALL ON PHONE

1. Open **Files** / **My Files** app → go to `Download/`
2. Tap `app-debug.apk`
3. If prompted: **"Install unknown apps"** → tap **Settings** → allow your file manager/browser → go back
4. Tap **Install** → **Open** when done

The app icon is a purple square with a white star. Launch it — you'll see:
- **Session Switcher** at top (tap to create/switch sessions)
- **Tab Bar** below it
- **Omnibox** (URL/search + menu button for script runner)
- **Floating ball** (bottom-right) — drag to move, tap to hide/show header

---

## TEST THE ISOLATION (Discord A vs B)

1. In **Session A** (default): open Discord → log in
2. Tap floating ball → header hides (tap again to show)
3. Tap **Session Switcher** → **+** → name it "Session B"
4. Switch to **Session B** → open Discord → you see **login page** (not logged in) ✅

---

## RE-RUN MANUALLY (any time)

1. Actions tab → **Build APK** (left sidebar)
2. Tap **Run workflow** (right side) → **Run workflow** button
3. Wait for green check → download new APK

---

## TROUBLESHOOTING

| Problem | Fix |
|---------|-----|
| Build fails | Open the failed run → tap the red step → read logs. Common: dependency version mismatch. |
| "No artifact" | Build didn't complete. Check the run logs. |
| APK won't install | Enable "Unknown sources" for your file manager. Delete old app first. |
| Push rejected | `git pull --rebase origin main` then `git push` |

---

## FILES IN THIS REPO

```
multisection-browser/
├── .github/workflows/build.yml    # ← Cloud build definition
├── app/
│   ├── build.gradle.kts           # Dependencies, compileSdk 34, GeckoView 129
│   └── src/main/...               # Kotlin + Compose source
├── gradle/wrapper/                # Gradle 8.5 wrapper (committed)
├── gradlew                        # Unix wrapper script
├── gradlew.bat                    # Windows wrapper script
├── settings.gradle.kts
├── build.gradle.kts               # AGP 8.3.0, Kotlin 1.9.22
├── gradle.properties
├── .gitignore
├── REPORT.md                      # Full architecture doc
└── DEPLOY.md                      # This file
```

---

## VERSION PINS (why it builds on cloud)

| Tool | Version | Why |
|------|---------|-----|
| Gradle | 8.5 | Wrapper committed, works on JDK 17 |
| AGP | 8.3.0 | Compatible with Gradle 8.5, compileSdk 34 |
| Kotlin | 1.9.22 | Stable with Compose BOM 2024.02.00 |
| Compose BOM | 2024.02.00 | Matches Kotlin 1.9.x |
| GeckoView | 129.0.20240819150008 | Last version supporting compileSdk 34 |
| Java | 17 (Temurin) | `setup-java@v4` provides this |

---

## NO SECRETS IN CODE

- `secrets.GITHUB_TOKEN` is used automatically by `actions/upload-artifact`
- No personal tokens in any file
- If you ever need a PAT, add it in **Repo Settings → Secrets → Actions → GH_PAT**

---

## DONE

After Step 5, you have a working MultiSection Browser APK built in the cloud, installed on your phone, with full session isolation.