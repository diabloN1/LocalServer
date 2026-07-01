#!/usr/bin/env python3
"""
Sample CGI script for LocalServer.
Outputs HTML with request environment info.
"""
import os
import sys
import json
from datetime import datetime

# CGI output: headers first, then blank line, then body
print("Content-Type: text/html; charset=utf-8")
print()  # blank line separates headers from body

method = os.environ.get("REQUEST_METHOD", "GET")
query_string = os.environ.get("QUERY_STRING", "")
path_info = os.environ.get("PATH_INFO", "")
content_length = int(os.environ.get("CONTENT_LENGTH", "0") or "0")

# Read POST body if present
body = ""
if method == "POST" and content_length > 0:
    body = sys.stdin.read(content_length)

# Parse query parameters
params = {}
if query_string:
    for pair in query_string.split("&"):
        if "=" in pair:
            k, v = pair.split("=", 1)
            params[k] = v

# Build environment table
env_vars = [
    ("REQUEST_METHOD", method),
    ("PATH_INFO", path_info),
    ("QUERY_STRING", query_string),
    ("CONTENT_LENGTH", str(content_length)),
    ("SERVER_SOFTWARE", os.environ.get("SERVER_SOFTWARE", "")),
    ("GATEWAY_INTERFACE", os.environ.get("GATEWAY_INTERFACE", "")),
    ("HTTP_HOST", os.environ.get("HTTP_HOST", "")),
    ("HTTP_USER_AGENT", os.environ.get("HTTP_USER_AGENT", "")),
]

env_rows = "\n".join(
    f'<tr><td style="color:#94a3b8;padding:.3rem .8rem">{k}</td>'
    f'<td style="color:#e2e8f0;padding:.3rem .8rem">{v or "<em style=color:#475569>not set</em>"}</td></tr>'
    for k, v in env_vars
)

params_html = ""
if params:
    params_html = "<h3 style='margin-top:1.5rem'>Query Parameters</h3><ul>" + \
                  "".join(f"<li><b>{k}</b> = {v}</li>" for k, v in params.items()) + "</ul>"

body_html = ""
if body:
    body_html = f"<h3 style='margin-top:1.5rem'>Request Body</h3><pre style='background:#0f172a;padding:1rem;border-radius:.5rem'>{body[:500]}</pre>"

print(f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>CGI Hello - LocalServer</title>
  <style>
    body {{ font-family: monospace; background: #0f172a; color: #e2e8f0;
            padding: 2rem; max-width: 800px; margin: 0 auto; }}
    h1 {{ color: #3b82f6; margin-bottom: 0.5rem; }}
    h2 {{ color: #60a5fa; font-size: 1rem; margin: 1.5rem 0 0.5rem; }}
    table {{ border-collapse: collapse; width: 100%; margin-top: 0.5rem; }}
    tr:nth-child(even) {{ background: #1e293b; }}
    .tag {{ background: #1d4ed8; color: white; font-size: 0.7rem;
            padding: 0.15rem 0.4rem; border-radius: 9999px; margin-left: 0.5rem; }}
    a {{ color: #60a5fa; }}
  </style>
</head>
<body>
  <h1>🐍 CGI Script Response <span class="tag">Python</span></h1>
  <p style="color:#94a3b8">Executed at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}</p>

  <h2>CGI Environment Variables</h2>
  <table>{env_rows}</table>

  {params_html}
  {body_html}

  <p style="margin-top:2rem"><a href="/">← Back to Home</a></p>
</body>
</html>""")
