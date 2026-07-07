# How to get a ModelScope API key

GalleriaIDA can use [ModelScope](https://modelscope.ai) (Alibaba's model platform) as a free image generation service. ModelScope gives you access to high-quality models like Qwen-Image with a generous free daily allowance and no credit card required.

---

## Getting an API key

### 1. Go to ModelScope

Open your browser and go to **[modelscope.ai](https://modelscope.ai)**.

Click **Sign in** (top right) and choose a sign-in method. Opt to **Sign in with GitHub** when creating your ModelScope account. If you don't have a GitHub account, go to [github.com](https://github.com) and create one using **Sign up with Google** — that way you don't need to create separate accounts and everything stays linked to your Google account.

<img src="images/ModelScope-Sign-In.jpg" width="500" alt="ModelScope Sign In">

---

### 2. Link your Alibaba Cloud account

ModelScope requires a linked Alibaba Cloud account before the API will work. This is a one-time step and does **not** require a credit card.

1. Go to **[modelscope.ai/my/settings/account](https://modelscope.ai/my/settings/account)**
2. Find the **Alibaba Cloud Account** section and click **Bind**
3. If you don't have an Alibaba Cloud account yet, sign up for free at [account.alibabacloud.com](https://account.alibabacloud.com/register/intl_register.htm), then return and complete the binding

<img src="images/ModelScope-Bind-Alibaba.jpg" width="850" alt="Bind Alibaba Cloud Account">

> ⚠️ If you skip this step, the API will return a message asking you to bind your Alibaba Cloud account before use.

---

### 3. Create an API token

1. Go to **[modelscope.ai/my/access/token](https://modelscope.ai/my/access/token)**
2. Click **Create new token**
3. Fill out the **Name** (eg. Galleria Ida API Key)

<img src="images/ModelScope-Create-API-Key-Form.jpg" width="500" alt="Create API Key Form">

4. Your API token will be displayed
5. Click the **copy** icon to copy it

> ⚠️ **Keep your API key private.** Do not share it or post it publicly. Anyone with your key can use your quota.

The limits per model can be found [here](https://modelscope.ai/docs/model-service/API-Inference/limits).

---

### 4. Enter the key in GalleriaIDA

1. Open GalleriaIDA and tap the **⚙️** icon (bottom-right on most screens)
2. Paste your key in the **ModelScope API Key** field
3. Tap **Test & Save**

> ⚠️ Depending on server load, image creation may take **up to a minute**. Please be patient and keep the app open until the image appears.

---

## Free tier limits

- **2,000 API calls per day** across all models
- **500 calls per day** per individual model
- No credit card required — free for personal, non-commercial use
- Recommended model: **Qwen-Image-2512** (best quality)

> ℹ️ ModelScope's servers are based in Asia, so image generation may be slightly slower than a local service, but the free allowance is far more than enough for everyday use.

---

## Troubleshooting

**"Invalid model id"** — make sure the model name is spelled exactly, e.g. `Qwen/Qwen-Image-2512`.

**"Please bind your Alibaba Cloud account"** — go back to Step 2 and link your Alibaba Cloud account.

**Key not accepted** — make sure there are no extra spaces when pasting the key into Settings.

**Images fail to generate** — check your internet connection. The app will display which model it is currently trying.
