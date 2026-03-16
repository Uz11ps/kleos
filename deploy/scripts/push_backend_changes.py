import pathlib
import sys

import paramiko


HOST = "5.187.5.24"
PORT = 22
USER = "root"
PASSWORD = "BY5da3muNBALiumT"

ROOT = pathlib.Path(r"C:\Users\1\AndroidStudioProjects\Kleos")

FILES = [
    ("server/src/routes/adminWeb.ts", "/opt/kleos/kleos/server/src/routes/adminWeb.ts"),
    ("server/src/routes/news.ts", "/opt/kleos/kleos/server/src/routes/news.ts"),
    ("server/src/routes/partners.ts", "/opt/kleos/kleos/server/src/routes/partners.ts"),
    ("server/src/routes/programs.ts", "/opt/kleos/kleos/server/src/routes/programs.ts"),
    ("server/src/models/News.ts", "/opt/kleos/kleos/server/src/models/News.ts"),
    ("server/src/models/Partner.ts", "/opt/kleos/kleos/server/src/models/Partner.ts"),
    ("server/src/models/Program.ts", "/opt/kleos/kleos/server/src/models/Program.ts"),
    ("server/src/models/University.ts", "/opt/kleos/kleos/server/src/models/University.ts"),
]

REMOTE_CMD = (
    "set -e; "
    "cd /opt/kleos/kleos; "
    "docker compose build api; "
    "docker compose up -d api nginx; "
    "docker compose ps; "
    "echo ---HEALTH---; "
    "curl -sS https://api.kleos-study.ru/health; echo; "
    "echo ---NEWS---; "
    "curl -sS https://api.kleos-study.ru/api/news | head -c 500; echo; "
    "echo ---PARTNERS---; "
    "curl -sS https://api.kleos-study.ru/api/partners | head -c 500; echo"
)


def main() -> int:
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, port=PORT, username=USER, password=PASSWORD, timeout=20, banner_timeout=20, auth_timeout=20)

    try:
        with ssh.open_sftp() as sftp:
            for local_rel, remote_path in FILES:
                local_path = ROOT / local_rel
                print(f"Uploading {local_rel} -> {remote_path}")
                sftp.put(str(local_path), remote_path)

        print("Running remote deploy command...")
        stdin, stdout, stderr = ssh.exec_command(REMOTE_CMD, timeout=1200)
        out = stdout.read().decode("utf-8", errors="replace")
        err = stderr.read().decode("utf-8", errors="replace")
        code = stdout.channel.recv_exit_status()

        if out:
            print(out)
        if err:
            print("---REMOTE STDERR---")
            print(err)

        print(f"Remote exit code: {code}")
        return code
    finally:
        ssh.close()


if __name__ == "__main__":
    sys.exit(main())
