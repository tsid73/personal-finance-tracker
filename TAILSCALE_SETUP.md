# Tailscale Setup And Usage

This app stays local-only by default.

It is not suitable for public internet deployment in its current form because the backend still uses a fixed demo user and does not require login.

For phone access over Tailscale, use the repo's tailnet-only proxy layer instead of rebinding the app itself to the network.

## Safe Remote Access Model

The finance app itself remains bound to `127.0.0.1`.

Remote access works through a separate Node proxy that:

- binds only to the machine's Tailscale IP
- serves the built frontend
- forwards `/api/*` to the local API on `127.0.0.1:4000`
- does not expose the MySQL port
- does not rebind the main API or Vite server to `0.0.0.0`

That keeps the existing localhost hardening in place while still allowing access from devices already inside your tailnet.

## Install Tailscale On Ubuntu

Official docs:

- [Tailscale Linux install](https://tailscale.com/docs/install/linux)

Run:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```

After `tailscale up`, complete login in the browser.

Check status:

```bash
tailscale status
tailscale ip -4
```

## Install Tailscale On Phone

Install the Tailscale app on Android or iPhone and sign in using the same account.

Turn Tailscale on in the app.

## Start The Finance App Locally

```bash
bash scripts/start-dev.sh
```

Local URLs:

- `http://127.0.0.1:5173`
- `http://127.0.0.1:4000/api/health`

## Start Tailscale Access For Phone

Once Tailscale is connected on both laptop and phone:

```bash
npm run start:tailscale
```

This will:

1. confirm Tailscale is connected
2. start the app if it is not already running
3. build the web app
4. start a tailnet-only proxy on the machine's Tailscale IP
5. print the phone-access URLs

Typical output:

```text
Tailnet IP URL: http://100.x.y.z:5173
MagicDNS URL: http://your-hostname.your-tailnet.ts.net:5173
```

Open one of those URLs from the phone while Tailscale is enabled on the phone.

## Stop Tailscale Access

```bash
npm run stop:tailscale
```

This only stops the Tailscale proxy layer.

It does not stop the local app.

## Daily Steps To Use The App Again

Each time the laptop was shut down:

1. Turn on the laptop.
2. Confirm Tailscale is connected.
3. Start local app with `bash scripts/start-dev.sh`.
4. If using the phone, run `npm run start:tailscale`.
5. Open the printed Tailscale URL on the phone.

When done:

```bash
bash scripts/stop-dev.sh
```

## Turn Tailscale On

If the service is installed but disconnected:

```bash
sudo tailscale up
```

Check:

```bash
tailscale status
```

## Turn Tailscale Off

Disconnect without uninstalling:

```bash
sudo tailscale down
```

This stops the device from being connected to your tailnet until you bring it back up.

## Completely Remove Tailscale

First disconnect:

```bash
sudo tailscale down
```

Remove the package:

```bash
sudo apt remove tailscale
```

If you want to also remove package data:

```bash
sudo apt purge tailscale
sudo apt autoremove
```

If you want to remove the device from the Tailscale admin side too, delete or expire the device from your Tailscale admin console.

## Things To Consider

### Security

- This is safer than rebinding the app to all interfaces.
- The main app still stays on `127.0.0.1`.
- Only the tailnet-only proxy binds to the Tailscale IP.
- Traffic stays inside your tailnet, not on the public internet.
- This is not a substitute for app-level authentication.
- Any device on your tailnet can access the app while the proxy is running.

### Required Account Hygiene

- Use MFA on the account you use for Tailscale.
- Keep phone and laptop screen lock enabled.
- Do not share your tailnet unless you intend to share access to the app.

### Availability

- The laptop must be powered on.
- The app must be running.

### Current App Constraints

- This project still uses a development-style local app plus a small remote proxy.
- The local frontend is served from Vite on `127.0.0.1:5173`.
- The local API runs on `127.0.0.1:4000`.
- The remote proxy serves the built frontend on the Tailscale IP at port `5173`.
- There is no user login yet.

### Better Long-Term Setup

If this becomes a tool you use regularly, the next improvements should be:

1. add authentication
2. add a deliberate remote-access design instead of relying on dev-server exposure
3. replace the small remote proxy with a deliberate production deployment if needed
4. optionally add auth later if tailnet sharing ever expands
5. add backups for MySQL data

## Quick Commands Reference

Install and connect:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```

Check status:

```bash
tailscale status
tailscale ip -4
```

Start app:

```bash
bash scripts/start-dev.sh
```

Start phone access:

```bash
npm run start:tailscale
```

Stop app:

```bash
bash scripts/stop-dev.sh
```

Stop phone access:

```bash
npm run stop:tailscale
```

Disconnect Tailscale:

```bash
sudo tailscale down
```

Reconnect Tailscale:

```bash
sudo tailscale up
```

Remove Tailscale:

```bash
sudo apt purge tailscale
sudo apt autoremove
```

## Local-Only App Mode With Tailscale Proxy

This section applies to the current repo setup.

The app itself is still localhost-only:

- frontend stays on `127.0.0.1:5173`
- API stays on `127.0.0.1:4000`
- MySQL stays on `127.0.0.1:3306`

That was kept on purpose to avoid reopening the app to the network.

### What Changed

Instead of rebinding the app to `0.0.0.0`, the repo now includes a separate tailnet-only proxy layer:

- `scripts/tailscale-remote-server.mjs`
- `scripts/start-tailscale-access.sh`
- `scripts/stop-tailscale-access.sh`

This proxy:

- binds only to the machine's Tailscale IP
- serves the built frontend
- forwards `/api/*` to the local API
- does not expose MySQL
- does not change the main app bindings

### Why This Is Safer

- the original app remains localhost-only
- access is limited to devices on your Tailscale tailnet
- there is no public internet exposure through this workflow
- you are not relying on Apache or any other existing service on port 80

### Start Local App

If the app is not already running:

```bash
bash scripts/start-dev.sh
```

### Start Phone Access Over Tailscale

```bash
npm run start:tailscale
```

This command will:

1. verify Tailscale is connected
2. start the local app if needed
3. build the frontend
4. start the Tailscale-bound proxy
5. print the URLs to use from the phone

Example output:

```text
Tailnet IP URL: http://100.x.y.z:5173
MagicDNS URL: http://your-hostname.your-tailnet.ts.net:5173
```

### Open From Phone

With Tailscale enabled on the phone, open either:

```text
http://100.x.y.z:5173
```

or:

```text
http://your-hostname.your-tailnet.ts.net:5173
```

Do not use the direct machine IP without the app port unless you intentionally want whatever is running on port 80.

### Stop Only Tailscale Phone Access

```bash
npm run stop:tailscale
```

This stops only the tailnet proxy.

It does not stop:

- frontend dev server
- API server
- MySQL

### Stop Entire App

```bash
bash scripts/stop-dev.sh
```

or:

```bash
npm run stop:app
```

This stops the local app stack. That also effectively ends phone access because the proxy depends on the local app.

### Clear The Current Tailscale Proxy Session

In normal use, just run:

```bash
npm run stop:tailscale
```

If you need to force-clear the current proxy process:

```bash
pkill -f tailscale-remote-server.mjs
rm -f logs/tailscale-remote.pid
```

Use that only if the normal stop command fails.

### Typical Daily Flow

1. turn on laptop
2. connect Tailscale on laptop
3. connect Tailscale on phone
4. run `bash scripts/start-dev.sh` if app is not running
5. run `npm run start:tailscale`
6. open the printed Tailscale URL on the phone

When done:

1. run `npm run stop:tailscale` if you only want to stop phone access
2. run `bash scripts/stop-dev.sh` if you want to stop the app completely
