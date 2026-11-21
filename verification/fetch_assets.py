#!/usr/bin/env python3
"""Populate the NeoForm Runtime asset cache over HTTPS.

The bundled downloader uses a plain-HTTP base URL which Mojang's CDN now
rejects with AccountRequiresHttps, so the download hangs. Fetching the exact
objects/sizes the index asks for fixes it, and nfrt then sees a complete cache.
"""
import concurrent.futures
import json
import os
import sys
import urllib.request

INDEX = "/home/hatch/workspace/gradle-test/ghrun/caches/neoformruntime/assets/indexes/17.json"
ROOT = "/home/hatch/workspace/gradle-test/ghrun/caches/neoformruntime/assets/objects"
BASE = "https://resources.download.minecraft.net/"


def fetch(job):
    digest, size = job
    path = os.path.join(ROOT, digest[:2], digest)
    if os.path.exists(path) and os.path.getsize(path) == size:
        return "skip"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for attempt in range(5):
        try:
            with urllib.request.urlopen(BASE + digest[:2] + "/" + digest, timeout=60) as resp:
                data = resp.read()
            if len(data) != size:
                raise IOError("size mismatch %d != %d" % (len(data), size))
            tmp = path + ".tmp"
            with open(tmp, "wb") as handle:
                handle.write(data)
            os.replace(tmp, path)
            return "ok"
        except Exception as exc:  # noqa: BLE001
            last = exc
    return "fail %s: %s" % (digest, last)


def main():
    objects = json.load(open(INDEX))["objects"]
    jobs = [(v["hash"], v["size"]) for v in objects.values()]
    print("objects:", len(jobs))
    done = 0
    failed = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
        for result in pool.map(fetch, jobs):
            done += 1
            if result.startswith("fail"):
                failed += 1
                print(result)
            if done % 500 == 0:
                print("progress", done)
    print("done", done, "failed", failed)


if __name__ == "__main__":
    main()
