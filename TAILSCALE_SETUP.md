# Tailscale Setup And Usage

This app is currently configured for local-only access.

It is not suitable for public deployment in its current form because the backend still uses a fixed demo user and does not require login.

## What Tailscale Is Used For

Tailscale is not enough to expose this app in the current setup because the services are bound to `127.0.0.1` only.

Typical usage flow:

1. Start the laptop.
2. Make sure Tailscale is connected.
3. Start the app.
4. Open the app from the same machine unless you intentionally add a separate remote-access layer.

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

## Start The Finance App

```bash
bash scripts/start-dev.sh
```

This starts:

- MySQL
- API
- frontend

## Access Scope

Use the app locally:

```text
http://127.0.0.1:5173
```

Test the API locally:

```text
http://127.0.0.1:4000/api/health
```

## Daily Steps To Use The App Again

Each time the laptop was shut down:

1. Turn on the laptop.
2. Confirm Tailscale is connected.
3. Start the app with `bash scripts/start-dev.sh`.
4. Open `http://127.0.0.1:5173` on the same machine.

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

- This is safer than exposing the app on your local network.
- Traffic stays on the local machine in the current setup.
- This is not a substitute for app-level authentication.
- Anyone with local machine access can use the app.

### Required Account Hygiene

- Use MFA on the account you use for Tailscale.
- Keep phone and laptop screen lock enabled.
- Do not share your tailnet unless you intend to share access to the app.

### Availability

- The laptop must be powered on.
- The app must be running.

### Current App Constraints

- This project is still running in a development-style setup.
- The frontend is served from the Vite dev server on port `5173`.
- The API runs on port `4000`.
- There is no user login yet.

### Better Long-Term Setup

If this becomes a tool you use regularly, the next improvements should be:

1. add authentication
2. add a deliberate remote-access design instead of relying on dev-server exposure
3. run the app in a production setup instead of the dev server
4. optionally serve frontend and API behind one port
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

Stop app:

```bash
bash scripts/stop-dev.sh
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
