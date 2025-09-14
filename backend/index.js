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

app.use(cors());
app.use(bodyParser.json());

// Serve static webapp from ../webapp
app.use(express.static(path.join(__dirname, '..', 'webapp')));

// Ensure data file exists
if (!fs.existsSync(DATA_FILE)) {
  fs.writeFileSync(DATA_FILE, '[]', 'utf-8');
}

app.get('/api/clients', (req, res) => {
  try {
    const data = fs.readFileSync(DATA_FILE, 'utf-8');
    res.json(JSON.parse(data));
  } catch (e) {
    res.status(500).json({ error: 'Cannot read clients' });
  }
});

app.post('/api/clients', (req, res) => {
  try {
    const client = req.body;
    const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
    data.push({
      name: client.name || '',
      phone: client.phone || '',
      email: client.email || ''
    });
    fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: 'Cannot save client' });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Backend running on http://0.0.0.0:${PORT}`);
});
