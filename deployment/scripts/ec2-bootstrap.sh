#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/stockpro}"
APP_USER="${APP_USER:-ubuntu}"

sudo apt-get update
sudo apt-get install -y ca-certificates curl git nginx ufw

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
fi

sudo usermod -aG docker "$APP_USER"
sudo mkdir -p "$APP_DIR"
sudo chown -R "$APP_USER":"$APP_USER" "$APP_DIR"

sudo mkdir -p /var/www/certbot
sudo cp deployment/nginx/stockpro.conf /etc/nginx/sites-available/stockpro.conf
sudo ln -sf /etc/nginx/sites-available/stockpro.conf /etc/nginx/sites-enabled/stockpro.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl restart nginx

sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "Bootstrap complete."
echo "Next steps:"
echo "1. Clone the repository into $APP_DIR"
echo "2. Copy .env.example to .env and set real secrets"
echo "3. Run deployment/scripts/deploy.sh"
echo "4. Add TLS with certbot when your domain points to this server"
