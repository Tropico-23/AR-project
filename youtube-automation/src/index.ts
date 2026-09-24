import { WorkflowEntrypoint } from "cloudflare:workers";

export interface Env {
  DB: D1Database;
  MEDIA_BUCKET: R2Bucket;
  AI: any;
  MEDIA: any;
  YOUTUBE_PIPELINE: any;

  OPENAI_API_KEY: string;
  GOOGLE_CLIENT_ID: string;
  GOOGLE_CLIENT_SECRET: string;
  TOKEN_ENCRYPTION_KEY: string;
  ADMIN_TOKEN: string;

  CONTENT_NICHE: string;
  OPENAI_MODEL: string;
  VIDEO_MODEL: string;
  VIDEO_DURATION_SECONDS: string;
  AUTO_PUBLISH: string;
  YOUTUBE_CATEGORY_ID: string;
  TIMEZONE: string;
}

type PipelineParams = {
  reason?: string;
  topic?: string;
};

type GeneratedContent = {
  topic: string;
  title: string;
  description: string;
  tags: string[];
  spoken_script: string;
  visual_prompt: string;
};

function json(data: unknown, status = 200): Response {
  return Response.json(data, {
    status,
    headers: { "Cache-Control": "no-store" }
  });
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function requireAdmin(request: Request, env: Env): Response | null {
  const expected = "Bearer " + env.ADMIN_TOKEN;
  return request.headers.get("authorization") === expected
    ? null
    : json({ error: "Unauthorized" }, 401);
}

function bytesToBase64(bytes: Uint8Array): string {
  let binary = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(
      ...bytes.subarray(i, Math.min(i + chunk, bytes.length))
    );
  }
  return btoa(binary);
}

function base64ToBytes(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  const binary = atob(padded);
  const result = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) result[i] = binary.charCodeAt(i);
  return result;
}

function base64Url(bytes: Uint8Array): string {
  return bytesToBase64(bytes)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

// Keep Web Crypto inputs typed as ArrayBuffer rather than ArrayBufferLike.
// Newer TypeScript lib definitions distinguish SharedArrayBuffer from ArrayBuffer.
function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
  return copy.buffer;
}

async function importEncryptionKey(env: Env): Promise<CryptoKey> {
  const raw = base64ToBytes(env.TOKEN_ENCRYPTION_KEY);
  if (raw.byteLength !== 32) {
    throw new Error("TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes.");
  }
  return crypto.subtle.importKey("raw", toArrayBuffer(raw), "AES-GCM", false, [
    "encrypt",
    "decrypt"
  ]);
}

async function encryptSecret(
  env: Env,
  plaintext: string
): Promise<{ iv: string; ciphertext: string }> {
  const key = await importEncryptionKey(env);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encrypted = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv: toArrayBuffer(iv) },
    key,
    toArrayBuffer(new TextEncoder().encode(plaintext))
  );

  return {
    iv: bytesToBase64(iv),
    ciphertext: bytesToBase64(new Uint8Array(encrypted))
  };
}

async function decryptSecret(
  env: Env,
  ivBase64: string,
  ciphertextBase64: string
): Promise<string> {
  const key = await importEncryptionKey(env);
  const decrypted = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: toArrayBuffer(base64ToBytes(ivBase64)) },
    key,
    toArrayBuffer(base64ToBytes(ciphertextBase64))
  );
  return new TextDecoder().decode(decrypted);
}

async function storeYoutubeRefreshToken(
  env: Env,
  token: string
): Promise<void> {
  const encrypted = await encryptSecret(env, token);
  await env.DB.prepare(
    "INSERT INTO secrets(name, ciphertext, iv) VALUES (?, ?, ?) " +
    "ON CONFLICT(name) DO UPDATE SET ciphertext=excluded.ciphertext, iv=excluded.iv"
  )
    .bind(
      "youtube_refresh_token",
      encrypted.ciphertext,
      encrypted.iv
    )
    .run();
}

async function getYoutubeRefreshToken(env: Env): Promise<string> {
  const row = await env.DB.prepare(
    "SELECT ciphertext, iv FROM secrets WHERE name = ?"
  )
    .bind("youtube_refresh_token")
    .first() as { ciphertext: string; iv: string } | null;

  if (!row?.ciphertext || !row?.iv) {
    throw new Error("YouTube is not connected. Visit /auth/youtube first.");
  }

  return decryptSecret(env, row.iv, row.ciphertext);
}

function getCallbackUrl(request: Request): string {
  const url = new URL(request.url);
  return url.origin + "/auth/youtube/callback";
}

async function createYoutubeOAuthUrl(
  request: Request,
  env: Env
): Promise<string> {
  const state = base64Url(crypto.getRandomValues(new Uint8Array(32)));
  const now = Math.floor(Date.now() / 1000);

  await env.DB.prepare(
    "INSERT INTO oauth_states(state, created_at, expires_at) VALUES (?, ?, ?)"
  )
    .bind(state, now, now + 600)
    .run();

  const params = new URLSearchParams({
    client_id: env.GOOGLE_CLIENT_ID,
    redirect_uri: getCallbackUrl(request),
    response_type: "code",
    access_type: "offline",
    prompt: "consent",
    include_granted_scopes: "true",
    scope: "https://www.googleapis.com/auth/youtube.upload",
    state
  });

  return "https://accounts.google.com/o/oauth2/v2/auth?" + params.toString();
}

async function exchangeYoutubeCode(
  request: Request,
  env: Env,
  code: string
): Promise<void> {
  const body = new URLSearchParams({
    code,
    client_id: env.GOOGLE_CLIENT_ID,
    client_secret: env.GOOGLE_CLIENT_SECRET,
    redirect_uri: getCallbackUrl(request),
    grant_type: "authorization_code"
  });

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });

  const data = (await response.json()) as any;

  if (!response.ok || !data.refresh_token) {
    throw new Error(
      "Google OAuth exchange failed: " + JSON.stringify(data)
    );
  }

  await storeYoutubeRefreshToken(env, data.refresh_token);
}

async function getYoutubeAccessToken(env: Env): Promise<string> {
  const refreshToken = await getYoutubeRefreshToken(env);

  const body = new URLSearchParams({
    client_id: env.GOOGLE_CLIENT_ID,
    client_secret: env.GOOGLE_CLIENT_SECRET,
    refresh_token: refreshToken,
    grant_type: "refresh_token"
  });

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });

  const data = (await response.json()) as any;

  if (!response.ok || !data.access_token) {
    throw new Error(
      "Google token refresh failed: " + JSON.stringify(data)
    );
  }

  return String(data.access_token);
}

function extractOpenAIText(data: any): string {
  if (typeof data.output_text === "string") return data.output_text;

  const parts: string[] = [];

  for (const item of data.output || []) {
    for (const entry of item.content || []) {
      if (typeof entry.text === "string") parts.push(entry.text);
    }
  }

  return parts.join("");
}

function parseGeneratedJson(text: string): GeneratedContent {
  const cleaned = text
    .trim()
    .replace(/^~~~json\s*/i, "")
    .replace(/^~~~\s*/i, "")
    .replace(/~~~$/i, "")
    .trim();

  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");

  if (start < 0 || end <= start) {
    throw new Error(
      "OpenAI returned non-JSON content: " + cleaned.slice(0, 500)
    );
  }

  const parsed = JSON.parse(
    cleaned.slice(start, end + 1)
  ) as Partial<GeneratedContent>;

  if (
    !parsed.topic ||
    !parsed.title ||
    !parsed.description ||
    !parsed.spoken_script ||
    !parsed.visual_prompt ||
    !Array.isArray(parsed.tags)
  ) {
    throw new Error("OpenAI response is missing required fields.");
  }

  return {
    topic: String(parsed.topic).trim().slice(0, 500),
    title: String(parsed.title).trim().slice(0, 100),
    description: String(parsed.description).trim().slice(0, 5000),
    tags: parsed.tags
      .map((tag) => String(tag).trim())
      .filter(Boolean)
      .slice(0, 15),
    spoken_script: String(parsed.spoken_script).trim().slice(0, 1600),
    visual_prompt: String(parsed.visual_prompt).trim().slice(0, 3000)
  };
}

async function generateContent(
  env: Env,
  params: PipelineParams
): Promise<GeneratedContent> {
  const duration = Math.max(
    5,
    Math.min(20, Number(env.VIDEO_DURATION_SECONDS || "15"))
  );

  const topicInstruction = params.topic
    ? "Use this requested topic: " + params.topic + "."
    : "Choose a useful evergreen topic.";

  const prompt =
    "Create one original YouTube Short for the niche " +
    env.CONTENT_NICHE +
    ". " +
    topicInstruction +
    " Target about " +
    duration +
    " seconds of spoken narration. " +
    "Use a strong first-sentence hook, one clear idea, and a concise ending. " +
    "Avoid made-up statistics, invented sources, unsafe instructions, political persuasion, " +
    "medical advice, and current-event claims that require live verification. " +
    "Return ONLY valid JSON with exactly these keys: topic, title, description, tags, spoken_script, visual_prompt. " +
    "The visual_prompt must describe cinematic vertical-friendly footage and must not request " +
    "logos, watermarks, or readable on-screen text.";

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      Authorization: "Bearer " + env.OPENAI_API_KEY,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: env.OPENAI_MODEL || "gpt-5.6-luna",
      input: [
        {
          role: "system",
          content: [
            {
              type: "input_text",
              text:
                "You are the content planner for an automated YouTube Shorts channel. " +
                "Favor originality, factual caution, concise wording, and useful information."
            }
          ]
        },
        {
          role: "user",
          content: [{ type: "input_text", text: prompt }]
        }
      ]
    })
  });

  const data = (await response.json()) as any;

  if (!response.ok) {
    throw new Error("OpenAI request failed: " + JSON.stringify(data));
  }

  return parseGeneratedJson(extractOpenAIText(data));
}

async function generateVideo(
  env: Env,
  content: GeneratedContent,
  duration: number
): Promise<string> {
  const prompt =
    "Create a coherent cinematic video for a YouTube Short. " +
    "Portrait-friendly composition, dynamic camera movement, high visual quality, " +
    "no logos, no watermarks, no readable text, no fake subtitles. " +
    "The visual story should match this concept: " +
    content.visual_prompt;

  const result = (await env.AI.run(
    env.VIDEO_MODEL || "black-forest-labs/flux-3-video",
    {
      mode: "t2v",
      prompt,
      resolution: "hd",
      duration,
      generate_audio: true
    }
  )) as any;

  const videoUrl = result?.result?.video || result?.video;

  if (!videoUrl) {
    throw new Error(
      "Workers AI video generation did not return a video URL: " +
        JSON.stringify(result)
    );
  }

  return String(videoUrl);
}

async function normalizeVideo(
  env: Env,
  jobId: string,
  sourceUrl: string,
  duration: number
): Promise<{ sourceKey: string; finalKey: string }> {
  const sourceResponse = await fetch(sourceUrl);

  if (!sourceResponse.ok || !sourceResponse.body) {
    throw new Error(
      "Failed to fetch generated video: " + sourceResponse.status
    );
  }

  const sourceKey = "jobs/" + jobId + "/source.mp4";
  const finalKey = "jobs/" + jobId + "/final.mp4";

  await env.MEDIA_BUCKET.put(sourceKey, sourceResponse.body, {
    httpMetadata: { contentType: "video/mp4" }
  });

  const stored = await env.MEDIA_BUCKET.get(sourceKey);

  if (!stored?.body) {
    throw new Error("Generated video was not stored in R2.");
  }

  const result = env.MEDIA
    .input(stored.body)
    .transform({
      width: 1080,
      height: 1920,
      fit: "cover"
    })
    .output({
      mode: "video",
      duration: duration + "s",
      audio: true
    });

  await env.MEDIA_BUCKET.put(finalKey, await result.media(), {
    httpMetadata: {
      contentType: await result.contentType()
    }
  });

  return { sourceKey, finalKey };
}

async function uploadVideoToYoutube(
  env: Env,
  videoKey: string,
  content: GeneratedContent
): Promise<string> {
  const object = await env.MEDIA_BUCKET.get(videoKey);

  if (!object?.body || typeof object.size !== "number") {
    throw new Error("Final video not found in R2.");
  }

  const accessToken = await getYoutubeAccessToken(env);
  const privacyStatus =
    env.AUTO_PUBLISH.toLowerCase() === "true" ? "public" : "private";

  const initResponse = await fetch(
    "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status",
    {
      method: "POST",
      headers: {
        Authorization: "Bearer " + accessToken,
        "Content-Type": "application/json; charset=UTF-8",
        "X-Upload-Content-Type": "video/mp4",
        "X-Upload-Content-Length": String(object.size)
      },
      body: JSON.stringify({
        snippet: {
          title: content.title,
          description: content.description,
          tags: content.tags,
          categoryId: env.YOUTUBE_CATEGORY_ID || "28"
        },
        status: {
          privacyStatus,
          selfDeclaredMadeForKids: false
        }
      })
    }
  );

  if (!initResponse.ok) {
    throw new Error(
      "YouTube upload initialization failed: " +
        (await initResponse.text())
    );
  }

  const uploadUrl = initResponse.headers.get("location");

  if (!uploadUrl) {
    throw new Error("YouTube did not return a resumable upload URL.");
  }

  const uploadResponse = await fetch(uploadUrl, {
    method: "PUT",
    headers: {
      Authorization: "Bearer " + accessToken,
      "Content-Type": "video/mp4",
      "Content-Length": String(object.size)
    },
    body: object.body
  });

  const uploadData = (await uploadResponse.json().catch(() => ({}))) as any;

  if (!uploadResponse.ok || !uploadData?.id) {
    throw new Error(
      "YouTube video upload failed: " + JSON.stringify(uploadData)
    );
  }

  return String(uploadData.id);
}

async function updateJob(
  env: Env,
  jobId: string,
  patch: Record<string, string | null | undefined>
): Promise<void> {
  const allowed = new Set([
    "workflow_id",
    "topic",
    "title",
    "description",
    "tags_json",
    "script",
    "visual_prompt",
    "source_key",
    "final_key",
    "status",
    "youtube_video_id",
    "error"
  ]);

  const entries = Object.entries(patch).filter(([key]) => allowed.has(key));

  if (!entries.length) return;

  const setSql =
    entries.map(([key]) => key + " = ?").join(", ") +
    ", updated_at = ?";

  const values = entries.map(([, value]) => value ?? null);
  values.push(new Date().toISOString(), jobId);

  await env.DB.prepare(
    "UPDATE jobs SET " + setSql + " WHERE id = ?"
  )
    .bind(...values)
    .run();
}

async function getYoutubeChannel(env: Env): Promise<any> {
  const accessToken = await getYoutubeAccessToken(env);

  const response = await fetch(
    "https://www.googleapis.com/youtube/v3/channels?part=id,snippet&mine=true",
    {
      headers: {
        Authorization: "Bearer " + accessToken
      }
    }
  );

  const data = (await response.json()) as any;

  if (!response.ok) {
    throw new Error(
      "YouTube channel lookup failed: " + JSON.stringify(data)
    );
  }

  return data;
}

async function dashboard(): Promise<Response> {
  const html = [
    "<!doctype html>",
    "<html lang='en'><head><meta charset='utf-8'>",
    "<meta name='viewport' content='width=device-width,initial-scale=1'>",
    "<title>AR Project • YouTube AI</title>",
    "<style>",
    "body{font-family:system-ui,-apple-system,sans-serif;max-width:900px;margin:40px auto;padding:20px;background:#0b1020;color:#eef2ff}",
    ".card{padding:20px;margin:14px 0;border:1px solid #29324d;border-radius:16px;background:#151c31}",
    "button,a{display:inline-block;margin:4px;padding:11px 15px;border:0;border-radius:10px;background:#6d7cff;color:white;text-decoration:none;font-weight:700;cursor:pointer}",
    "input{box-sizing:border-box;width:min(100%,600px);padding:11px;border-radius:10px;border:1px solid #384463;background:#0f1528;color:white}",
    "pre{white-space:pre-wrap;word-break:break-word;color:#cbd5e1}",
    "</style></head><body>",
    "<h1>AR Project • YouTube AI Automation</h1>",
    "<div class='card'><h2>YouTube</h2>",
    "<p>Use Google's official OAuth flow to connect the channel.</p>",
    "<a href='/auth/youtube'>Connect YouTube</a></div>",
    "<div class='card'><h2>Run a test</h2>",
    "<p>The initial deployment keeps uploads private. Enter ADMIN_TOKEN:</p>",
    "<input id='token' type='password' autocomplete='off'><br>",
    "<button onclick='runJob()'>Generate test Short</button>",
    "<button onclick='showJobs()'>Show jobs</button>",
    "<pre id='out'></pre></div>",
    "<div class='card'><a href='/health'>Health check</a></div>",
    "<script>",
    "const out=document.getElementById('out');",
    "async function call(path,options={}){const t=document.getElementById('token').value;options.headers={...(options.headers||{}),Authorization:'Bearer '+t,'Content-Type':'application/json'};const r=await fetch(path,options);out.textContent=await r.text();}",
    "async function runJob(){await call('/api/run',{method:'POST',body:'{}'});}",
    "async function showJobs(){await call('/api/jobs');}",
    "</script></body></html>"
  ].join("");

  return new Response(html, {
    headers: { "Content-Type": "text/html; charset=utf-8" }
  });
}

export class YouTubeAutomationWorkflow extends WorkflowEntrypoint<Env, PipelineParams> {
  async run(event: any, step: any): Promise<any> {
    const params = (event.payload || {}) as PipelineParams;
    const jobId = crypto.randomUUID();
    const workflowId = String(event.instanceId || jobId);

    await step.do("create job", async () => {
      await this.env.DB.prepare(
        "INSERT INTO jobs(id, workflow_id, topic, status) VALUES (?, ?, ?, ?)"
      )
        .bind(jobId, workflowId, params.topic || null, "created")
        .run();

      return { jobId };
    });

    try {
      const content = await step.do(
        "generate content",
        async () => {
          await updateJob(this.env, jobId, {
            status: "generating_content"
          });
          return generateContent(this.env, params);
        }
      );

      await step.do("save content", async () => {
        await updateJob(this.env, jobId, {
          topic: content.topic,
          title: content.title,
          description: content.description,
          tags_json: JSON.stringify(content.tags),
          script: content.spoken_script,
          visual_prompt: content.visual_prompt,
          status: "content_ready"
        });
      });

      const duration = Math.max(
        5,
        Math.min(
          20,
          Number(this.env.VIDEO_DURATION_SECONDS || "15")
        )
      );

      const videoUrl = await step.do(
        "generate video",
        async () => {
          await updateJob(this.env, jobId, {
            status: "generating_video"
          });
          return generateVideo(this.env, content, duration);
        }
      );

      const stored = await step.do(
        "normalize video",
        async () => {
          await updateJob(this.env, jobId, {
            status: "formatting_video"
          });
          return normalizeVideo(
            this.env,
            jobId,
            videoUrl,
            duration
          );
        }
      );

      await step.do("save media", async () => {
        await updateJob(this.env, jobId, {
          source_key: stored.sourceKey,
          final_key: stored.finalKey,
          status: "ready_to_upload"
        });
      });

      const youtubeVideoId = await step.do(
        "upload to YouTube",
        async () => {
          await updateJob(this.env, jobId, {
            status: "uploading"
          });

          return uploadVideoToYoutube(
            this.env,
            stored.finalKey,
            content
          );
        }
      );

      await step.do("finish job", async () => {
        await updateJob(this.env, jobId, {
          status: "uploaded",
          youtube_video_id: youtubeVideoId,
          error: null
        });
      });

      return {
        jobId,
        status: "uploaded",
        youtubeVideoId,
        title: content.title
      };
    } catch (error) {
      await updateJob(this.env, jobId, {
        status: "failed",
        error: errorMessage(error).slice(0, 5000)
      });
      throw error;
    }
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/") {
      return dashboard();
    }

    if (request.method === "GET" && url.pathname === "/health") {
      return json({
        ok: true,
        service: "ar-project-youtube-automation",
        time: new Date().toISOString(),
        timezone: env.TIMEZONE
      });
    }

    if (request.method === "GET" && url.pathname === "/auth/youtube") {
      return Response.redirect(
        await createYoutubeOAuthUrl(request, env),
        302
      );
    }

    if (
      request.method === "GET" &&
      url.pathname === "/auth/youtube/callback"
    ) {
      const state = url.searchParams.get("state");
      const code = url.searchParams.get("code");

      if (!state || !code) {
        return json(
          { error: "Missing OAuth state or code." },
          400
        );
      }

      const now = Math.floor(Date.now() / 1000);
      const stateRow = await env.DB.prepare(
        "SELECT state FROM oauth_states WHERE state = ? AND expires_at > ?"
      )
        .bind(state, now)
        .first();

      if (!stateRow) {
        return json(
          { error: "OAuth state is invalid or expired." },
          400
        );
      }

      await env.DB.prepare(
        "DELETE FROM oauth_states WHERE state = ?"
      )
        .bind(state)
        .run();

      await exchangeYoutubeCode(request, env, code);

      return new Response(
        "<h1>YouTube connected</h1><p>The refresh token was stored encrypted in D1. You can close this page.</p>",
        {
          headers: { "Content-Type": "text/html; charset=utf-8" }
        }
      );
    }

    if (url.pathname.startsWith("/api/")) {
      const denied = requireAdmin(request, env);
      if (denied) return denied;
    }

    if (request.method === "POST" && url.pathname === "/api/run") {
      let params: PipelineParams = {};

      try {
        params = (await request.json()) as PipelineParams;
      } catch {
        params = {};
      }

      const instance = await env.YOUTUBE_PIPELINE.create({
        id: crypto.randomUUID(),
        params: {
          reason: "manual",
          topic: params.topic
        }
      });

      return json(
        {
          accepted: true,
          workflowId: instance.id,
          status: await instance.status()
        },
        202
      );
    }

    if (request.method === "GET" && url.pathname === "/api/status") {
      const id = url.searchParams.get("id");

      if (!id) {
        return json({ error: "Missing workflow id." }, 400);
      }

      const instance = await env.YOUTUBE_PIPELINE.get(id);
      return json(await instance.status());
    }

    if (request.method === "GET" && url.pathname === "/api/jobs") {
      const limit = Math.min(
        50,
        Math.max(1, Number(url.searchParams.get("limit") || "20"))
      );

      const result = await env.DB.prepare(
        "SELECT id, workflow_id, topic, title, status, youtube_video_id, error, created_at, updated_at " +
        "FROM jobs ORDER BY created_at DESC LIMIT " +
        String(limit)
      ).all();

      return json(result.results);
    }

    if (
      request.method === "GET" &&
      url.pathname === "/api/youtube/status"
    ) {
      return json(await getYoutubeChannel(env));
    }

    return json({ error: "Not found." }, 404);
  }
} satisfies ExportedHandler<Env>;
