# Server Migration Runbook (Docker)

This runbook migrates backend runtime from an unstable VPS to a clean server with Docker Compose and Nginx.

## 1) Scope and assumptions

- Backend code is from this repository.
- Database is external (`MongoDB Atlas`), so DB dump/restore is not required.
- Runtime data to migrate:
  - `deploy/.env`
  - uploaded files from `server/uploads` (or old runtime upload directory).
- Domain for API: `api.kleos-study.ru`.

## 2) Pre-migration checklist

1. Keep old VPS online until migration is complete.
2. Lower DNS TTL for `api.kleos-study.ru` to `300` seconds (at least 1 hour before cutover).
3. Ensure you have:
   - new VPS root access,
   - current production `.env`,
   - backup of uploads.
4. Add the new VPS public IP to Atlas Network Access allowlist.

## 3) New VPS bootstrap checklist

Run on new VPS (Ubuntu 22.04/24.04):

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release ufw fail2ban
```

Install Docker Engine + Compose plugin:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

Firewall baseline:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Verify:

```bash
docker --version
docker compose version
sudo ufw status
```

## 4) Deploy repository on new VPS

```bash
mkdir -p /opt/kleos
cd /opt/kleos
git clone <your_repo_url> kleos
cd /opt/kleos/kleos
```

Prepare environment:

```bash
cp deploy/.env.example deploy/.env
nano deploy/.env
```

Mandatory keys in `deploy/.env`:

- `MONGODB_URI`
- `JWT_SECRET`
- `CORS_ORIGIN`
- `PORT=8080`

## 5) Uploads migration

On old server, archive uploads:

```bash
cd /path/to/current/runtime
tar -czf uploads_backup.tar.gz -C server/uploads .
```

Copy archive to new server into `/opt/kleos/kleos/backups`.

On new server:

```bash
cd /opt/kleos/kleos
mkdir -p backups
mkdir -p data/uploads
tar -xzf backups/uploads_backup.tar.gz -C data/uploads
```

The compose file uses persistent volume `uploads_data`. To seed it once:

```bash
docker volume create kleos_uploads_data
docker run --rm \
  -v kleos_uploads_data:/dest \
  -v /opt/kleos/kleos/data/uploads:/src \
  alpine sh -c "cp -R /src/. /dest/"
```

## 6) Build and start stack

```bash
cd /opt/kleos/kleos
docker compose build --no-cache
docker compose up -d
docker compose ps
```

Smoke checks:

```bash
curl -i http://localhost/health
curl -i http://localhost/api/news
curl -i http://localhost/api/gallery
curl -i http://localhost/api/universities
```

Logs:

```bash
docker compose logs -f api
docker compose logs -f nginx
```

## 7) TLS and DNS cutover

1. Configure TLS certificates (Let's Encrypt or your existing cert chain).
2. Update Nginx config if needed for `443` listener.
3. Point `api.kleos-study.ru` A-record to new VPS IP.
4. Wait for DNS propagation (TTL 300 should be quick).

Post-cutover checks:

```bash
curl -i https://api.kleos-study.ru/health
curl -i https://api.kleos-study.ru/api/news
```

Application checks:

- Android app opens home/news/gallery without network errors.
- iOS app opens home/news/gallery without network errors.
- Admin panel loads and creates/edits content.
- Uploads are accessible via `/uploads/...`.

## 8) Monitoring first 60 minutes

Run during first hour:

```bash
docker stats --no-stream
docker compose ps
docker compose logs --since=10m api
```

Targets:

- API health stable (no restarts).
- RAM usage below 80% sustained.
- No repeated 5xx spikes.

## 9) Rollback plan

If severe errors occur after cutover:

1. Repoint DNS `api.kleos-study.ru` to old VPS IP.
2. Confirm old API health.
3. Keep new VPS running for diagnostics, do not destroy immediately.

Rollback verification:

```bash
curl -i https://api.kleos-study.ru/health
```

## 10) Hardening and operations

- Keep `NODE_ENV=production`.
- Keep memory cap in compose (`mem_limit`) and Node heap cap (`NODE_OPTIONS`).
- Rotate logs (already set in `docker-compose.yml`).
- Keep old VPS for at least 48-72 hours after successful cutover.
- Rotate all server passwords after migration.
