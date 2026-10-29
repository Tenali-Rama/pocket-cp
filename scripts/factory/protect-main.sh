#!/usr/bin/env bash
# Owner runs once (token needs Administration: write): main accepts changes only through PRs with green CI.
# strict=false on purpose: "branch must be up to date" caused endless rebase rounds in polykov.
set -euo pipefail
source "$(git rev-parse --show-toplevel)/scripts/factory/owner.env"
gh api -X PUT "repos/$GH_REPO/branches/main/protection" --input - >/dev/null <<'JSON'
{
  "required_status_checks": { "strict": false, "contexts": ["verify", "policy", "ui (phone)", "ui (tablet)"] },
  "enforce_admins": true,
  "required_pull_request_reviews": null,
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
echo "main is protected: PR + green CI (verify, policy, ui) required."
