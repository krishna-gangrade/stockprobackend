# EC2 Deployment Guide

## Target layout

- Ubuntu EC2 host
- Docker Engine on the host
- Docker Compose plugin on the host
- NGINX on the host for public `80/443`
- StockPro containers behind NGINX

## Recommended security group

- Allow `22/tcp` only from your admin IP
- Allow `80/tcp` from the internet
- Allow `443/tcp` from the internet
- Do not expose MySQL, Redis, Kafka, Eureka, or Admin Server publicly

## Server bootstrap

From the repository root on the EC2 machine:

```bash
bash deployment/scripts/ec2-bootstrap.sh
```

## First deployment

1. Clone the repository to `/opt/stockpro`
2. Copy `.env.example` to `.env`
3. Replace all placeholder secrets
4. Start the stack:

```bash
docker compose up -d --build
```

## NGINX and HTTPS

- Public NGINX config: `deployment/nginx/stockpro.conf`
- Place your real domain in `server_name`
- For HTTPS, use certbot after DNS points to the EC2 public IP
- Keep Docker services bound to `127.0.0.1` where possible

## GitHub Actions secrets

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`
- `EC2_PORT`
- `EC2_APP_DIR`

## Update flow

1. Push to `main`, `frontend`, or `backend`
2. GitHub Actions runs backend tests, frontend tests, and Docker builds
3. The deploy job SSHs into EC2
4. `deployment/scripts/deploy.sh` pulls the latest branch and restarts containers with `docker compose up -d --build --remove-orphans`

## Google OAuth production checklist

- Add your production domain to Google Authorized JavaScript origins
- Add your production callback URL to Google Authorized redirect URIs
- Keep the frontend served over HTTPS
- Keep `GOOGLE_CLIENT_SECRET` only in `.env` or GitHub Secrets
