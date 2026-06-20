# How to get a Pollinations API key

GalleriaIDA uses [Pollinations](https://pollinations.ai) as a fallback image generation service. Using it without an API key works fine on the free public tier, but an API key gives you higher limits and priority access.

> **This step is optional.** You can leave the Pollinations API key field blank in Settings and the app will still generate images using Pollinations' free public tier.

---

## Free tier (no key needed)

Pollinations offers free, unlimited image generation without an account. The free tier is shared across all users, so during busy periods image generation may be slower or occasionally fail — in which case GalleriaIDA automatically tries the next fallback model.

---

## Getting an API key (optional)

### 1. Go to Pollinations

Open your browser and go to **[pollinations.ai](https://pollinations.ai)**.

<!-- SCREENSHOT: Pollinations homepage showing the "Get API access" or sign-up section -->

---

### 2. Request API access

As of 2025, Pollinations API access is granted on request rather than via instant self-serve signup.

1. Go to **[pollinations.ai/join](https://pollinations.ai/join)** or look for the **"For developers"** / **"API access"** link on the homepage
2. Fill in the request form with your name, use case, and expected usage
3. You will receive your API key by email once approved

Alternatively, join the [Pollinations Discord server](https://discord.gg/pollinations) where API keys are often distributed directly to community members.

---

### 3. Enter the key in GalleriaIDA

1. Open GalleriaIDA and tap the **⚙️** icon (top-right of the player selection screen)
2. Paste your key in the **Pollinations API Key** field
3. Tap **Save**

---

## What the API key changes

| | Without key | With key |
|---|---|---|
| Image generation | ✅ Works | ✅ Works |
| Speed | Standard (shared) | Faster (priority) |
| Rate limits | Shared public limits | Higher personal limits |
| Cost | Free | Free |

---

## Troubleshooting

**Images fail to generate** — if both Gemini and all Pollinations fallback models fail, check your internet connection. The app will display which model it is currently trying.

**Key not accepted** — make sure there are no extra spaces when pasting the key into Settings.
