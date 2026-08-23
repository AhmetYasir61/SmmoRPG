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

/**
 * How long a server stays listed after its last heartbeat, in seconds.
 *
 * Long enough to survive a restart or a slow tick, short enough that a server that has
 * actually gone away disappears from the list rather than sending players at nothing.
 */
const SERVER_TTL = 120;

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

/**
 * The fallback key header.
 *
 * Some hosts strip Authorization no matter what the .htaccess says — it is special-cased
 * by the server for security reasons and not every plan lets you override that. An ordinary
 * custom header is passed through untouched everywhere, so the mod sends the key both ways
 * and either one is accepted. Deliberately not a query parameter: those end up in access
 * logs, and a secret in a log file is a secret you have given away.
 */
function apiKeyHeader(): string
{
    $value = $_SERVER['HTTP_X_API_KEY'] ?? '';

    if ($value === '' && function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        $value = $headers['X-Api-Key'] ?? $headers['x-api-key'] ?? '';
    }
    return (string) $value;
}

/** Constant-time comparison, so the key cannot be guessed a character at a time. */
function authorised(): bool
{
    return hash_equals(API_KEY, bearerToken()) || hash_equals(API_KEY, apiKeyHeader());
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
// server directory
// ---------------------------------------------------------------------------

function serverFile(): string
{
    return DATA_DIR . '/servers.json';
}

/** Every server that has checked in recently, oldest entries dropped. */
function liveServers(): array
{
    $file = serverFile();
    if (!is_file($file)) {
        return [];
    }

    $all = json_decode((string) file_get_contents($file), true);
    if (!is_array($all)) {
        return [];
    }

    $now = time();
    $live = [];
    foreach ($all as $server) {
        if (!is_array($server) || ($now - (int) ($server['seen'] ?? 0)) > SERVER_TTL) {
            continue;
        }
        // 'seen' is bookkeeping; the client has no use for it.
        unset($server['seen']);
        $live[] = $server;
    }

    usort($live, fn($a, $b) => ($b['players'] ?? 0) <=> ($a['players'] ?? 0));
    return $live;
}

/** Records or refreshes one server's entry, keyed by its address. */
function heartbeat(array $body): void
{
    $file = serverFile();
    $all = is_file($file) ? json_decode((string) file_get_contents($file), true) : [];
    if (!is_array($all)) {
        $all = [];
    }

    $address = (string) $body['address'];

    // Only the fields the list actually shows are stored. Echoing back whatever a server
    // sent would let one of them put arbitrary text on every player's screen.
    $all[$address] = [
        'address' => $address,
        'name' => mb_substr((string) ($body['name'] ?? $address), 0, 48),
        'players' => max(0, (int) ($body['players'] ?? 0)),
        'max_players' => max(0, (int) ($body['max_players'] ?? 0)),
        'version' => mb_substr((string) ($body['version'] ?? ''), 0, 24),
        'seen' => time(),
    ];

    // Drop anything long dead so the file cannot grow without bound.
    $cutoff = time() - SERVER_TTL * 10;
    $all = array_filter($all, fn($s) => is_array($s) && (int) ($s['seen'] ?? 0) >= $cutoff);

    writeAtomic($file, json_encode($all, JSON_UNESCAPED_SLASHES));
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

/*
 * Diagnostics for exactly one problem: "I sent the key and still got 401."
 *
 * It reports which headers actually survived the trip and whether either matched — as
 * booleans only. It never echoes a key, so it is safe to leave reachable and safe to paste
 * into a support thread.
 */
if (($segments[0] ?? '') === 'whoami') {
    respond(200, [
        'authorization_header_arrived' => bearerToken() !== '',
        'x_api_key_header_arrived' => apiKeyHeader() !== '',
        'authorized' => authorised(),
        'key_configured' => API_KEY !== 'CHANGE-ME-to-a-long-random-string',
    ]);
}

/*
 * The public server list.
 *
 * Unauthenticated on purpose. A client reads this from the title screen, before it has
 * joined anything and therefore before it could have been given a key — and shipping a key
 * to every client to read a list of public addresses would be giving away the account
 * service to protect nothing.
 *
 * Writes are a different matter and are handled below, behind the key.
 */
if (($segments[0] ?? '') === 'servers' && $method === 'GET' && !isset($segments[1])) {
    respond(200, ['servers' => liveServers()]);
}

if (($segments[0] ?? '') === 'servers' && ($segments[1] ?? '') === 'heartbeat') {
    if (!authorised()) {
        respond(401, ['error' => 'unauthorised']);
    }
    if ($method !== 'POST') {
        respond(405, ['error' => 'method not allowed']);
    }
    $body = json_decode((string) file_get_contents('php://input'), true);
    if (!is_array($body) || empty($body['address'])) {
        respond(400, ['error' => 'bad body']);
    }
    heartbeat($body);
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
