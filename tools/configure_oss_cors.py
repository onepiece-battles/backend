#!/usr/bin/env python3
"""Allow the app to read signed OSS image URLs, preserving existing rules.

Uses ALIYUN_OSS_ACCESS_KEY_ID / ALIYUN_OSS_ACCESS_KEY_SECRET from the environment.
Only CORS configuration changes; bucket/object ACLs are never modified.
"""
import argparse
import base64
import email.utils
import hashlib
import hmac
import os
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

BUCKET = "onepiece-battles"
ENDPOINT = f"https://{BUCKET}.oss-cn-shanghai.aliyuncs.com/"
APP_ORIGINS = ["https://onepiece-battles.com", "https://www.onepiece-battles.com"]


def request(credentials, method, body=None):
    date = email.utils.formatdate(usegmt=True)
    md5 = base64.b64encode(hashlib.md5(body).digest()).decode() if body else ""
    content_type = "application/xml" if body else ""
    canonical = f"{method}\n{md5}\n{content_type}\n{date}\n/{BUCKET}/?cors"
    signature = base64.b64encode(hmac.new(
        credentials["ALIYUN_OSS_ACCESS_KEY_SECRET"].encode(),
        canonical.encode(), hashlib.sha1).digest()).decode()
    headers = {"Date": date, "Authorization":
        f"OSS {credentials['ALIYUN_OSS_ACCESS_KEY_ID']}:{signature}"}
    if body:
        headers.update({"Content-MD5": md5, "Content-Type": content_type})
    return urllib.request.urlopen(urllib.request.Request(
        ENDPOINT + "?cors", method=method, data=body, headers=headers), timeout=30)


def configure(credentials, origins):
    try:
        with request(credentials, "GET") as response:
            root = ET.fromstring(response.read())
    except urllib.error.HTTPError as error:
        code = ET.fromstring(error.read()).findtext("Code")
        if error.code != 404 or code != "NoSuchCORSConfiguration":
            raise RuntimeError(f"Cannot read bucket CORS: HTTP {error.code}, {code}") from None
        root = ET.Element("CORSConfiguration")
    missing = []
    for origin in origins:
        if not any(
            (origin in [e.text for e in rule.findall("AllowedOrigin")] or
             "*" in [e.text for e in rule.findall("AllowedOrigin")]) and
            {"GET", "HEAD"}.issubset({e.text for e in rule.findall("AllowedMethod")})
            for rule in root.findall("CORSRule")
        ):
            missing.append(origin)
    if missing:
        rule = ET.SubElement(root, "CORSRule")
        for origin in missing:
            ET.SubElement(rule, "AllowedOrigin").text = origin
        for method in ["GET", "HEAD"]:
            ET.SubElement(rule, "AllowedMethod").text = method
        ET.SubElement(rule, "ExposeHeader").text = "ETag"
        ET.SubElement(rule, "MaxAgeSeconds").text = "3600"
    vary = root.find("ResponseVary")
    update_vary = vary is None or vary.text != "true"
    if vary is None:
        vary = ET.SubElement(root, "ResponseVary")
    vary.text = "true"
    if missing or update_vary:
        with request(credentials, "PUT", ET.tostring(root, encoding="utf-8")):
            pass
    print("OSS CORS configured for GET/HEAD:", ", ".join(origins))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--origin", action="append", help="Additional development origin")
    args = parser.parse_args()
    configure(os.environ, APP_ORIGINS + (args.origin or []))
