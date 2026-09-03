# Alter Ego backend

Fastify and Postgres. It does four things and nothing else.

1. **Serves curated content.** The app ships with a bundle in assets, so this exists to push
   corrections between releases. A client that is current gets a 304.
2. **Owns the evidence database.** Every scientific claim, its source, its evidence level and the
   full history of every previous wording.
3. **Verifies purchases** with Google Play and stores entitlements.
4. **Collects aggregate metrics**, with the user's own words stripped out twice.

It deliberately does not store commitment history, reset reasons, notes or quotes. Those live on the
user's device. See `../docs/PRIVACY_DATA_SAFETY.md`.

## Run locally

```bash
npm install
cp .env.example .env        # fill in DATABASE_URL
npm run migrate
npm run seed
npm run dev
npm test
```

## Publishing content

```bash
# From the repository root, after editing content/
node scripts/validate-content.mjs
node scripts/sync-content.mjs

# Dry run against the server's own policy
curl -X POST http://localhost:8080/v1/admin/content/validate \
  -H "content-type: application/json" \
  -H "x-admin-key: $ADMIN_API_KEY" \
  --data @server/src/content/seed/bundle.json

# Publish
curl -X POST http://localhost:8080/v1/admin/content/publish \
  -H "content-type: application/json" \
  -H "x-admin-key: $ADMIN_API_KEY" \
  -H "x-admin-user: your.name" \
  -H "x-publish-notes: corrected the abstinence claim" \
  --data @server/src/content/seed/bundle.json
```

Publishing refuses a bundle that contains a fabricated biological measurement, shaming language, an
over-long line, or a health Moment citing a claim that does not exist. That check is in
`src/lib/contentPolicy.js` and is covered by tests.

## Deploy

`app.yaml` is a DigitalOcean App Platform spec with a managed Postgres database and a pre-deploy
migration job.

```bash
doctl apps create --spec app.yaml
```

Set `ADMIN_API_KEY` and `GOOGLE_SERVICE_ACCOUNT_B64` as encrypted secrets in the App Platform console.
Generate the admin key with `openssl rand -hex 32`.
