FROM node:20-alpine

WORKDIR /app

# Install production dependencies first so this layer caches across code changes.
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev || npm install --omit=dev

COPY src ./src
COPY migrations ./migrations

ENV NODE_ENV=production
ENV PORT=8080
EXPOSE 8080

# DigitalOcean App Platform health check hits /health.
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD node -e "fetch('http://127.0.0.1:8080/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

USER node
CMD ["node", "src/index.js"]
