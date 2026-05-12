# 빌드 컨텍스트는 모노레포 루트(.). pnpm workspace 의존성 해결을 위해 루트 기준으로 빌드한다.
# .gitlab-ci.yml 은 `docker build -f web/Dockerfile .` 형태로 호출한다.

FROM node:22-alpine AS deps
RUN corepack enable && corepack prepare pnpm@10.32.0 --activate
WORKDIR /workspace
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY web/package.json ./web/
RUN pnpm install --frozen-lockfile

FROM node:22-alpine AS builder
RUN corepack enable && corepack prepare pnpm@10.32.0 --activate
WORKDIR /workspace
COPY --from=deps /workspace/node_modules ./node_modules
COPY --from=deps /workspace/web/node_modules ./web/node_modules
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY web ./web
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm --filter ./web build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=8000 \
    HOSTNAME=0.0.0.0
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder --chown=app:app /workspace/web/.next/standalone ./
COPY --from=builder --chown=app:app /workspace/web/.next/static ./web/.next/static
COPY --from=builder --chown=app:app /workspace/web/public ./web/public
USER app
EXPOSE 8000
WORKDIR /app/web
CMD ["node", "server.js"]
