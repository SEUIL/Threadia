FROM node:22-bookworm-slim AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-bookworm-slim AS build
WORKDIR /app

COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

FROM node:22-bookworm-slim AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN groupadd --system --gid 10001 threadia && \
    useradd --system --uid 10001 --gid threadia --home-dir /app --shell /usr/sbin/nologin threadia

COPY --from=build --chown=threadia:threadia /app/public ./public
COPY --from=build --chown=threadia:threadia /app/.next/standalone ./
COPY --from=build --chown=threadia:threadia /app/.next/static ./.next/static

USER threadia

EXPOSE 3000

CMD ["node", "server.js"]
