import { defineConfig } from "@playwright/test";

const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3100";
const apiBaseURL = process.env.E2E_API_BASE_URL ?? `${baseURL}/backend-api`;
const managedStack =
  process.env.E2E_MANAGED_STACK === "1" ||
  Boolean(process.env.E2E_BASE_URL) ||
  Boolean(process.env.E2E_API_BASE_URL);

process.env.E2E_API_BASE_URL = apiBaseURL;

export default defineConfig({
  testDir: "./tests/e2e",
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  webServer: managedStack
    ? undefined
    : [
        {
          command:
            "cmd /c \"set SPRING_PROFILES_ACTIVE=test&& set SERVER_PORT=8081&& set CORS_ALLOWED_ORIGINS=http://localhost:3100,http://127.0.0.1:3100&& mvn -q -f ..\\backend\\pom.xml -DskipTests -Dspring-boot.run.useTestClasspath=true spring-boot:run\"",
          url: "http://localhost:8081/api/health",
          reuseExistingServer: true,
          timeout: 180_000
        },
        {
          command:
            "cmd /c \"set PORT=3100&& set HOSTNAME=127.0.0.1&& set API_PROXY_TARGET=http://localhost:8081/api&& set NEXT_PUBLIC_API_BASE_URL=/backend-api&& npm run dev:e2e\"",
          url: baseURL,
          reuseExistingServer: false,
          timeout: 240_000
        }
      ],
  use: {
    baseURL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure"
  }
});
