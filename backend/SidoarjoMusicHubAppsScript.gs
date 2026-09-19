// ─── CONFIG ───────────────────────────────────────────────────────────────────
const SPREADSHEET_ID = "121rFh8z7MS8Yp89dX0qPCclg42U5mZ7lHNW_0DWZC2A";

// ─── JALANKAN FUNGSI INI SEKALI UNTUK SETUP SHEET ADMIN ──────────────────────
// Pilih "setupAdminSheet" di dropdown lalu klik tombol ▶ Run
function setupAdminSheet() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);

  // Rename "Form Registrasi" → "admin" jika masih ada
  const oldSheet = ss.getSheetByName("Form Registrasi");
  if (oldSheet) {
    oldSheet.setName("admin");
    Logger.log("✅ Sheet 'Form Registrasi' berhasil direname menjadi 'admin'");
  }

  // Pastikan sheet admin ada
  let adminSheet = ss.getSheetByName("admin");
  if (!adminSheet) {
    adminSheet = ss.insertSheet("admin");
    Logger.log("✅ Sheet 'admin' dibuat baru");
  }

  // Hapus semua isi lama
  adminSheet.clearContents();

  // Set header
  adminSheet.getRange("A1").setValue("username");
  adminSheet.getRange("B1").setValue("password");
  adminSheet.getRange("C1").setValue("status");

  // Set data admin default
  adminSheet.getRange("A2").setValue("admin");
  adminSheet.getRange("B2").setValue("admin");
  adminSheet.getRange("C2").setValue("ACTIVE");

  // Format header tebal
  adminSheet.getRange("A1:C1").setFontWeight("bold");
  adminSheet.setFrozenRows(1);

  Logger.log("✅ Sheet 'admin' sudah siap. Username: admin | Password: admin | Status: ACTIVE");

  // Pastikan sheet lain juga ada
  setupSheets_();
  Logger.log("✅ Semua sheet sudah siap: admin, published_songs, portal_posts, videos");
}

// Pilih "fixSheets" di dropdown lalu klik tombol ▶ Run untuk memperbaiki semua sheet
function fixSheets() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  fixOneSheet_(ss, SHEET_PUBLISHED, PUBLISHED_HEADERS);
  fixOneSheet_(ss, SHEET_VIDEOS, VIDEOS_HEADERS);
  fixOneSheet_(ss, SHEET_VIDEO_CHANNELS, VIDEO_CHANNEL_HEADERS);
  fixOneSheet_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  fixOneSheet_(ss, SHEET_PORTAL_POSTER_COOLDOWNS, PORTAL_POSTER_COOLDOWN_HEADERS);
  fixOneSheet_(ss, SHEET_SONG_SUBMISSION_COOLDOWNS, SONG_SUBMISSION_COOLDOWN_HEADERS);
  Logger.log("✅ fixSheets selesai. Semua sheet sudah bersih dan rapi.");
}

// Helper: bersihkan satu sheet agar headernya ada tepat di row 1
function fixOneSheet_(ss, sheetName, expectedHeaders) {
  const sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    Logger.log("⚠️ Sheet '" + sheetName + "' tidak ditemukan, dilewati.");
    return;
  }

  const lastRow = sheet.getLastRow();
  const lastCol = sheet.getLastColumn();

  if (lastRow === 0) {
    // Sheet kosong — langsung tulis header
    sheet.getRange(1, 1, 1, expectedHeaders.length).setValues([expectedHeaders]);
    Logger.log("✅ '" + sheetName + "' kosong, header ditulis di row 1.");
    return;
  }

  // Baca semua data yang ada
  const allValues = sheet.getDataRange().getValues();

  // Cari baris mana yang merupakan header asli (mengandung nilai expectedHeaders[0] di kolom pertama)
  let headerRowIndex = -1;
  for (let i = 0; i < allValues.length; i++) {
    if (String(allValues[i][0] || "").trim() === expectedHeaders[0]) {
      headerRowIndex = i;
      break;
    }
  }

  // Kumpulkan baris DATA (setelah header, bukan baris kosong, bukan duplikat header)
  let dataRows = [];
  if (headerRowIndex >= 0 && headerRowIndex + 1 < allValues.length) {
    for (let i = headerRowIndex + 1; i < allValues.length; i++) {
      const row = allValues[i];
      const firstCell = String(row[0] || "").trim();
      // Lewati baris kosong atau baris duplikat header
      if (firstCell === "" || firstCell === expectedHeaders[0]) continue;
      dataRows.push(row);
    }
  }

  // Hapus semua isi sheet
  sheet.clearContents();

  // Tulis header bersih di row 1
  sheet.getRange(1, 1, 1, expectedHeaders.length).setValues([expectedHeaders]);

  // Tulis ulang data rows (jika ada) mulai row 2
  if (dataRows.length > 0) {
    // Potong setiap baris ke jumlah kolom header yang benar
    const trimmedRows = dataRows.map(row => {
      const trimmed = row.slice(0, expectedHeaders.length);
      while (trimmed.length < expectedHeaders.length) trimmed.push("");
      return trimmed;
    });
    sheet.getRange(2, 1, trimmedRows.length, expectedHeaders.length).setValues(trimmedRows);
    Logger.log("✅ '" + sheetName + "' diperbaiki: " + dataRows.length + " baris data disimpan.");
  } else {
    Logger.log("✅ '" + sheetName + "' diperbaiki: tidak ada baris data (hanya header).");
  }

  // Hapus kolom berlebih di luar range header
  if (sheet.getLastColumn() > expectedHeaders.length) {
    sheet.deleteColumns(expectedHeaders.length + 1, sheet.getLastColumn() - expectedHeaders.length);
  }
}

const SHEET_ADMIN      = "admin";
const SHEET_PUBLISHED  = "published_songs";
const SHEET_PORTAL     = "portal_posts";
const SHEET_PORTAL_POSTER_COOLDOWNS = "portal_poster_cooldowns";
const SHEET_SONG_SUBMISSION_COOLDOWNS = "song_submission_cooldowns";
const SHEET_VIDEOS     = "videos";
const SHEET_VIDEO_CHANNELS = "video_channels";

const SONG_COVER_FOLDER_ID   = "1wd586v5J2FdLGSOIuGfh85oCkaRTSb2s";
const PORTAL_POSTER_FOLDER_ID = "1Hfj3fpVDunhJ7T7QAarTJ_l7amOchpIy";

const ADMIN_HEADERS = ["username", "password", "status"];

const PUBLISHED_HEADERS = [
  "song_id", "artist", "title", "genre", "city",
  "release_year", "cover_url", "audio_url", "status"
];

const PORTAL_HEADERS = [
  "id", "user_id", "category", "poster_url", "posting_at", "status"
];
const PORTAL_POSTER_COOLDOWN_HEADERS = ["device_id", "last_posted_at"];
const PORTAL_POSTER_COOLDOWN_MS = 1 * 60 * 60 * 1000;
const SONG_SUBMISSION_COOLDOWN_HEADERS = ["device_id", "last_submitted_at"];
const SONG_SUBMISSION_COOLDOWN_MS = 30 * 60 * 1000;

const VIDEOS_HEADERS = [
  "id", "title", "artist", "video_url", "status",
  "release_year", "album_name", "genre"
];

// Channel disimpan terpisah supaya video Google Drive/manual yang sudah ada tidak berubah.
const VIDEO_CHANNEL_HEADERS = [
  "id", "artist", "channel_url", "channel_id", "status", "created_at"
];

// ─── GET ──────────────────────────────────────────────────────────────────────
function doGet(e) {
  try {
    setupSheets_();
    const type = String((e && e.parameter && e.parameter.type) || "").trim();

    if (type === "portal_posts") {
      return jsonOutput_({ success: true, portal_posts: getActivePortalPosts_() });
    }
    if (type === "videos") {
      const artist = String((e && e.parameter && e.parameter.artist) || "").trim();
      return jsonOutput_({ success: true, videos: getActiveVideos_(artist) });
    }
    if (type === "video_bands") {
      return jsonOutput_({ success: true, bands: getActiveVideoBands_() });
    }
    if (type === "video_channels") {
      return jsonOutput_({ success: true, channels: getVideoChannels_() });
    }
    const songs = getActiveSongs_();
    return jsonOutput_({ success: true, songs: songs });
  } catch (error) {
    return jsonOutput_({ success: false, message: error && error.message ? error.message : "Server error (GET)." });
  }
}

// ─── POST ─────────────────────────────────────────────────────────────────────
function doPost(e) {
  try {
    setupSheets_();
    if (!e || !e.postData) {
      Logger.log("⚠️ doPost dijalankan tanpa event data (manual Run dari editor). Setup sheet berhasil dilakukan.");
      return jsonOutput_({ success: true, message: "Setup sheet otomatis berhasil dijalankan via doPost." });
    }
    const payload = JSON.parse((e && e.postData && e.postData.contents) || "{}");
    const type = String(payload.type || "").trim();

    if (type === "admin_login")               return handleAdminLogin_(payload);
    if (type === "admin_check_status")        return handleAdminCheckStatus_(payload);
    if (type === "admin_update_credentials")  return handleAdminUpdateCredentials_(payload);
    if (type === "portal_post")               return handlePortalPost_(payload);
    if (type === "portal_delete")             return handlePortalDelete_(payload);
    if (type === "song_registration")         return handleSongRegistration_(payload);
    if (type === "video_registration")        return handleVideoRegistration_(payload);
    if (type === "channel_import" || type === "youtube_channel_import") {
      return handleYoutubeChannelImport_(payload);
    }
    if (type === "channel_delete") return handleYoutubeChannelDelete_(payload);
    if (type === "video_delete") return handleVideoDelete_(payload);
    if (type === "video_update") return handleVideoUpdate_(payload);

    return jsonOutput_({ success: false, message: "Tipe request tidak dikenal." });
  } catch (error) {
    return jsonOutput_({ success: false, message: error && error.message ? error.message : "Server error (POST)." });
  }
}

// ─── SETUP ────────────────────────────────────────────────────────────────────
function setupSheets_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);

  // Otomatis rename "Form Registrasi" → "admin" jika masih ada
  const oldSheet = ss.getSheetByName("Form Registrasi");
  if (oldSheet) {
    oldSheet.setName("admin");
    oldSheet.clearContents();
    oldSheet.getRange("A1").setValue("username");
    oldSheet.getRange("B1").setValue("password");
    oldSheet.getRange("C1").setValue("status");
    oldSheet.getRange("A2").setValue("admin");
    oldSheet.getRange("B2").setValue("admin");
    oldSheet.getRange("C2").setValue("ACTIVE");
    oldSheet.getRange("A1:C1").setFontWeight("bold");
    oldSheet.setFrozenRows(1);
    Logger.log("✅ Sheet 'Form Registrasi' otomatis direname menjadi 'admin' & diinisialisasi.");
  }

  ensureAdminSheetWithDefaults_(ss);
  ensureSheetWithHeaders_(ss, SHEET_PUBLISHED, PUBLISHED_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_PORTAL_POSTER_COOLDOWNS, PORTAL_POSTER_COOLDOWN_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_SONG_SUBMISSION_COOLDOWNS, SONG_SUBMISSION_COOLDOWN_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_VIDEOS, VIDEOS_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_VIDEO_CHANNELS, VIDEO_CHANNEL_HEADERS);
}

// ─── ADMIN AUTH ───────────────────────────────────────────────────────────────
function handleAdminLogin_(payload) {
  const username = String(payload.username || "").trim();
  const password = String(payload.password || "").trim();
  const admin = findAdminByUsernameForLogin_(username, password);
  if (!admin) return jsonOutput_({ success: false, message: "Admin tidak ditemukan." });
  if (admin.password !== password) return jsonOutput_({ success: false, message: "Password salah." });
  if (admin.status !== "ACTIVE")  return jsonOutput_({ success: false, message: "Akun admin tidak aktif." });

  return jsonOutput_({
    success: true,
    message: "Login berhasil.",
    user: {
      id: admin.username,
      username: admin.username,
      createdAt: formatDateValue_(new Date()),
      status: admin.status
    }
  });
}

function handleAdminCheckStatus_(payload) {
  const username = String(payload.username || "").trim();
  const admin = findAdminByUsername_(username);
  if (!admin) return jsonOutput_({ success: false, message: "Info admin tidak ditemukan." });

  const password = String(payload.password || "").trim();
  if (password && admin.password !== password) {
    return jsonOutput_({ success: false, message: "Password admin salah." });
  }

  return jsonOutput_({
    success: true,
    message: "Status ditemukan.",
    user: {
      id: admin.username,
      username: admin.username,
      createdAt: "",
      status: admin.status
    }
  });
}

function handleAdminUpdateCredentials_(payload) {
  const currentUsername = String(payload.username || "").trim();
  const newUsername = String(payload.new_username || payload.newUsername || "").trim();
  const newPassword = String(payload.new_password || payload.newPassword || "").trim();

  if (!currentUsername) return jsonOutput_({ success: false, message: "Username saat ini tidak ditemukan." });
  if (!newUsername)     return jsonOutput_({ success: false, message: "Username baru wajib diisi." });
  if (!newPassword)     return jsonOutput_({ success: false, message: "Password baru wajib diisi." });

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureAdminSheetWithDefaults_(ss);
  const values = sheet.getDataRange().getValues();
  const col = getHeaderMap_(values[0]);
  const usernameIdx = col["username"];
  const passwordIdx = col["password"];

  if (usernameIdx === undefined || passwordIdx === undefined) {
    return jsonOutput_({ success: false, message: "Kolom admin tidak ditemukan." });
  }

  let targetRow = -1, duplicateRow = -1;
  for (let i = 1; i < values.length; i++) {
    const rowUsername = String(values[i][usernameIdx] || "").trim();
    if (rowUsername === currentUsername) targetRow = i + 1;
    if (rowUsername === newUsername) duplicateRow = i + 1;
  }

  if (targetRow < 2) return jsonOutput_({ success: false, message: "Admin tidak ditemukan." });
  if (duplicateRow > 0 && duplicateRow !== targetRow) {
    return jsonOutput_({ success: false, message: "Username baru sudah dipakai admin lain." });
  }

  sheet.getRange(targetRow, usernameIdx + 1).setValue(newUsername);
  sheet.getRange(targetRow, passwordIdx + 1).setValue(newPassword);

  return jsonOutput_({
    success: true,
    message: "Akun admin berhasil diperbarui.",
    user: {
      id: newUsername,
      username: newUsername,
      createdAt: formatDateValue_(new Date()),
      status: "ACTIVE"
    }
  });
}

function validateActiveAdmin_(payload) {
  const username = String(payload.username || "").trim();
  const password = String(payload.password || "").trim();
  const admin = password ? findAdminByUsernameForLogin_(username, password) : findAdminByUsername_(username);
  if (!admin) throw new Error("Admin tidak ditemukan.");
  if (password && admin.password !== password) throw new Error("Password admin salah.");
  if (admin.status !== "ACTIVE") throw new Error("Akun admin tidak aktif.");
  return admin;
}

function findAdminByUsernameForLogin_(username, password) {
  const normalized = String(username || "").trim().toLowerCase();
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureAdminSheetWithDefaults_(ss);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) {
    if (normalized === "admin" && password === "admin") return { username: "admin", password: "admin", status: "ACTIVE" };
    return null;
  }
  const col = getHeaderMap_(values[0]);
  const usernameIdx = col["username"];
  const passwordIdx = col["password"];
  const statusIdx = col["status"];
  if (usernameIdx === undefined || passwordIdx === undefined) return null;
  for (let i = 1; i < values.length; i++) {
    const rowUsername = String(values[i][usernameIdx] || "").trim().toLowerCase();
    const rowPassword = String(values[i][passwordIdx] || "").trim();
    if (rowUsername === normalized && rowPassword === password) {
      const rowStatus = (statusIdx !== undefined && String(values[i][statusIdx] || "").trim())
        ? String(values[i][statusIdx] || "").trim()
        : "ACTIVE";
      return { username: String(values[i][usernameIdx] || "").trim(), password: rowPassword, status: rowStatus };
    }
  }
  return null;
}

function findAdminByUsername_(username) {
  const normalized = String(username || "").trim().toLowerCase();
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureAdminSheetWithDefaults_(ss);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) {
    return normalized === "admin" ? { username: "admin", password: "admin", status: "ACTIVE" } : null;
  }
  const col = getHeaderMap_(values[0]);
  const usernameIdx = col["username"];
  const passwordIdx = col["password"];
  const statusIdx = col["status"];
  if (usernameIdx === undefined || passwordIdx === undefined) return null;
  for (let i = 1; i < values.length; i++) {
    const rowUsername = String(values[i][usernameIdx] || "").trim().toLowerCase();
    if (rowUsername === normalized) {
      const rowStatus = (statusIdx !== undefined && String(values[i][statusIdx] || "").trim())
        ? String(values[i][statusIdx] || "").trim()
        : "ACTIVE";
      return {
        username: String(values[i][usernameIdx] || "").trim(),
        password: String(values[i][passwordIdx] || "").trim(),
        status: rowStatus
      };
    }
  }
  return null;
}

// ─── PORTAL POSTS ─────────────────────────────────────────────────────────────
function handlePortalPost_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const admin = getActiveAdminOrNull_(payload);
  const deviceId = String(payload.device_id || "").trim();
  if (!admin && !deviceId) throw new Error("Identitas perangkat tidak tersedia. Coba buka ulang aplikasi.");

  const posterBase64 = String(
    payload.posterImageBase64 || payload.imageBase64 || payload.base64 || ""
  ).trim();
  if (!posterBase64) throw new Error("Base64 poster tidak ditemukan.");

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    if (!admin) enforcePosterCooldown_(ss, deviceId);

    const posterStorageKey = admin
      ? "admin:" + String(admin.username || "").trim().toLowerCase()
      : "device:" + deviceId;
    const posterUpload = uploadOrReplacePortalPoster_(
      posterBase64,
      payload.posterImageFileName || "poster.jpg",
      payload.posterImageMimeType || "image/jpeg",
      posterStorageKey
    );
    if (posterUpload.replacedUrl) {
      replacePortalPosterUrl_(sheet, posterUpload.replacedUrl, posterUpload.posterUrl);
    }
    const posterUrl = posterUpload.posterUrl;
    const category = String(payload.category || "").trim();
    const username = admin ? admin.username : "Pengguna Portal";

    sheet.appendRow([
      "portal_" + Date.now(),
      username,
      category,
      posterUrl,
      new Date(),
      admin ? "ACTIVE" : "PENDING"
    ]);
    if (!admin) savePosterCooldown_(ss, deviceId, new Date());

    return jsonOutput_({ success: true, message: "Poster berhasil dipasang." });
  } finally {
    lock.releaseLock();
  }
}

function getActiveAdminOrNull_(payload) {
  const username = String(payload.username || "").trim();
  const password = String(payload.password || "").trim();
  if (!username || !password) return null;
  const admin = findAdminByUsernameForLogin_(username, password);
  return admin && admin.status === "ACTIVE" ? admin : null;
}

function enforcePosterCooldown_(ss, deviceId) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_POSTER_COOLDOWNS, PORTAL_POSTER_COOLDOWN_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return;
  const headers = getHeaderMap_(values[0]);
  const deviceIdIndex = headers["device_id"];
  const postedAtIndex = headers["last_posted_at"];
  for (let row = 1; row < values.length; row++) {
    if (String(values[row][deviceIdIndex] || "").trim() !== deviceId) continue;
    const lastPostedAt = new Date(values[row][postedAtIndex]);
    const lastPostedMillis = lastPostedAt.getTime();
    if (isNaN(lastPostedMillis)) return;
    const remaining = PORTAL_POSTER_COOLDOWN_MS - (Date.now() - lastPostedMillis);
    if (remaining > 0) {
      const remainingMinutes = Math.ceil(remaining / 60000);
      const hours = Math.floor(remainingMinutes / 60);
      const minutes = remainingMinutes % 60;
      throw new Error("Anda dapat memasang poster lagi dalam " + hours + " jam " + minutes + " menit.");
    }
    return;
  }
}

function savePosterCooldown_(ss, deviceId, postedAt) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_POSTER_COOLDOWNS, PORTAL_POSTER_COOLDOWN_HEADERS);
  const values = sheet.getDataRange().getValues();
  const headers = getHeaderMap_(values[0] || []);
  const deviceIdIndex = headers["device_id"];
  const postedAtIndex = headers["last_posted_at"];
  for (let row = 1; row < values.length; row++) {
    if (String(values[row][deviceIdIndex] || "").trim() === deviceId) {
      sheet.getRange(row + 1, postedAtIndex + 1).setValue(postedAt);
      return;
    }
  }
  sheet.appendRow([deviceId, postedAt]);
}

function replacePortalPosterUrl_(sheet, previousUrl, replacementUrl) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return;
  const headers = getHeaderMap_(values[0]);
  const posterUrlIndex = headers["poster_url"];
  if (posterUrlIndex === undefined) return;
  for (let row = 1; row < values.length; row++) {
    if (String(values[row][posterUrlIndex] || "").trim() === previousUrl) {
      sheet.getRange(row + 1, posterUrlIndex + 1).setValue(replacementUrl);
    }
  }
}

function isPortalPosterUrlReferenced_(sheet, posterUrl) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return false;
  const posterUrlIndex = getHeaderMap_(values[0])["poster_url"];
  if (posterUrlIndex === undefined) return false;
  return values.slice(1).some(row => String(row[posterUrlIndex] || "").trim() === posterUrl);
}

function trashPortalPosterFile_(posterUrl) {
  const match = String(posterUrl || "").match(/[?&]id=([^&]+)/);
  if (!match) return;
  try {
    DriveApp.getFileById(decodeURIComponent(match[1])).setTrashed(true);
  } catch (e) {
    Logger.log("Poster Drive tidak dapat dipindahkan ke sampah: " + e);
  }
}

function handlePortalDelete_(payload) {
  validateActiveAdmin_(payload);
  const postId = String(payload.postId || payload.post_id || payload.id || "").trim();
  if (!postId) return jsonOutput_({ success: false, message: "postId wajib diisi." });

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return jsonOutput_({ success: false, message: "Data kosong." });

  const col = getHeaderMap_(values[0]);
  const idIdx = col["id"];
  const posterUrlIdx = col["poster_url"];
  if (idIdx === undefined) return jsonOutput_({ success: false, message: "Kolom id tidak ditemukan." });

  for (let r = 1; r < values.length; r++) {
    if (String(values[r][idIdx] || "").trim() !== postId) continue;
    const posterUrl = posterUrlIdx === undefined ? "" : String(values[r][posterUrlIdx] || "").trim();
    sheet.deleteRow(r + 1);
    if (posterUrl && !isPortalPosterUrlReferenced_(sheet, posterUrl)) {
      trashPortalPosterFile_(posterUrl);
    }
    return jsonOutput_({ success: true, message: "Poster berhasil dihapus.", deleted_id: postId });
  }
  return jsonOutput_({ success: false, message: "Poster tidak ditemukan." });
}

function getActivePortalPosts_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  return readSheetObjects_(sheet)
    .filter(p => String(p.status || "").trim().toUpperCase() === "ACTIVE")
    .map(p => ({
      id: p.id,
      title: p.category || p.user_id || "",
      band_name: p.user_id || "",
      submitter_name: p.user_id || "",
      category: p.category || "",
      poster_url: p.poster_url || "",
      short_caption: "",
      status: p.status || "",
      created_at: formatDateValue_(p.posting_at)
    }));
}

// ─── SONGS ────────────────────────────────────────────────────────────────────
function handleSongRegistration_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PUBLISHED, PUBLISHED_HEADERS);

  const title      = String(payload.songTitle || payload.title || "").trim();
  const artist     = String(payload.bandName || payload.artist || payload.artistName || "").trim();
  const genre      = String(payload.genre || "").trim();
  const city       = String(payload.city || "").trim();
  const releaseYear = String(payload.releaseYear || payload.release_year || "").trim();
  const audioUrl   = String(payload.audioUrl || payload.audio_url || payload.mp3Link || "").trim();
  const coverArtworkLink = String(payload.coverArtworkLink || payload.cover_url || "").trim();
  const coverImageBase64 = String(payload.coverImageBase64 || "").trim();
  const coverImageFileName = String(payload.coverImageFileName || "cover.jpg").trim();
  const coverImageMimeType = String(payload.coverImageMimeType || "image/jpeg").trim();
  const admin = getActiveAdminOrNull_(payload);
  const deviceId = String(payload.device_id || "").trim();

  if (!title) return jsonOutput_({ success: false, message: "Judul lagu wajib diisi." });
  if (!artist) return jsonOutput_({ success: false, message: "Nama artis wajib diisi." });
  if (!audioUrl) return jsonOutput_({ success: false, message: "Link audio/MP3 wajib diisi." });

  if (!admin && !deviceId) return jsonOutput_({ success: false, message: "Identitas perangkat tidak tersedia. Coba buka ulang aplikasi." });
  const lock = admin ? null : LockService.getScriptLock();
  if (lock) lock.waitLock(30000);
  try {
    if (!admin) enforceSongSubmissionCooldown_(ss, deviceId);

    let coverUrl = coverArtworkLink;
    if (!coverUrl && coverImageBase64) {
      coverUrl = uploadBase64ImageToDrive_(coverImageBase64, coverImageFileName, coverImageMimeType, SONG_COVER_FOLDER_ID);
    }

    sheet.appendRow([
      "song_" + Date.now(),
      artist, title, genre, city, releaseYear,
      coverUrl, audioUrl, admin ? "approved" : "pending"
    ]);
    if (!admin) saveSongSubmissionCooldown_(ss, deviceId, new Date());
  } finally {
    if (lock) lock.releaseLock();
  }

  return jsonOutput_({ success: true, message: "Lagu berhasil disimpan." });
}

function enforceSongSubmissionCooldown_(ss, deviceId) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_SONG_SUBMISSION_COOLDOWNS, SONG_SUBMISSION_COOLDOWN_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return;
  const headers = getHeaderMap_(values[0]);
  const deviceIdIndex = headers["device_id"];
  const submittedAtIndex = headers["last_submitted_at"];
  for (let row = 1; row < values.length; row++) {
    if (String(values[row][deviceIdIndex] || "").trim() !== deviceId) continue;
    const lastSubmittedMillis = new Date(values[row][submittedAtIndex]).getTime();
    if (isNaN(lastSubmittedMillis)) return;
    const remaining = SONG_SUBMISSION_COOLDOWN_MS - (Date.now() - lastSubmittedMillis);
    if (remaining > 0) {
      throw new Error("Pengiriman musik berikutnya tersedia dalam " + Math.ceil(remaining / 60000) + " menit.");
    }
    return;
  }
}

function saveSongSubmissionCooldown_(ss, deviceId, submittedAt) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_SONG_SUBMISSION_COOLDOWNS, SONG_SUBMISSION_COOLDOWN_HEADERS);
  const values = sheet.getDataRange().getValues();
  const headers = getHeaderMap_(values[0] || []);
  const deviceIdIndex = headers["device_id"];
  const submittedAtIndex = headers["last_submitted_at"];
  for (let row = 1; row < values.length; row++) {
    if (String(values[row][deviceIdIndex] || "").trim() === deviceId) {
      sheet.getRange(row + 1, submittedAtIndex + 1).setValue(submittedAt);
      return;
    }
  }
  sheet.appendRow([deviceId, submittedAt]);
}

function getActiveSongs_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PUBLISHED, PUBLISHED_HEADERS);
  return readSheetObjects_(sheet)
    .filter(s => {
      const status = String(s.status || "").trim().toLowerCase();
      return status === "approved" || status === "active" || status === "";
    })
    .map(s => ({
      song_id: s.song_id || "",
      artist: s.artist || "",
      title: s.title || "",
      genre: s.genre || "",
      city: s.city || "",
      release_year: s.release_year || "",
      cover_url: s.cover_url || "",
      audio_url: s.audio_url || "",
      status: s.status || ""
    }));
}

// ─── VIDEOS ───────────────────────────────────────────────────────────────────
function handleVideoRegistration_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEOS, VIDEOS_HEADERS);

  const title      = String(payload.title || "").trim();
  const resolvedTitle = title || "Untitled MV";
  const artist     = String(payload.artist || payload.bandName || "").trim();
  const videoUrl   = String(payload.video_url || payload.videoUrl || "").trim();
  const releaseYear = String(payload.releaseYear || payload.release_year || "").trim();
  const albumName   = String(payload.albumName || payload.album_name || "").trim();
  const genre       = String(payload.genre || "").trim();
  const admin = getActiveAdminOrNull_(payload);

  if (!videoUrl) return jsonOutput_({ success: false, message: "Link video wajib diisi." });

  sheet.appendRow([
    "vid_" + Date.now(),
    resolvedTitle, artist, videoUrl, admin ? "approved" : "pending",
    releaseYear, albumName, genre
  ]);

  return jsonOutput_({ success: true, message: "Video berhasil disimpan." });
}

function getActiveVideos_(artistFilter) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEOS, VIDEOS_HEADERS);
  const manualVideos = readSheetObjects_(sheet)
    .filter(v => {
      const status = String(v.status || "").trim().toLowerCase();
      return status === "approved" || status === "active" || status === "";
    })
    .map(v => ({
      id: v.id || "",
      is_manual: true,
      title: v.title || "",
      artist: v.artist || "",
      video_url: v.video_url || "",
      status: v.status || "",
      release_year: v.release_year || "",
      album_name: v.album_name || "",
      genre: v.genre || ""
    }));

  // Channel YouTube tidak meng-copy video ke sheet. Setiap refresh aplikasi membaca
  // daftar publik terbaru, sehingga upload video baru langsung ikut muncul.
  const channelSheet = ensureSheetWithHeaders_(ss, SHEET_VIDEO_CHANNELS, VIDEO_CHANNEL_HEADERS);
  const channelVideos = [];
  readSheetObjects_(channelSheet)
    .filter(channel => isActiveStatus_(channel.status))
    .forEach(channel => {
      const artist = String(channel.artist || "").trim() || "Channel YouTube";
      const channelId = String(channel.channel_id || "").trim();
      if (!channelId) return;
      fetchYoutubeChannelVideos_(channelId, 100).forEach(video => {
        channelVideos.push({
          id: "yt_" + channelId + "_" + video.videoId,
          title: video.title || "Video YouTube",
          artist: artist,
          description: video.description || "",
          video_url: video.url,
          cover_url: video.thumbnailUrl || "",
          status: "ACTIVE",
          release_year: video.year || "",
          album_name: "",
          genre: ""
        });
      });
    });

  const seen = {};
  const normalizedArtist = normalizeBandName_(artistFilter);
  return manualVideos.concat(channelVideos).filter(video => {
    const key = String(video.video_url || video.id || "").trim();
    if (!key || seen[key]) return false;
    seen[key] = true;
    return !normalizedArtist || normalizeBandName_(video.artist) === normalizedArtist;
  });
}

function handleVideoDelete_(payload) {
  validateActiveAdmin_(payload);
  const videoId = String(payload.video_id || payload.id || "").trim();
  if (!videoId) return jsonOutput_({ success: false, message: "ID video wajib diisi." });

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEOS, VIDEOS_HEADERS);
  const values = sheet.getDataRange().getValues();
  const headers = getHeaderMap_(values[0] || []);
  const idIndex = headers["id"];
  if (idIndex === undefined) return jsonOutput_({ success: false, message: "Kolom ID video tidak ditemukan." });

  for (let row = 1; row < values.length; row++) {
    if (String(values[row][idIndex] || "").trim() !== videoId) continue;
    sheet.deleteRow(row + 1);
    return jsonOutput_({ success: true, message: "Video berhasil dihapus.", deleted_id: videoId });
  }
  return jsonOutput_({ success: false, message: "Video tidak ditemukan." });
}

function handleVideoUpdate_(payload) {
  validateActiveAdmin_(payload);
  const videoId = String(payload.video_id || payload.id || "").trim();
  const title = String(payload.title || "").trim();
  if (!videoId) return jsonOutput_({ success: false, message: "ID video wajib diisi." });
  if (!title) return jsonOutput_({ success: false, message: "Judul video wajib diisi." });

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEOS, VIDEOS_HEADERS);
  const values = sheet.getDataRange().getValues();
  const headers = getHeaderMap_(values[0] || []);
  const idIndex = headers["id"];
  const titleIndex = headers["title"];
  if (idIndex === undefined || titleIndex === undefined) {
    return jsonOutput_({ success: false, message: "Kolom video tidak lengkap." });
  }

  for (let row = 1; row < values.length; row++) {
    if (String(values[row][idIndex] || "").trim() !== videoId) continue;
    sheet.getRange(row + 1, titleIndex + 1).setValue(title);
    return jsonOutput_({ success: true, message: "Judul video berhasil diubah." });
  }
  return jsonOutput_({ success: false, message: "Video tidak ditemukan." });
}

function getActiveVideoBands_() {
  const grouped = {};
  getActiveVideos_("").forEach(video => {
    const artist = String(video.artist || "").trim() || "Tanpa Nama Band";
    const key = normalizeBandName_(artist);
    if (!grouped[key]) {
      grouped[key] = { artist: artist, video_count: 0, cover_url: video.cover_url || "" };
    }
    grouped[key].video_count++;
    if (!grouped[key].cover_url && video.cover_url) grouped[key].cover_url = video.cover_url;
  });
  return Object.keys(grouped)
    .map(key => grouped[key])
    .sort((a, b) => a.artist.localeCompare(b.artist));
}

function getVideoChannels_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  return readSheetObjects_(ensureSheetWithHeaders_(ss, SHEET_VIDEO_CHANNELS, VIDEO_CHANNEL_HEADERS))
    .filter(row => isActiveStatus_(row.status))
    .map(row => ({ id: row.id || "", artist: row.artist || "", channel_url: row.channel_url || "" }));
}

function normalizeBandName_(value) {
  return String(value || "").trim().toLowerCase();
}

function handleYoutubeChannelImport_(payload) {
  const admin = getActiveAdminOrNull_(payload);
  const artist = String(payload.artist || payload.bandName || "").trim();
  const channelUrl = normalizeYoutubeChannelUrl_(payload.channel_url || payload.channelUrl || payload.videoUrl || "");
  if (!artist) return jsonOutput_({ success: false, message: "Nama band/channel wajib diisi." });
  if (!channelUrl) return jsonOutput_({ success: false, message: "Link channel YouTube wajib diisi." });

  const channelInfo = resolveYoutubeChannelInfo_(channelUrl);
  if (!channelInfo.channelId) {
    return jsonOutput_({ success: false, message: "Channel YouTube tidak ditemukan atau tidak publik." });
  }

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEO_CHANNELS, VIDEO_CHANNEL_HEADERS);
  const existing = readSheetObjects_(sheet).some(row =>
    String(row.channel_id || "").trim() === channelInfo.channelId
  );

  if (!existing) {
    sheet.appendRow([
      "channel_" + Date.now(),
      artist,
      channelInfo.url,
      channelInfo.channelId,
      admin ? "ACTIVE" : "PENDING",
      new Date()
    ]);
  }

  return jsonOutput_({
    success: true,
    message: existing ? "Channel sudah terdaftar." : (admin ? "Channel YouTube berhasil ditambahkan." : "Channel dikirim dan menunggu persetujuan admin."),
    channel: {
      artist: artist,
      channelId: channelInfo.channelId,
      channelUrl: channelInfo.url,
      channelTitle: channelInfo.title || artist
    }
  });
}

function handleYoutubeChannelDelete_(payload) {
  validateActiveAdmin_(payload);
  const channelId = String(payload.channel_id || payload.channelId || payload.id || "").trim();
  if (!channelId) return jsonOutput_({ success: false, message: "ID channel wajib diisi." });
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEO_CHANNELS, VIDEO_CHANNEL_HEADERS);
  const values = sheet.getDataRange().getValues();
  const headers = getHeaderMap_(values[0] || []);
  const idIndex = headers["id"];
  if (idIndex === undefined) return jsonOutput_({ success: false, message: "Kolom ID channel tidak ditemukan." });
  for (let row = 1; row < values.length; row++) {
    if (String(values[row][idIndex] || "").trim() !== channelId) continue;
    sheet.deleteRow(row + 1);
    return jsonOutput_({ success: true, message: "Channel berhasil dihapus.", deleted_id: channelId });
  }
  return jsonOutput_({ success: false, message: "Channel tidak ditemukan." });
}

function isActiveStatus_(status) {
  const value = String(status || "").trim().toLowerCase();
  return value === "" || value === "active" || value === "approved";
}

function normalizeYoutubeChannelUrl_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "";
  if (raw.charAt(0) === "@") return "https://www.youtube.com/" + raw;
  if (/^https?:\/\//i.test(raw)) return raw;
  if (/^www\./i.test(raw)) return "https://" + raw;
  return "https://www.youtube.com/" + raw.replace(/^\/+/, "");
}

function resolveYoutubeChannelInfo_(channelUrl) {
  const normalizedUrl = normalizeYoutubeChannelUrl_(channelUrl);
  const directMatch = normalizedUrl.match(/youtube\.com\/channel\/(UC[A-Za-z0-9_-]{20,})/i);
  if (directMatch && directMatch[1]) {
    return { channelId: directMatch[1], title: "", url: "https://www.youtube.com/channel/" + directMatch[1] };
  }
  const html = fetchText_(normalizedUrl);
  const channelId = extractYoutubeChannelIdFromText_(html);
  return {
    channelId: channelId,
    title: extractYoutubeChannelTitleFromHtml_(html),
    url: channelId ? "https://www.youtube.com/channel/" + channelId : normalizedUrl
  };
}

function extractYoutubeChannelIdFromText_(text) {
  const raw = String(text || "");
  const patterns = [
    /"channelId"\s*:\s*"(UC[A-Za-z0-9_-]{20,})"/,
    /"externalId"\s*:\s*"(UC[A-Za-z0-9_-]{20,})"/,
    /\/channel\/(UC[A-Za-z0-9_-]{20,})/i
  ];
  for (let i = 0; i < patterns.length; i++) {
    const match = raw.match(patterns[i]);
    if (match && match[1]) return match[1];
  }
  return "";
}

function extractYoutubeChannelTitleFromHtml_(html) {
  const raw = String(html || "");
  const title = raw.match(/<title>([^<]+)<\/title>/i);
  return title && title[1] ? decodeHtmlEntities_(title[1]).replace(/\s*-\s*YouTube\s*$/i, "").trim() : "";
}

function fetchText_(url) {
  const response = UrlFetchApp.fetch(url, {
    muteHttpExceptions: true,
    followRedirects: true,
    headers: { "User-Agent": "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36 Chrome/124 Safari/537.36" }
  });
  return response.getResponseCode() >= 200 && response.getResponseCode() < 300 ? response.getContentText() : "";
}

function fetchYoutubeChannelVideos_(channelId, maxItems) {
  const items = [];
  const seen = {};
  function add(item) {
    const videoId = String(item && item.videoId || "").trim();
    if (!videoId || seen[videoId]) return;
    seen[videoId] = true;
    items.push({
      videoId: videoId,
      url: "https://www.youtube.com/watch?v=" + videoId,
      title: String(item.title || "").trim() || "Video YouTube",
      description: String(item.description || "").trim(),
      thumbnailUrl: String(item.thumbnailUrl || "").trim() || buildYoutubeThumbnailUrl_(videoId),
      year: String(item.year || "").trim()
    });
  }
  fetchYoutubeChannelRssVideos_(channelId).forEach(add);
  parseYoutubeVideoRendererItems_(fetchText_(
    "https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/videos?view=0&sort=dd&flow=grid"
  )).forEach(add);
  return items.slice(0, Math.min(Math.max(Number(maxItems) || 100, 1), 100));
}

function fetchYoutubeChannelRssVideos_(channelId) {
  const items = [];
  try {
    const text = fetchText_("https://www.youtube.com/feeds/videos.xml?channel_id=" + encodeURIComponent(channelId));
    if (!text) return items;
    const root = XmlService.parse(text).getRootElement();
    const atom = XmlService.getNamespace("http://www.w3.org/2005/Atom");
    const media = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    const yt = XmlService.getNamespace("yt", "http://www.youtube.com/xml/schemas/2015");
    root.getChildren("entry", atom).forEach(entry => {
      const videoId = entry.getChild("videoId", yt);
      const title = entry.getChild("title", atom);
      const group = entry.getChild("group", media);
      const description = group && group.getChild("description", media);
      const thumbnail = group && group.getChild("thumbnail", media);
      const published = entry.getChild("published", atom);
      const id = videoId ? videoId.getText() : "";
      if (!id) return;
      items.push({
        videoId: id,
        title: title ? title.getText() : "",
        description: description ? description.getText() : "",
        thumbnailUrl: thumbnail ? String(thumbnail.getAttribute("url").getValue() || "") : "",
        year: published ? String(published.getText() || "").substring(0, 4) : ""
      });
    });
  } catch (error) {
    Logger.log("YouTube RSS gagal: " + (error && error.message ? error.message : String(error)));
  }
  return items;
}

function parseYoutubeVideoRendererItems_(text) {
  const raw = String(text || "").replace(/\\u0026/g, "&").replace(/\\u003d/g, "=").replace(/\\u002f/g, "/");
  const items = [];
  const seen = {};
  const detailed = /"videoId"\s*:\s*"([A-Za-z0-9_-]{11})"[\s\S]{0,2500}?"title"\s*:\s*\{[\s\S]{0,500}?"text"\s*:\s*"([^"]*)"/g;
  let match;
  while ((match = detailed.exec(raw)) !== null) {
    const id = match[1];
    if (seen[id]) continue;
    seen[id] = true;
    items.push({ videoId: id, title: decodeHtmlEntities_(match[2]), description: "", thumbnailUrl: buildYoutubeThumbnailUrl_(id), year: "" });
  }
  return items;
}

function buildYoutubeThumbnailUrl_(videoId) {
  return videoId ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg" : "";
}

function decodeHtmlEntities_(text) {
  return String(text || "").replace(/&amp;/g, "&").replace(/&quot;/g, "\"").replace(/&#39;/g, "'").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
}

// ─── SHEET HELPERS ────────────────────────────────────────────────────────────
function ensureSheetWithHeaders_(ss, sheetName, headers) {
  const sheet = ss.getSheetByName(sheetName) || ss.insertSheet(sheetName);
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
  } else {
    const current = sheet.getRange(1, 1, 1, headers.length).getValues()[0];
    const mismatch = headers.some((h, i) => String(current[i] || "").trim() !== h);
    if (mismatch) sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  }
  return sheet;
}

function ensureAdminSheetWithDefaults_(ss) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_ADMIN, ADMIN_HEADERS);
  const values = sheet.getDataRange().getValues();
  const hasAdmin = values.length > 1 && values.slice(1).some(
    row => String(row[0] || "").trim().toLowerCase() === "admin"
  );
  if (!hasAdmin) sheet.appendRow(["admin", "admin", "ACTIVE"]);
  return sheet;
}

function readSheetObjects_(sheet) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];
  const headers = values[0].map(h => String(h || "").trim());
  return values.slice(1).map(row => {
    const obj = {};
    headers.forEach((h, i) => { obj[h] = row[i]; });
    return obj;
  });
}

function getHeaderMap_(headersRow) {
  const col = {};
  headersRow.forEach((h, i) => { col[String(h || "").trim()] = i; });
  return col;
}

// ─── DRIVE HELPERS ────────────────────────────────────────────────────────────
function uploadOrReplacePortalPoster_(base64, fileName, mimeType, storageKey) {
  try {
    const folder = DriveApp.getFolderById(PORTAL_POSTER_FOLDER_ID);
    const safeFileName = String(fileName || "poster.jpg").trim() || "poster.jpg";
    const description = "portal-poster-storage-key:" + storageKey;
    const previousFiles = folder.getFilesByName(safeFileName);
    let previousFile = null;
    let replacedUrl = "";
    while (previousFiles.hasNext()) {
      const candidate = previousFiles.next();
      if (candidate.getDescription() !== description) continue;
      previousFile = candidate;
      replacedUrl = "https://drive.google.com/thumbnail?id=" + candidate.getId() + "&sz=w2000";
      break;
    }

    const cleanBase64 = String(base64 || "").replace(/^data:.*;base64,/, "");
    const bytes = Utilities.base64Decode(cleanBase64);
    const blob = Utilities.newBlob(bytes, mimeType, safeFileName);
    const file = folder.createFile(blob);
    file.setDescription(description);
    try { file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW); } catch (e) {}
    if (previousFile) previousFile.setTrashed(true);
    return {
      posterUrl: "https://drive.google.com/thumbnail?id=" + file.getId() + "&sz=w2000",
      replacedUrl: replacedUrl
    };
  } catch (error) {
    throw new Error("Upload poster ke Drive gagal: " + (error && error.message ? error.message : String(error)));
  }
}

function uploadBase64ImageToDrive_(base64, fileName, mimeType, folderId) {
  try {
    const folder = DriveApp.getFolderById(folderId);
    const cleanBase64 = String(base64 || "").replace(/^data:.*;base64,/, "");
    const bytes = Utilities.base64Decode(cleanBase64);
    const blob = Utilities.newBlob(bytes, mimeType, fileName);
    const file = folder.createFile(blob);
    try { file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW); } catch (e) {}
    return "https://drive.google.com/thumbnail?id=" + file.getId() + "&sz=w2000";
  } catch (error) {
    throw new Error("Upload ke Drive gagal: " + (error && error.message ? error.message : String(error)));
  }
}

// ─── DATE HELPERS ─────────────────────────────────────────────────────────────
function parseDateValue_(value) {
  if (!value) return 0;
  if (Object.prototype.toString.call(value) === "[object Date]") return value.getTime();
  const parsed = new Date(value);
  return isNaN(parsed.getTime()) ? 0 : parsed.getTime();
}

function formatDateValue_(value) {
  const timestamp = parseDateValue_(value);
  if (!timestamp) return "";
  return Utilities.formatDate(new Date(timestamp), "Asia/Jakarta", "yyyy-MM-dd'T'HH:mm:ssXXX");
}

function jsonOutput_(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}
