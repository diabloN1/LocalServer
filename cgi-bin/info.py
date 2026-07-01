#!/usr/bin/env python3
"""CGI script that returns JSON info - useful for API testing."""
import os
import json
import sys
from datetime import datetime

print("Content-Type: application/json")
print()

data = {
    "server": os.environ.get("SERVER_SOFTWARE", "LocalServer"),
    "gateway": os.environ.get("GATEWAY_INTERFACE", "CGI/1.1"),
    "method": os.environ.get("REQUEST_METHOD", ""),
    "path": os.environ.get("PATH_INFO", ""),
    "query": os.environ.get("QUERY_STRING", ""),
    "host": os.environ.get("HTTP_HOST", ""),
    "timestamp": datetime.utcnow().isoformat() + "Z",
    "python_version": sys.version,
}

print(json.dumps(data, indent=2))
