const APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbz1vWq1Ged6Qk5bfZUYUDvCeTfZicwBBZcvcwL5iHjrCiL1cLEQ4tCfKGt5IaAhJ69Mhw/exec";
const MAX_AUDIO_BYTES = 10 * 1024 * 1024;

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, PUT, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Range, X-Admin-Username, X-Admin-Password, X-File-Name, X-Device-ID"
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { headers: CORS_HEADERS });
    if (url.pathname === "/admin-upload") {
      if (request.method !== "PUT") return jsonResponse({ success: false, message: "Gunakan PUT untuk upload MP3." }, 405);
      return uploadAdminAudio(request, env, url);
    }
    if (url.pathname === "/public-upload") {
      if (request.method !== "PUT") return jsonResponse({ success: false, message: "Gunakan PUT untuk upload MP3." }, 405);
      return uploadPublicAudio(request, env, url);
    }
    return serveAudio(request, env, url);
  }
};

async function uploadAdminAudio(request, env, url) {
  const username = (request.headers.get("X-Admin-Username") || "").trim();
  const password = request.headers.get("X-Admin-Password") || "";
  const artist = (url.searchParams.get("artist") || "").trim();
  const title = (url.searchParams.get("title") || "").trim();
  const releaseDate = (url.searchParams.get("releaseDate") || "").trim();
  const contentType = (request.headers.get("Content-Type") || "").toLowerCase();
  const contentLength = Number(request.headers.get("Content-Length") || 0);

  if (!username || !password) return jsonResponse({ success: false, message: "Login admin diperlukan." }, 401);
  if (!artist || !title) return jsonResponse({ success: false, message: "Nama band dan judul lagu wajib diisi." }, 400);
  if (!request.body || !contentType.startsWith("audio/")) return jsonResponse({ success: false, message: "File harus berupa audio MP3." }, 400);
  if (!Number.isFinite(contentLength) || contentLength <= 0 || contentLength > MAX_AUDIO_BYTES) {
    return jsonResponse({ success: false, message: "Ukuran MP3 maksimal 10 MB." }, 413);
  }

  if (!await verifyActiveAdmin(username, password)) {
    return jsonResponse({ success: false, message: "Login admin tidak valid atau tidak aktif." }, 403);
  }

  const fileName = `${safeFilePart(artist)} - ${safeFilePart(title)}.mp3`;
  const objectKey = `songs/${fileName}`;
  await env.AUDIO_BUCKET.put(objectKey, request.body, {
    httpMetadata: {
      contentType: "audio/mpeg",
      contentDisposition: `inline; filename="${fileName.replace(/"/g, "")}"`
    },
    customMetadata: { artist, title, releaseDate, uploadedBy: username }
  });

  return jsonResponse({ success: true, objectKey, audioUrl: `${url.origin}/${encodePath(objectKey)}` });
}

async function uploadPublicAudio(request, env, url) {
  const artist = (url.searchParams.get("artist") || "").trim();
  const title = (url.searchParams.get("title") || "").trim();
  const releaseDate = (url.searchParams.get("releaseDate") || "").trim();
  const contentType = (request.headers.get("Content-Type") || "").toLowerCase();
  const contentLength = Number(request.headers.get("Content-Length") || 0);
  const originalName = request.headers.get("X-File-Name") || "audio.mp3";

  if (!artist || !title) return jsonResponse({ success: false, message: "Nama band dan judul lagu wajib diisi." }, 400);
  if (!request.body || !contentType.startsWith("audio/")) return jsonResponse({ success: false, message: "File harus berupa audio MP3." }, 400);
  if (!Number.isFinite(contentLength) || contentLength <= 0 || contentLength > MAX_AUDIO_BYTES) {
    return jsonResponse({ success: false, message: "Ukuran MP3 maksimal 10 MB." }, 413);
  }

  const fileName = `${safeFilePart(artist)} - ${safeFilePart(title)}.mp3`;
  const objectKey = `songs/pending/${crypto.randomUUID().slice(0, 8)}-${fileName}`;
  await env.AUDIO_BUCKET.put(objectKey, request.body, {
    httpMetadata: {
      contentType: "audio/mpeg",
      contentDisposition: `inline; filename="${fileName.replace(/"/g, "")}"`
    },
    customMetadata: { artist, title, releaseDate, originalName, uploadedBy: "public", approvalStatus: "pending" }
  });

  return jsonResponse({ success: true, objectKey, audioUrl: `${url.origin}/${encodePath(objectKey)}` });
}

async function verifyActiveAdmin(username, password) {
  try {
    const response = await fetch(APPS_SCRIPT_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ type: "admin_login", username, password })
    });
    if (!response.ok) return false;
    const result = await response.json();
    return result && result.success === true && String(result.user && result.user.status || "").toUpperCase() === "ACTIVE";
  } catch (_) {
    return false;
  }
}

async function serveAudio(request, env, url) {
  try {
    const objectKey = decodeURIComponent(url.pathname.replace(/^\/+/, ""));
    if (!objectKey) return new Response("Missing object path", { status: 400, headers: CORS_HEADERS });

    const range = request.headers.get("Range");
    const object = await env.AUDIO_BUCKET.get(objectKey, range ? { range } : {});
    if (!object) return new Response("Not found", { status: 404, headers: CORS_HEADERS });

    const headers = new Headers(CORS_HEADERS);
    object.writeHttpMetadata(headers);
    headers.set("etag", object.httpEtag);
    headers.set("Accept-Ranges", "bytes");
    if (object.range) {
      const start = object.range.offset;
      headers.set("Content-Range", `bytes ${start}-${start + object.range.length - 1}/${object.size}`);
      headers.set("Content-Length", String(object.range.length));
      return new Response(request.method === "HEAD" ? null : object.body, { status: 206, headers });
    }
    headers.set("Content-Length", String(object.size));
    return new Response(request.method === "HEAD" ? null : object.body, { headers });
  } catch (error) {
    return jsonResponse({ success: false, message: error && error.message || "Gagal mengambil audio." }, 500);
  }
}

function safeFilePart(value) {
  return value.replace(/[\\/:*?"<>|]/g, " ").replace(/\s+/g, " ").trim().slice(0, 120) || "untitled";
}

function encodePath(path) {
  return path.split("/").map(encodeURIComponent).join("/");
}

function jsonResponse(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json; charset=UTF-8" }
  });
}
