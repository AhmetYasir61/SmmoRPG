<?php
/**
 * SmmoRPG account service — shared-hosting edition.
 *
 * Written in PHP rather than Node because that is what shared hosting actually runs. A
 * .js file dropped in public_html is not executed by Apache; it is either served to
 * anyone who asks for it as plain text, or refused with a 403. Neither is a service.
 *
 * Upload this as:   public_html/PmmoRPG_Api/index.php
 * Alongside:        public_html/PmmoRPG_Api/.htaccess   (routing + header fix + data lock)
 *
 * Then set, on every game server:
 *   backendUrl    = "https://your-domain.tld/PmmoRPG_Api"
 *   backendApiKey = "<the same string as API_KEY below>"
 */

declare(strict_types=1);

// ---------------------------------------------------------------------------
// configuration
// ---------------------------------------------------------------------------

/**
 * The shared secret. Must match backendApiKey in smmorpg-server.toml.
 *
 * Replace this before uploading. A long random string — 40+ characters, letters and
 * digits. Anyone who learns it can read and rewrite every account, so it does not belong
 * in a repository, a screenshot or a support message.
 */
const API_KEY = 'CHANGE-ME-to-a-long-random-string';

/**
 * Where accounts are stored.
 *
 * Deliberately one level above public_html. A data directory inside the web root is a
 * directory the whole internet can download account files out of, and no amount of
 * obscure naming changes that. The .htaccess is a second line of defence, not the first.
 */
const DATA_DIR = __DIR__ . '/../../smmorpg_data';

// ---------------------------------------------------------------------------
// plumbing
// ---------------------------------------------------------------------------

header('Content-Type: application/json');

/** Ends the request with a status and a JSON body. */
function respond(int $status, array $body = []): never
{
    http_response_code($status);
    echo json_encode($body, JSON_UNESCAPED_SLASHES);
    exit;
}

/**
 * Reads the bearer token.
 *
 * Apache under CGI/FastCGI strips the Authorization header unless it is explicitly passed
 * through, which is why the .htaccess sets REDIRECT_HTTP_AUTHORIZATION. Both spellings are
 * checked here so the service works on hosts that do it either way.
 */
function bearerToken(): string
{
    $header = $_SERVER['HTTP_AUTHORIZATION']
        ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
        ?? '';

    if ($header === '' && function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        $header = $headers['Authorization'] ?? $headers['authorization'] ?? '';
    }

    return str_starts_with($header, 'Bearer ') ? substr($header, 7) : '';
}

/** Constant-time comparison, so the key cannot be guessed a character at a time. */
function authorised(): bool
{
    return hash_equals(API_KEY, bearerToken());
}

/** The path segments after this script, e.g. ["accounts", "<uuid>"]. */
function segments(): array
{
    $path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?? '/';
    $base = rtrim(dirname($_SERVER['SCRIPT_NAME'] ?? ''), '/');

    if ($base !== '' && str_starts_with($path, $base)) {
        $path = substr($path, strlen($base));
    }
    return array_values(array_filter(explode('/', trim($path, '/')), fn($s) => $s !== ''));
}

/** Rejects anything that is not a plain UUID, so a path can never escape DATA_DIR. */
function accountFile(string $uuid): string
{
    if (!preg_match('/^[0-9a-fA-F-]{32,36}$/', $uuid)) {
        respond(400, ['error' => 'bad uuid']);
    }
    return DATA_DIR . '/' . strtolower($uuid) . '.json';
}

/**
 * Writes a file atomically.
 *
 * A plain fwrite that is interrupted leaves a half-written account behind, and a
 * half-written account is a lost account. Writing to a temporary file and renaming means a
 * reader only ever sees the old file or the new one.
 */
function writeAtomic(string $path, string $contents): void
{
    $temp = $path . '.' . bin2hex(random_bytes(6)) . '.tmp';
    if (file_put_contents($temp, $contents, LOCK_EX) === false) {
        respond(500, ['error' => 'write failed']);
    }
    if (!rename($temp, $path)) {
        @unlink($temp);
        respond(500, ['error' => 'rename failed']);
    }
}

// ---------------------------------------------------------------------------
// routing
// ---------------------------------------------------------------------------

if (!is_dir(DATA_DIR) && !@mkdir(DATA_DIR, 0700, true) && !is_dir(DATA_DIR)) {
    respond(500, ['error' => 'data directory unavailable']);
}

$segments = segments();
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// Health is the one endpoint that still answers without a key: the mod uses it to decide
// whether the service is up, and a probe that needs a secret cannot tell "down" from
// "misconfigured".
if (($segments[0] ?? '') === 'health') {
    respond(200, ['ok' => true]);
}

if (!authorised()) {
    respond(401, ['error' => 'unauthorised']);
}

if (($segments[0] ?? '') !== 'accounts' || !isset($segments[1])) {
    respond(404, ['error' => 'not found']);
}

$uuid = $segments[1];
$file = accountFile($uuid);

if ($method === 'GET') {
    if (!is_file($file)) {
        respond(404, ['error' => 'no such account']);
    }
    http_response_code(200);
    echo file_get_contents($file);
    exit;
}

if ($method === 'POST' || $method === 'PUT') {
    $raw = file_get_contents('php://input');
    $incoming = json_decode($raw, true);

    if (!is_array($incoming) || !isset($incoming['uuid'])) {
        respond(400, ['error' => 'bad body']);
    }

    /*
     * The rule that makes replays safe.
     *
     * The mod queues writes to disk during an outage and sends them again afterwards, so
     * the same body can arrive twice. Comparing revisions means the second arrival changes
     * nothing — without this, a network hiccup would turn into duplicated coins.
     *
     * It also protects the newer copy: a server that kept playing while this service was
     * down is ahead of it, and must not be overwritten by whatever arrives later.
     */
    $existing = is_file($file) ? json_decode((string) file_get_contents($file), true) : null;
    $incomingRevision = (int) ($incoming['revision'] ?? 0);
    $existingRevision = is_array($existing) ? (int) ($existing['revision'] ?? 0) : -1;

    if ($incomingRevision >= $existingRevision) {
        writeAtomic($file, json_encode($incoming, JSON_UNESCAPED_SLASHES));
    }

    // 200 either way. A rejected stale write is not an error the game server can act on,
    // and reporting one would only make it retry something that must not be applied.
    respond(200, ['ok' => true, 'revision' => max($incomingRevision, $existingRevision)]);
}

respond(405, ['error' => 'method not allowed']);
