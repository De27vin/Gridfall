FROM node:22-bookworm-slim

WORKDIR /app

RUN apt-get update -y \
    && apt-get install -y openssl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
COPY prisma ./prisma

RUN npm ci
RUN npx prisma generate

COPY tsconfig.json ./
COPY src ./src

RUN npm run build

EXPOSE 8080

CMD ["sh", "-c", "npx prisma migrate deploy && node dist/server.js"]