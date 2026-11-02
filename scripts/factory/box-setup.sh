#!/usr/bin/env bash
# Sets up this folder's Omnirush sandbox box: tools, git identity, hooks and GitHub credentials. Idempotent.
# Run it inside the box (from an Omnirush session). The token itself is installed by box-token.sh.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
source scripts/factory/owner.env
KTLINT_VERSION=1.3.1
GITLEAKS_VERSION=8.30.1
main_checkout=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
token_file="$main_checkout/.factory/gh_token"

export DEBIAN_FRONTEND=noninteractive
if ! command -v java >/dev/null || ! [ -x /usr/bin/gh ] || ! command -v jq >/dev/null || ! command -v curl >/dev/null; then
  apt-get update -qq
  apt-get install -y -qq gh jq curl openjdk-17-jre-headless >/dev/null
fi
if ! command -v ktlint >/dev/null; then
  curl -fsSL -o /usr/local/bin/ktlint \
    "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint"
  chmod +x /usr/local/bin/ktlint
fi
if ! command -v gitleaks >/dev/null; then
  curl -fsSL "https://github.com/gitleaks/gitleaks/releases/download/v$GITLEAKS_VERSION/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
    | tar -xz -C /usr/local/bin gitleaks
fi

# gh reads the token file on every call, so replacing the token needs no other step.
cat >/usr/local/bin/gh <<WRAPPER
#!/usr/bin/env bash
GH_TOKEN=\$(cat "$token_file") GH_REPO="$GH_REPO" exec /usr/bin/gh "\$@"
WRAPPER
chmod +x /usr/local/bin/gh

git config user.name "$OWNER_NAME"
git config user.email "$OWNER_EMAIL"
git config core.hooksPath scripts/hooks
git config --replace-all credential.https://github.com.helper ""
git config --add credential.https://github.com.helper \
  "!f() { echo username=x-access-token; echo \"password=\$(cat '$token_file')\"; }; f"
mkdir -p "$main_checkout/.factory/worktrees"
chmod +x scripts/*.sh scripts/factory/*.sh scripts/hooks/*
echo "box setup done."
[ -s "$token_file" ] || echo "No GitHub token yet: the owner runs scripts/factory/box-token.sh <file> outside the sandbox."
