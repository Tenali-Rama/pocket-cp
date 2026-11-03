#!/usr/bin/env bash
# Sets up this folder's Omnirush sandbox box: tools, git identity, hooks and GitHub credentials. Idempotent.
# Run it inside the box (from an Omnirush session). The token itself is installed by box-token.sh.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
source scripts/factory/owner.env
KTLINT_VERSION=1.3.1
GITLEAKS_VERSION=8.30.1
GH_VERSION=2.103.3
main_checkout=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
token_file="$main_checkout/.factory/gh_token"

export DEBIAN_FRONTEND=noninteractive
if ! command -v java >/dev/null || ! command -v jq >/dev/null || ! command -v curl >/dev/null || ! command -v python3 >/dev/null; then
  apt-get update -qq
fi
if ! command -v java >/dev/null || ! command -v jq >/dev/null || ! command -v curl >/dev/null; then
  apt-get install -y -qq jq curl openjdk-17-jre-headless >/dev/null
fi
if ! command -v python3 >/dev/null; then
  apt-get install -y -qq python3 >/dev/null
fi
# The box's network drops connections now and then.
fetch() { curl -fsSL --retry 5 --retry-all-errors --retry-delay 5 "$@"; }
# The box's own gh package is too old for the factory scripts.
if ! [ -x /opt/gh/bin/gh ] || ! /opt/gh/bin/gh --version | grep -q "version $GH_VERSION "; then
  mkdir -p /opt/gh
  fetch "https://github.com/cli/cli/releases/download/v$GH_VERSION/gh_${GH_VERSION}_linux_amd64.tar.gz" \
    | tar -xz --no-same-owner --strip-components=1 -C /opt/gh
fi
if ! command -v ktlint >/dev/null; then
  fetch -o /usr/local/bin/ktlint \
    "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint"
  chmod +x /usr/local/bin/ktlint
fi
if ! command -v gitleaks >/dev/null; then
  fetch "https://github.com/gitleaks/gitleaks/releases/download/v$GITLEAKS_VERSION/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
    | tar -xz --no-same-owner -C /usr/local/bin gitleaks
fi

# gh reads the token file on every call, so replacing the token needs no other step.
cat >/usr/local/bin/gh <<WRAPPER
#!/usr/bin/env bash
GH_TOKEN=\$(cat "$token_file") GH_REPO="$GH_REPO" exec /opt/gh/bin/gh "\$@"
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
