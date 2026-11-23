# PhoneGuard

A small Android app that checks your phone for real signs of compromise and lets you
back up your contacts in one tap. It does **not** monitor anyone else's device and
only reads settings on the phone it's installed on.

**What it checks:**
- Apps holding an unusually large bundle of sensitive permissions (camera, mic, SMS, location)
- Unknown device administrator apps (a common way attackers keep control of a phone)
- Unknown accessibility services (commonly abused by stalkerware to read your screen)
- Whether installing apps from outside the Play Store is allowed
- How old your security patch is

**Honesty note:** this cannot detect every possible spyware tool (some hide very well),
and it cannot recover data that was never backed up before a factory reset. Use it
alongside, not instead of, the account-recovery and police steps already discussed.

## Option A — Build it yourself with Android Studio (most control)
1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open this folder (`File > Open`) and let it sync — Android Studio will download
   Gradle automatically the first time, using the version pinned in
   `gradle/wrapper/gradle-wrapper.properties`.
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. Find the APK in `app/build/outputs/apk/debug/app-debug.apk` and copy it to your phone to install.

## Option B — Let GitHub build it for you (no software to install)
1. Create a free GitHub account if you don't have one, and create a new empty repository.
2. Upload every file in this folder to that repository (GitHub's web "Upload files" button works for this — drag the whole folder in).
3. Go to the repo's **Actions** tab — a workflow called "Build APK" will run automatically.
4. When it finishes (green checkmark), open that run and download the **PhoneGuard-debug-apk** artifact — it's a zip containing your `.apk`.
5. Transfer that `.apk` to your phone and tap it to install (you'll need to allow "install unknown apps" for whichever app you used to open it — Files, Chrome, etc.).

## Option C — A free online APK-build service
Services like `appetize.io`-style CI or any "build Android project online" tool can
also take this exact folder as a zip and return an APK. Option B (GitHub Actions) is
the most reliable free route and is already wired up, so I'd start there.

## Installing the APK on your phone
Since this isn't from the Play Store, Android will ask you to confirm "install unknown
apps" the first time — that's expected for any sideloaded app, not a warning sign by
itself.
