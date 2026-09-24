# AR Project — Cloudflare YouTube AI Automation

This directory contains the Cloudflare-hosted YouTube Shorts automation service for the AR Project repository.

## Architecture

OpenAI Responses API
  -> content plan and script
Cloudflare Workers AI
  -> AI-generated video with audio
Cloudflare R2
  -> durable media storage
Cloudflare Media Transformations
  -> 1080x1920 portrait MP4 normalization
Cloudflare Workflows
  -> scheduled, durable execution
YouTube Data API + Google OAuth
  -> channel upload

The laptop is not part of the production runtime. Once deployed, the scheduled Workflow runs on Cloudflare infrastructure.

## What is already in this repository

- Cloudflare Worker entrypoint in src/index.ts
- Scheduled Workflow
- D1 migration
- R2 bucket binding
- Workers AI binding
- Media Transformations binding
- YouTube OAuth flow
- Encrypted YouTube refresh-token storage in D1
- OpenAI content generation
- Workers AI video generation
- YouTube resumable video upload
- Minimal protected dashboard
- Manual pipeline trigger
- Job history and error state

## Important first-run defaults

The configuration starts with:

- niche: technology
- video duration: 15 seconds
- YouTube upload visibility: PRIVATE
- automatic public publishing: OFF
- daily schedule: 17:00 UTC

Do not turn on public publishing until the private test succeeds.

## 1. Install Wrangler

Use Node.js 20+ or a current supported Node release.

From this directory:

    npm install

Check the installed version:

    npx wrangler --version

The Workflow schedule configuration requires a current Wrangler release. Cloudflare's current Workflow documentation supports schedules directly on the Workflow binding.

## 2. Log in to Cloudflare

Run:

    npx wrangler login

A browser window will open for Cloudflare authentication.

## 3. Create the D1 database

Run:

    npx wrangler d1 create ar-project-youtube

Copy the returned database ID.

Open wrangler.jsonc and replace:

    REPLACE_WITH_D1_DATABASE_ID

with that real ID.

Then apply the migration remotely:

    npx wrangler d1 migrations apply ar-project-youtube --remote

## 4. Create the R2 bucket

Run:

    npx wrangler r2 bucket create ar-project-youtube-media

The bucket name must match ar-project-youtube-media in wrangler.jsonc.

## 5. Configure the required secrets

Never put these values in GitHub or in wrangler.jsonc.

Generate the encryption key:

    openssl rand -base64 32

Create the secrets:

    npx wrangler secret put OPENAI_API_KEY
    npx wrangler secret put GOOGLE_CLIENT_ID
    npx wrangler secret put GOOGLE_CLIENT_SECRET
    npx wrangler secret put TOKEN_ENCRYPTION_KEY
    npx wrangler secret put ADMIN_TOKEN

TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes.

Use a long random value for ADMIN_TOKEN.

## 6. Create the Google OAuth client

In Google Cloud Console:

1. Create or select a Google Cloud project.
2. Enable YouTube Data API v3.
3. Configure the OAuth consent screen.
4. Create an OAuth 2.0 client of type Web application.
5. Add this exact redirect URI after deployment:

    https://YOUR-WORKER-DOMAIN/auth/youtube/callback

The redirect URI must exactly match the deployed Worker origin and path.

The Worker requests only:

    https://www.googleapis.com/auth/youtube.upload

Do not give the Worker your Google password.

## 7. Deploy

From youtube-automation:

    npx wrangler deploy

After deployment, open the Worker URL in a browser.

Then open:

    https://YOUR-WORKER-DOMAIN/auth/youtube

Approve the YouTube upload permission.

The refresh token is encrypted with AES-GCM and stored in D1. The token is never committed to Git.

## 8. Test with a private Short

The default configuration has:

    AUTO_PUBLISH = false

From the dashboard, enter your ADMIN_TOKEN and start a test job.

Or use:

    curl -X POST https://YOUR-WORKER-DOMAIN/api/run -H "Authorization: Bearer YOUR_ADMIN_TOKEN" -H "Content-Type: application/json" -d "{}"

Check recent jobs:

    curl https://YOUR-WORKER-DOMAIN/api/jobs -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

Check a specific Workflow:

    curl "https://YOUR-WORKER-DOMAIN/api/status?id=WORKFLOW_ID" -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

The expected result is a YouTube upload with private visibility.

## 9. Automatic daily generation

The Workflow is configured with:

    "schedules": ["0 17 * * *"]

Cloudflare cron schedules are UTC. Tunisia is UTC+1 in this setup, so this is intended to run at 18:00 Africa/Tunis.

Change the cron expression in wrangler.jsonc when you want a different schedule.

Because the schedule is attached directly to the Workflow binding, each matching trigger creates a new durable Workflow instance.

## 10. Enable public publishing

Only after the private upload is confirmed:

Change:

    "AUTO_PUBLISH": "false"

to:

    "AUTO_PUBLISH": "true"

and redeploy.

Google may restrict uploads from unverified API projects to private viewing until the project's required audit/verification is completed. Treat public publishing as dependent on Google's current project state.

## 11. Change the content niche

Change:

    "CONTENT_NICHE": "technology"

Examples:

    "CONTENT_NICHE": "cybersecurity"
    "CONTENT_NICHE": "space"
    "CONTENT_NICHE": "history"

The content prompt intentionally favors evergreen subjects so the system does not silently invent fresh facts.

## 12. Video generation

The default Workers AI model is:

    black-forest-labs/flux-3-video

The Worker requests:

- text-to-video
- HD output
- 5–20 second duration
- generated audio
- portrait-friendly composition

The model's returned signed video URL is copied into R2 immediately because signed URLs are temporary.

## 13. Portrait formatting

Media Transformations changes the stored video to:

- width: 1080
- height: 1920
- fit: cover
- MP4 video output
- preserved audio

This gives the YouTube Shorts pipeline a consistent portrait output.

## 14. Current limitation of the MVP

The Workers AI video model generates the audiovisual clip and the OpenAI API creates the spoken script. The MVP does not yet synthesize that exact script with a separate TTS track and mux it into the video.

That means the generated audio is model-generated rather than guaranteed word-for-word narration of the OpenAI script.

The architecture is deliberately modular so an exact TTS + render stage can be added later without changing the YouTube or D1 layers.

## 15. Security

Never commit:

- .env
- .dev.vars
- Google OAuth client secrets
- OAuth refresh tokens
- API keys
- ADMIN_TOKEN

The API routes are protected by Authorization: Bearer ADMIN_TOKEN.

The OAuth state is single-use and expires after ten minutes.

The YouTube refresh token is encrypted before D1 storage.

## 16. Costs

Possible usage charges include:

- OpenAI API usage
- Workers AI video generation
- R2 storage and operations
- Media Transformations after any applicable beta/free allowance
- YouTube API quota limitations

Check current provider pricing before turning on high-volume generation.

## Useful endpoints

GET /
Dashboard

GET /health
Health check

GET /auth/youtube
Start Google OAuth

GET /auth/youtube/callback
OAuth callback

POST /api/run
Start a Workflow

GET /api/status?id=...
Workflow status

GET /api/jobs
Recent jobs

GET /api/youtube/status
Connected channel information

All /api/* endpoints require the admin bearer token.

## Development

    npm install
    npx wrangler types
    npm run check
    npx wrangler dev

The Media Transformations binding is not locally simulated; use remote binding mode for local development when needed.

## Production principle

This project is designed so that the computer that edits this Git repository does not have to stay on.

GitHub contains the source.
Cloudflare runs the Worker and Workflow.
D1 stores state.
R2 stores video files.
Workers AI generates the video.
YouTube stores the published content.
