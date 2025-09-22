import express from 'express';
import cors from 'cors';
import bodyParser from 'body-parser';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 4000;
const DATA_FILE = path.join(__dirname, 'clients.json');
const MESSAGES_FILE = path.join(__dirname, 'messages.json');

app.use(cors());
app.use(bodyParser.json());

// In-memory SSE clients
const sseClients = new Set();

// Serve static files from backend/public (Express default)
app.use(express.static(path.join(__dirname, 'public')));
// Serve static webapp from ../webapp (opzionale)
app.use(express.static(path.join(__dirname, '..', 'webapp')));

// Ensure data file exists
if (!fs.existsSync(DATA_FILE)) {
  fs.writeFileSync(DATA_FILE, '[]', 'utf-8');
}
if (!fs.existsSync(MESSAGES_FILE)) {
  fs.writeFileSync(MESSAGES_FILE, '[]', 'utf-8');
}

// API to get all messages
app.get('/api/messages', (req, res) => {
  try {
    const messages = JSON.parse(fs.readFileSync(MESSAGES_FILE, 'utf-8'));
    res.json(messages);
  } catch (e) {
    res.status(500).json({ error: 'Cannot read messages', details: e.message });
  }
});

app.get('/api/clients', (req, res) => {
  try {
    const data = fs.readFileSync(DATA_FILE, 'utf-8');
    res.json(JSON.parse(data));
  } catch (e) {
    console.error('Errore lettura clients:', e);
    res.status(500).json({ error: 'Cannot read clients', details: e.message });
  }
});

// SSE stream for real-time client events
app.get('/api/clients/stream', (req, res) => {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.flushHeaders?.();
  res.write(`: connected\n\n`);

  sseClients.add(res);

  // Heartbeat to keep the connection alive (every 25s)
  const hb = setInterval(() => {
    try { res.write(`: ping\n\n`); } catch (_) {}
  }, 25_000);

  req.on('close', () => {
    clearInterval(hb);
    sseClients.delete(res);
    try { res.end(); } catch (_) {}
  });
});

app.post('/api/clients', (req, res) => {
  try {
    const client = req.body;
    const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
    const newClient = {
      name: client.name || '',
      surname: client.surname || '',
      phone: client.phone || '',
      email: client.email || '',
      id: Date.now(),
      createdAt: Date.now()
    };
    data.push(newClient);
    fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));

    // Save also to messages.json
    let messages = [];
    try {
      messages = JSON.parse(fs.readFileSync(MESSAGES_FILE, 'utf-8'));
    } catch {}
    const newMessage = {
      type: 'registration',
      text: `Nuovo cliente registrato: ${newClient.name} ${newClient.surname} (${newClient.phone}${newClient.email ? ', ' + newClient.email : ''})`,
      client: newClient,
      timestamp: Date.now()
    };
    messages.push(newMessage);
    fs.writeFileSync(MESSAGES_FILE, JSON.stringify(messages, null, 2));

    res.json({ ok: true });

    // Broadcast SSE event
    const payload = `data: ${JSON.stringify({ type: 'client-created', client: newClient })}\n\n`;
    for (const r of [...sseClients]) {
      try { r.write(payload); } catch (e) { sseClients.delete(r); }
    }
  } catch (e) {
    console.error('Errore salvataggio client:', e);
    res.status(500).json({ error: 'Cannot save client', details: e.message });
  }
});

// Simple stats endpoint to know if someone registered
app.get('/api/stats', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const withCreated = data.map(c => ({ ...c, createdAt: c.createdAt ?? c.id ?? 0 }));
    const total = withCreated.length;
    const today = withCreated.filter(c => (c.createdAt || 0) >= startOfToday).length;
    const lastRegistrationAt = withCreated.reduce((m, c) => Math.max(m, c.createdAt || 0), 0) || null;
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const dayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i).getTime();
      const dayEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i + 1).getTime();
      const count = withCreated.filter(c => (c.createdAt || 0) >= dayStart && (c.createdAt || 0) < dayEnd).length;
      days.push({ dayStart, count });
    }
    res.json({ total, today, last7Days: days, lastRegistrationAt });
  } catch (e) {
    res.status(500).json({ error: 'Cannot compute stats', details: e.message });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Backend running on http://0.0.0.0:${PORT}`);
});
