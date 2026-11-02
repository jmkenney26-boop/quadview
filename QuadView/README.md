# Quad View

Watch up to 4 apps at once on an Android TV box (built for the vSeeBox V6 Plus).
Pick a layout, pick an app for each window, press **Launch**. A second home-screen
icon, **Quad View ▶ Go**, relaunches your last setup in one click.

Layouts: 2 × 2 Grid · 1 Big + 3 Side · 1 Big + 3 Bottom · 1 Big + 2 Side · Side by Side (2)

---

## Part 1 — Get the APK built by GitHub (on your Windows PC, ~10 min, one time)

1. Extract **QuadView.zip** somewhere easy, like your Desktop. You'll get a `QuadView` folder.
2. Go to **github.com** and create a free account (or sign in).
3. Click **+** (top right) → **New repository**.
   - Name: `QuadView`
   - Choose **Public** (so the box can download the APK without signing in)
   - Click **Create repository**.
4. On the new, empty repository page, click the link **"uploading an existing file"**.
5. Open the extracted `QuadView` folder in File Explorer, press **Ctrl+A** to select
   everything inside it (including the `.github` folder), and drag it all onto the
   GitHub page. Wait for the files to finish uploading, then click **Commit changes**.
6. Check the file list on GitHub shows a **.github** folder. If it doesn't, see
   *"If the .github folder is missing"* below.
7. Click the **Actions** tab. If GitHub asks, click the button to enable workflows.
   You'll see **Build APK** running. After ~5 minutes it turns into a green check.
   (If it doesn't start by itself, click **Build APK** → **Run workflow**.)

Your APK now lives at this permanent link (swap in your GitHub username):

```
https://github.com/YOUR-USERNAME/QuadView/releases/latest/download/QuadView.apk
```

### If the .github folder is missing
On your repository page: **Add file → Create new file**. For the file name type
`.github/workflows/build.yml` (typing the slashes creates the folders). Open
`QuadView\.github\workflows\build.yml` from the zip in Notepad, copy everything,
paste it into GitHub, and click **Commit changes**. Then continue at step 7.

---

## Part 2 — Install it on the box

1. On the box, install the free **Downloader** app (by AFTVnews) if you don't have it.
2. Settings → Apps (or Security) → allow **Install unknown apps** for Downloader.
3. In Downloader, type the APK link from Part 1 and press **Go**, then **Install**.

## Part 3 — Turn on floating windows (one time)

1. Settings → Device Preferences / System → **About** → click **Build number** 7 times.
2. Settings → **Developer options** → turn on:
   - **Force activities to be resizable**
   - **Enable freeform windows** (may be called *Freeform window support*)
   - **Force desktop mode**, if your box has it
3. **Restart the box.**

Open Quad View. The **Setup check** line at the top turns green when freeform is on.

If your box has no freeform toggle, it can usually be switched on from a PC with ADB:

```
adb connect BOX-IP-ADDRESS
adb shell settings put global enable_freeform_support 1
adb shell settings put global force_resizable_activities 1
adb reboot
```

---

## Using it

- A mouse or air-mouse remote makes it much easier to click inside each window.
- All windows play sound. Mute every app except the one you want to hear.
- If an app opens full screen instead of in its window, close it from Recents and launch again.
- A black window with sound means that app blocks windowed playback. Use a different app there.
- If playback stutters, lower the video quality inside each app or use wired Ethernet.

## Changing the app later

Edit any file on GitHub and commit. GitHub rebuilds automatically, and the same
download link always gives you the newest APK.
