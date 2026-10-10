// Récupère la spécification OpenAPI du backend local (Swagger est réservé aux administrateurs).
// Usage : HOOPMIND_ADMIN_USERNAME=admin HOOPMIND_ADMIN_PASSWORD=... npm run api:update
import { writeFile } from 'node:fs/promises';

const url = process.env.HOOPMIND_API_DOCS_URL ?? 'http://localhost:8083/v3/api-docs';
const username = process.env.HOOPMIND_ADMIN_USERNAME ?? 'admin';
const password = process.env.HOOPMIND_ADMIN_PASSWORD;
if (!password) {
  console.error('Définissez HOOPMIND_ADMIN_PASSWORD (compte administrateur du backend local).');
  process.exit(1);
}

const response = await fetch(url, {
  headers: { Authorization: 'Basic ' + Buffer.from(`${username}:${password}`).toString('base64') },
});
if (!response.ok) {
  console.error(`Échec ${response.status} sur ${url}`);
  process.exit(1);
}
const spec = await response.json();
await writeFile('openapi/hoopmind-api.json', JSON.stringify(spec, null, 2) + '\n');
console.log(`openapi/hoopmind-api.json mis à jour (${Object.keys(spec.paths).length} routes)`);
