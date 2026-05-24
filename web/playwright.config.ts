import { defineConfig, devices, type TraceMode } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

loadEnvFile(".env");

const PORT = Number(process.env.WEB_E2E_PORT ?? 3000);
const HOST = process.env.WEB_E2E_HOST ?? "localhost";
const baseURL = process.env.WEB_E2E_BASE_URL ?? `http://${HOST}:${PORT}`;
const shouldStartWebServer = !process.env.WEB_E2E_BASE_URL;
const traceMode = (process.env.WEB_E2E_TRACE ?? "on") as TraceMode;

export default defineConfig({
  testDir: "./e2e",
  timeout: 120_000,
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL,
    trace: {
      mode: traceMode,
      snapshots: true,
      screenshots: true,
      sources: true,
      attachments: true,
    },
    screenshot: "only-on-failure",
    video: "on-first-retry",
  },
  webServer: shouldStartWebServer
    ? {
        command: `pnpm dev --hostname ${HOST} --port ${PORT}`,
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
        env: {
          NEXT_PUBLIC_E2E_AUTH_BOOTSTRAP: "1",
        },
      }
    : undefined,
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});

function loadEnvFile(fileName: string) {
  const envPath = path.join(process.cwd(), fileName);
  if (!fs.existsSync(envPath)) {
    return;
  }

  for (const line of fs.readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#") || !trimmed.includes("=")) {
      continue;
    }

    const [rawKey, ...rawValue] = trimmed.split("=");
    const key = rawKey.trim();
    const value = rawValue.join("=").trim().replace(/^['"]|['"]$/g, "");

    if (key && process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}
