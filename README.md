# New Font 1 — Android drawing prototype

A native Android starter app built with Kotlin and Jetpack Compose. Select a character, draw it with your finger, choose a brush color and size, undo the latest stroke, or clear the canvas. Drawings for different characters are kept separately while the app remains open.

## Build an APK from your phone (no Termux needed)

1. Open your GitHub repository: https://github.com/jyotiverma6896-ux/New-font1
2. Tap **Add file** → **Upload files**.
3. Upload the contents of this ZIP, keeping the folder paths exactly as shown. On mobile, you may need to extract the ZIP first using your phone's Files app.
4. Scroll down and tap **Commit changes**.
5. Open the repository's **Actions** tab.
6. Choose **Build New Font 1 APK**, then **Run workflow**.
7. When the run completes successfully, open it and download the **new-font1-debug-apk** artifact.
8. Extract the downloaded artifact ZIP; install `app-debug.apk` on your phone. Android may ask you to allow installs from your browser/files app.

## Current limits

This is a starter prototype, not a complete Fontmaker clone. Drawings are not saved after closing the app, and real TTF/OTF font-file generation is not implemented yet.
