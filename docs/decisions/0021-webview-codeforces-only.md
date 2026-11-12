# 0021 — The WebView is restricted to Codeforces over HTTPS

- **Status**: accepted
- **Context**: Spec §16 restricts the WebView to Codeforces domains. The statement fallback (047) needs it now; the
  M4 login and fallback browser will reuse it, and must not each invent their own rule.
- **Decision**: One WebView, `core/webview`'s `CodeforcesWebView`. It loads or follows a URL only when
  `CodeforcesUrlPolicy.isAllowed`: scheme `https` and host `codeforces.com` or `*.codeforces.com`. A disallowed
  navigation of the page is cancelled and handed to the caller (the problem view opens `http(s)` ones in the system
  browser); in a frame it is only cancelled. JavaScript on for MathJax; file and content access off.
- **Consequences**: Subresources (scripts, images) are not filtered, only navigations. Login (M4) must add its
  extra hosts, if any, here and in `CodeforcesUrlPolicyTest`, never in a feature.
