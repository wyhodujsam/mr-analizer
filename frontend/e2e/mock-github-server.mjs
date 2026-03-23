import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FIXTURES_DIR = path.resolve(__dirname, '../../backend/src/test/resources/github-fixtures');
const PORT = 9999;

function loadFixture(name) {
  return fs.readFileSync(path.join(FIXTURES_DIR, name), 'utf-8');
}

const routes = [
  [/\/repos\/[^/]+\/[^/]+\/pulls\/1\/files/, 'pr-files-1.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/2\/files/, 'pr-files-2.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/3\/files/, 'pr-files-3.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/1\/reviews/, 'pr-reviews-approved.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/2\/reviews/, 'pr-reviews-empty.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/3\/reviews/, 'pr-reviews-approved.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/1$/, 'pr-single-1.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/2$/, 'pr-single-2.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\/3$/, 'pr-single-3.json'],
  [/\/repos\/[^/]+\/[^/]+\/pulls\?/, 'pr-list-3.json'],
];

const server = http.createServer((req, res) => {
  const url = req.url ?? '';
  res.setHeader('Content-Type', 'application/json');

  for (const [pattern, fixture] of routes) {
    if (pattern.test(url)) {
      res.writeHead(200);
      res.end(loadFixture(fixture));
      return;
    }
  }

  console.log(`[mock-github] 404: ${req.method} ${url}`);
  res.writeHead(404);
  res.end(JSON.stringify({ message: `Not found: ${url}` }));
});

server.listen(PORT, () => {
  console.log(`Mock GitHub API on http://localhost:${PORT}`);
});
