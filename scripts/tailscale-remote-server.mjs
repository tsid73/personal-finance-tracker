import { createReadStream, existsSync } from "node:fs";
import { readFile, stat } from "node:fs/promises";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, "..");
const distDir = path.join(rootDir, "apps", "web", "dist");
const host = process.env.TAILSCALE_BIND_IP;
const port = Number(process.env.REMOTE_PORT ?? "5173");
const apiTargetHost = "127.0.0.1";
const apiTargetPort = 4000;

if (!host) {
  throw new Error("TAILSCALE_BIND_IP is required.");
}

if (!existsSync(distDir)) {
  throw new Error(`Frontend build output not found at ${distDir}. Run the web build first.`);
}

const contentTypes = new Map([
  [".html", "text/html; charset=utf-8"],
  [".js", "application/javascript; charset=utf-8"],
  [".css", "text/css; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".svg", "image/svg+xml"],
  [".png", "image/png"],
  [".jpg", "image/jpeg"],
  [".jpeg", "image/jpeg"],
  [".ico", "image/x-icon"],
  [".woff", "font/woff"],
  [".woff2", "font/woff2"]
]);

function setCommonHeaders(response, contentType) {
  response.setHeader("Content-Type", contentType);
  response.setHeader("X-Content-Type-Options", "nosniff");
  response.setHeader("X-Frame-Options", "DENY");
  response.setHeader("Referrer-Policy", "same-origin");
}

function sendJson(response, statusCode, payload) {
  response.statusCode = statusCode;
  setCommonHeaders(response, "application/json; charset=utf-8");
  response.end(JSON.stringify(payload));
}

function proxyApi(request, response) {
  const upstream = http.request(
    {
      host: apiTargetHost,
      port: apiTargetPort,
      method: request.method,
      path: request.url,
      headers: {
        ...request.headers,
        host: `${apiTargetHost}:${apiTargetPort}`
      }
    },
    (upstreamResponse) => {
      response.writeHead(upstreamResponse.statusCode ?? 502, upstreamResponse.headers);
      upstreamResponse.pipe(response);
    }
  );

  upstream.on("error", (error) => {
    sendJson(response, 502, {
      message: "Unable to reach the local finance API.",
      detail: error instanceof Error ? error.message : "Unknown upstream error."
    });
  });

  request.pipe(upstream);
}

async function serveStatic(request, response) {
  const rawPath = decodeURIComponent((request.url ?? "/").split("?")[0] || "/");
  const normalizedPath = rawPath === "/" ? "/index.html" : rawPath;
  const requestedPath = path.normalize(normalizedPath).replace(/^(\.\.[/\\])+/, "");
  let filePath = path.join(distDir, requestedPath);

  try {
    const fileStat = await stat(filePath);
    if (fileStat.isDirectory()) {
      filePath = path.join(filePath, "index.html");
    }
  } catch {
    filePath = path.join(distDir, "index.html");
  }

  try {
    const fileStat = await stat(filePath);
    if (!fileStat.isFile()) {
      throw new Error("Not a file");
    }
  } catch {
    response.statusCode = 404;
    setCommonHeaders(response, "text/plain; charset=utf-8");
    response.end("Not found");
    return;
  }

  const extension = path.extname(filePath);
  const contentType = contentTypes.get(extension) ?? "application/octet-stream";
  setCommonHeaders(response, contentType);
  if (filePath.endsWith(".html")) {
    response.setHeader("Cache-Control", "no-store");
  } else {
    response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
  }

  createReadStream(filePath).pipe(response);
}

const server = http.createServer(async (request, response) => {
  if (!request.url) {
    sendJson(response, 400, { message: "Missing request URL." });
    return;
  }

  if (request.url.startsWith("/api/")) {
    proxyApi(request, response);
    return;
  }

  if (request.url === "/health") {
    const indexExists = existsSync(path.join(distDir, "index.html"));
    sendJson(response, 200, { ok: indexExists });
    return;
  }

  await serveStatic(request, response);
});

server.listen(port, host, async () => {
  const packageJson = await readFile(path.join(rootDir, "package.json"), "utf8");
  const packageName = JSON.parse(packageJson).name ?? "personal-finance-tracker";
  console.log(`${packageName} remote access server listening on http://${host}:${port}`);
});
