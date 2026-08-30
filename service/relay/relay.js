'use strict';

/*
 * SmmoRPG co-op relay.
 *
 * A reverse tunnel, and nothing more. The host's game never listens on the public
 * internet; it dials out to this box and keeps one control connection open. When a guest
 * turns up with the right code, this tells the host, the host dials out again with a
 * second socket, and the two sockets are joined together. Minecraft's own bytes go over
 * that pipe untouched.
 *
 * That shape is the whole point: it needs no port forwarding on either side and works
 * behind carrier-grade NAT, where a direct connection simply cannot be made.
 *
 * What this deliberately does NOT do:
 *   - read, parse or modify a single byte of the game protocol
 *   - hold any account data, or talk to the account service
 *   - authenticate players; Mojang session servers already do that, at both ends
 *
 * It is a piece of plumbing. Treat the code as a password to somebody's game, because
 * that is exactly what it is.
 */

const net = require('net');
const crypto = require('crypto');

const PORT = parseInt(process.env.RELAY_PORT || '25599', 10);
const BIND = process.env.RELAY_BIND || '0.0.0.0';

/** A session dies if the host has not been heard from in this long. */
const HOST_TIMEOUT_MS = 90_000;

/** How long a guest waits for the host to dial back before giving up. */
const DIAL_BACK_MS = 15_000;

/** Sixteen players, and one spare so a reconnect never bounces off the ceiling. */
const MAX_GUESTS = 17;

/** Bytes allowed before the handshake line must have arrived. Keeps garbage cheap. */
const MAX_HANDSHAKE_BYTES = 4096;

/** code -> session */
const sessions = new Map();

/** connection id -> guest socket waiting to be joined */
const pending = new Map();

function log(...args) {
  console.log(new Date().toISOString(), ...args);
}

/** Codes people read aloud: no O/0, no I/1, no lower case. */
function newCode() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code;
  do {
    code = Array.from(crypto.randomBytes(6))
      .map((b) => alphabet[b % alphabet.length])
      .join('');
  } while (sessions.has(code));
  return code;
}

/**
 * Reads one newline-terminated JSON line, then hands the socket over.
 *
 * Anything already buffered past the newline is passed along too — the game may well have
 * started talking before we finished reading our own header.
 */
function readHandshake(socket, onLine) {
  let buffer = Buffer.alloc(0);
  let done = false;

  const onData = (chunk) => {
    if (done) return;
    buffer = Buffer.concat([buffer, chunk]);

    const newline = buffer.indexOf(0x0a);
    if (newline < 0) {
      if (buffer.length > MAX_HANDSHAKE_BYTES) socket.destroy();
      return;
    }

    done = true;
    socket.removeListener('data', onData);

    const line = buffer.subarray(0, newline).toString('utf8').trim();
    const rest = buffer.subarray(newline + 1);

    let message;
    try {
      message = JSON.parse(line);
    } catch (e) {
      socket.destroy();
      return;
    }
    onLine(message, rest);
  };

  socket.on('data', onData);
}

function send(socket, object) {
  if (!socket.destroyed) socket.write(JSON.stringify(object) + '\n');
}

/** Joins two sockets so each one's bytes come out of the other. */
function pipe(a, b, aLeftovers, bLeftovers) {
  if (aLeftovers && aLeftovers.length) b.write(aLeftovers);
  if (bLeftovers && bLeftovers.length) a.write(bLeftovers);

  a.pipe(b);
  b.pipe(a);

  const close = () => {
    a.destroy();
    b.destroy();
  };
  a.on('error', close);
  b.on('error', close);
  a.on('close', close);
  b.on('close', close);
}

function closeSession(session, reason) {
  if (!sessions.has(session.code)) return;
  sessions.delete(session.code);
  clearInterval(session.timer);
  session.control.destroy();
  log('session', session.code, 'closed:', reason);
}

// --- roles ---

function onHost(socket, message) {
  const code = newCode();
  const session = {
    code,
    control: socket,
    guests: 0,
    lastSeen: Date.now(),
    name: typeof message.name === 'string' ? message.name.slice(0, 32) : 'host',
    timer: null,
  };

  session.timer = setInterval(() => {
    if (Date.now() - session.lastSeen > HOST_TIMEOUT_MS) {
      closeSession(session, 'host went quiet');
    } else {
      send(socket, { op: 'ping' });
    }
  }, 20_000);

  sessions.set(code, session);
  socket.setKeepAlive(true, 15_000);

  log('session', code, 'opened by', session.name);
  send(socket, { op: 'ready', code });

  // The host's control channel only ever says "still here"; everything else it does, it
  // does by opening a fresh socket. That keeps the pipe logic away from the state.
  readLines(socket, () => {
    session.lastSeen = Date.now();
  });

  socket.on('close', () => closeSession(session, 'host disconnected'));
  socket.on('error', () => closeSession(session, 'host errored'));
}

/** Keeps reading newline-delimited JSON off a socket after the first line. */
function readLines(socket, onLine) {
  let buffer = '';
  socket.on('data', (chunk) => {
    buffer += chunk.toString('utf8');
    let newline;
    while ((newline = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, newline).trim();
      buffer = buffer.slice(newline + 1);
      if (!line) continue;
      try {
        onLine(JSON.parse(line));
      } catch (e) {
        /* a malformed keepalive is not worth dropping a session over */
      }
    }
    if (buffer.length > MAX_HANDSHAKE_BYTES) socket.destroy();
  });
}

function onGuest(socket, message, leftovers) {
  const session = sessions.get(String(message.code || '').toUpperCase());
  if (!session) {
    send(socket, { op: 'error', reason: 'no_such_code' });
    socket.end();
    return;
  }
  if (session.guests >= MAX_GUESTS) {
    send(socket, { op: 'error', reason: 'full' });
    socket.end();
    return;
  }

  const id = crypto.randomUUID();
  session.guests++;

  pending.set(id, { socket, leftovers, session, at: Date.now() });
  send(session.control, { op: 'connect', id });

  log('session', session.code, 'guest', id, 'waiting for dial-back');

  const giveUp = setTimeout(() => {
    if (!pending.has(id)) return;
    pending.delete(id);
    session.guests--;
    socket.destroy();
    log('session', session.code, 'guest', id, 'timed out');
  }, DIAL_BACK_MS);

  socket.on('close', () => {
    clearTimeout(giveUp);
    if (pending.delete(id)) session.guests--;
  });
}

function onHostData(socket, message, leftovers) {
  const waiting = pending.get(String(message.id || ''));
  if (!waiting) {
    socket.destroy();
    return;
  }
  pending.delete(message.id);

  const session = waiting.session;
  log('session', session.code, 'joined guest', message.id);

  waiting.socket.on('close', () => session.guests--);
  pipe(waiting.socket, socket, waiting.leftovers, leftovers);
}

// --- the listener ---

const server = net.createServer((socket) => {
  socket.setNoDelay(true);
  socket.setTimeout(DIAL_BACK_MS * 2, () => {
    // Only bites before a handshake lands; piped sockets get their timeout cleared.
    socket.destroy();
  });

  readHandshake(socket, (message, leftovers) => {
    socket.setTimeout(0);

    switch (message.role) {
      case 'host':
        onHost(socket, message);
        break;
      case 'hostdata':
        onHostData(socket, message, leftovers);
        break;
      case 'guest':
        onGuest(socket, message, leftovers);
        break;
      default:
        socket.destroy();
    }
  });

  socket.on('error', () => socket.destroy());
});

server.listen(PORT, BIND, () => {
  log(`SmmoRPG relay listening on ${BIND}:${PORT}`);
});
