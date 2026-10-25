#!/usr/bin/env python3
"""Writes the Play Console service account key from ANDROID_PUBLISHER_CREDENTIALS to a file.

The secret holds the key's JSON itself, but pasting it into the GitHub secret form can add a
byte-order mark, surrounding quotes or blank lines, and some setups store it base64-encoded or
as an escaped JSON string. The Play publisher plugin parses the value strictly and fails on the
first character with "MalformedJsonException at line 1 column 1". This accepts all of those
forms, checks that the result is a service account key, and writes it, unchanged in content,
to the path given as the only argument (mode 600), for `PLAY_SERVICE_ACCOUNT_FILE`.

Never prints the secret or any part of it; errors only describe its shape.
"""

import base64
import binascii
import json
import os
import sys

REQUIRED_KEYS = ("type", "client_email", "private_key")


def parse(text):
    try:
        value = json.loads(text)
    except ValueError:
        return None
    # A key stored as an escaped JSON string ("{\"type\": ...}") decodes to a str first.
    if isinstance(value, str):
        return parse(value.strip())
    return value


def decode(raw):
    text = raw.strip().lstrip("﻿").strip()
    if len(text) >= 2 and text[0] == text[-1] and text[0] in "'\"":
        unquoted = parse(text) if text[0] == '"' else None
        if isinstance(unquoted, dict):
            return unquoted
        text = text[1:-1].strip()
    value = parse(text)
    if value is None:
        try:
            decoded = base64.b64decode("".join(text.split()), validate=True).decode("utf-8-sig")
        except (binascii.Error, UnicodeDecodeError, ValueError):
            return None
        value = parse(decoded.strip())
    return value


def describe(raw):
    text = raw.strip()
    first = text[:1]
    kind = "letter" if first.isalpha() else "digit" if first.isdigit() else repr(first) if first in "{[\"'" else "other"
    return f"{len(raw)} characters, first non-blank character is {kind}"


def main():
    if len(sys.argv) != 2:
        sys.exit("usage: play-credentials.py <output.json>")
    raw = os.environ.get("ANDROID_PUBLISHER_CREDENTIALS", "")
    if not raw.strip():
        sys.exit("::error::ANDROID_PUBLISHER_CREDENTIALS is empty.")
    key = decode(raw)
    if not isinstance(key, dict):
        text = raw.strip()
        if text.startswith(("/", "~", "./")) or text.endswith(".json"):
            sys.exit(
                "::error::ANDROID_PUBLISHER_CREDENTIALS looks like a file path. The secret must hold "
                "the key itself: paste the contents of the downloaded .json file."
            )
        sys.exit(
            "::error::ANDROID_PUBLISHER_CREDENTIALS is neither JSON nor base64-encoded JSON "
            f"({describe(raw)}). Paste the whole service account key file into the secret."
        )
    missing = [name for name in REQUIRED_KEYS if not key.get(name)]
    if missing or key.get("type") != "service_account":
        sys.exit(
            "::error::ANDROID_PUBLISHER_CREDENTIALS is JSON but not a service account key "
            f"(type={key.get('type')!r}, missing: {', '.join(missing) or 'none'})."
        )
    path = sys.argv[1]
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as out:
        json.dump(key, out)
    print(f"Service account key for {key['client_email'].split('@')[-1]} written.")


if __name__ == "__main__":
    main()
