# How to get a Gemini API key

GalleriaIDA uses Google's Gemini AI to generate quiz questions, translate the app interface, and create images. You need a free API key from Google AI Studio to use these features.

---

## Step-by-step

### 1. Go to Google AI Studio

Open your browser and go to **[aistudio.google.com](https://aistudio.google.com)**.

You will need a Google account (Gmail). If you don't have one, create it for free at [accounts.google.com](https://accounts.google.com).

<!-- SCREENSHOT: Google AI Studio homepage showing the "Get API key" button in the left sidebar -->

---

### 2. Sign in

Click **Sign in** and log in with your Google account.

---

### 3. Create an API key

1. In the left sidebar, click **Get API key**
2. Click **Create API key**
3. Choose **Create API key in new project** (recommended) or select an existing project if you have one

<!-- SCREENSHOT: The "Create API key" dialog in Google AI Studio -->

4. Your API key will be displayed — it looks like this:
   ```
   AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```
5. Click the **copy** icon to copy it

> ⚠️ **Keep your API key private.** Do not share it or post it publicly. Anyone with your key can use your quota.

---

### 4. Enter the key in GalleriaIDA

1. Open GalleriaIDA and tap the **⚙️** icon (top-right of the player selection screen)
2. Paste your key in the **Gemini API Key** field
3. Tap **Test & Save**
4. If the key is valid, the model selectors will populate automatically

---

## Free tier limits

The free tier of Gemini is sufficient for normal classroom use. Limits as of 2025:

| Model | Requests per minute | Requests per day |
|---|---|---|
| Gemini 2.0 Flash | 15 | 1,500 |
| Gemini 1.5 Flash | 15 | 1,500 |

If the app shows a "server is busy" message, you have hit the per-minute limit — wait a moment and try again.

For higher limits, Google offers paid plans via [Google Cloud](https://cloud.google.com/vertex-ai).

---

## Troubleshooting

**"API key not valid"** — make sure you copied the full key with no extra spaces.

**"The server is busy"** — you have hit the free-tier rate limit. Wait 60 seconds and try again.

**Models not loading** — check your internet connection, then tap Test & Save again.
