# 🌐 Publish Your App Landing Page For Free (GitHub Pages)

This guide provides step-by-step instructions to publish your brand-new, ultra-modern app landing page and host your APK file for users to download—**100% free of charge**!

---

## 🚀 Deployment Steps (GitHub Pages)

### Step 1: Export Your Project
1. Download your compiled application package (**APK**) from AI Studio:
   - Click the settings icon in the top right.
   - Click **Download APK** (or find your `app-release.apk` compiled file).
2. Export your project code as a **ZIP** from AI Studio (under the options/settings menu) or sync it directly to GitHub.

---

### Step 2: Set up a Free GitHub Repository
1. Go to [GitHub](https://github.com/) and log in (or create a free account).
2. Click **New Repository**.
3. Name your repository (e.g., `task-manager-app`).
4. Set visibility to **Public** (required for free GitHub Pages hosting).
5. Choose **Create Repository**.

---

### Step 3: Upload Website and APK Files
1. In your new repository on GitHub, click **uploading an existing file** or use Git to push your workspace.
2. Drag and drop the following files into the repository:
   - `/website/index.html` (Rename this or move it to the **root** of your repository so it resides directly at `index.html`).
   - Your compiled APK file (Rename it exactly to **`app-release.apk`** and upload it to the root of your repository so the download button works automatically).
3. Scroll down and click **Commit changes**.

---

### Step 4: Turn on GitHub Pages
1. On your GitHub repository page, click the **Settings** tab at the top.
2. In the left-hand sidebar, click **Pages** (under the "Code and automation" section).
3. Under **Build and deployment**:
   - Set **Source** to **Deploy from a branch**.
   - Under **Branch**, change `None` to **`main`** (or `master`) and select `/ (root)` folder.
4. Click **Save**.

---

### ⏳ That's It! Your Site is Online!
* GitHub will take **1-2 minutes** to build and launch your site.
* Refresh the **Settings > Pages** screen to see your live URL (e.g., `https://username.github.io/task-manager-app/`).
* Share your link with friends! Anyone opening the URL can instantly download and install your Task Manager APK with a single click.

---

## 💡 Alternatives (Netlify / Vercel)
If you prefer not to use GitHub, you can host your files on **Netlify** or **Vercel** with pure drag-and-drop:
1. Go to [Netlify Drop](https://app.netlify.com/drop) (no account needed initially!).
2. Place `index.html` and your `app-release.apk` inside a folder on your computer.
3. Drag that entire folder and drop it into the Netlify Drop webpage.
4. Your website and APK download link will be live instantly with a shareable URL!
