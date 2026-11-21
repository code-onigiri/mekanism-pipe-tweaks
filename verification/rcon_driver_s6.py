#!/usr/bin/env python3
"""RCON driver for the mixed-side guide capture (REPORT section 6).

Reuses the RCON client from rcon_driver.py. Watches the dev client log for
`MPT_AUTOTEST` markers, prepares the flat test area, teleports the dev player and
records the server-side block data at the placed pipe for cross-checking.
"""
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from rcon_driver import Rcon, RconError  # noqa: E402

HOST = "127.0.0.1"
PORT = 25575
PLAYER = "Dev"
CLIENT_PIPE = (14, 4, 15)
ANCHOR = (14, 4, 14)


def connect(retries=180, delay=1.0):
    last = None
    for _ in range(retries):
        try:
            return Rcon(host=HOST, port=PORT)
        except (OSError, RconError) as exc:
            last = exc
            time.sleep(delay)
    raise SystemExit("could not connect to rcon: %s" % last)


def main():
    log_path = sys.argv[1]
    driver_log_path = sys.argv[2]
    driver_log = open(driver_log_path, "a", buffering=1)

    def log(message):
        driver_log.write("[%s] %s\n" % (time.strftime("%H:%M:%S"), message))

    rcon = connect()

    def run(command):
        try:
            response = rcon.command(command)
        except Exception as exc:  # noqa: BLE001 - keep driving
            log("FAILED %r: %s" % (command, exc))
            return ""
        log("%s -> %s" % (command, response.replace("\n", " | ")))
        return response

    def pipe_data(label):
        out = run("execute if block %d %d %d mekanism:basic_mechanical_pipe run data get block %d %d %d"
                  % (CLIENT_PIPE + CLIENT_PIPE))
        if "has the following block data" in out:
            log("  SERVER %s C%s: %s" % (label, CLIENT_PIPE, out.split("block data:")[-1].strip()[:160]))
        else:
            log("  SERVER %s C%s: no pipe" % (label, CLIENT_PIPE))

    def setup_world():
        commands = [
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "gamerule sendCommandFeedback false",
            "difficulty peaceful",
            "time set noon",
            "weather clear",
            "fill 8 3 8 18 8 24 minecraft:air",
            "fill 8 2 8 18 2 24 minecraft:stone",
            "setblock %d %d %d minecraft:air" % CLIENT_PIPE,
            "setblock %d %d %d mekanism:basic_mechanical_pipe" % ANCHOR,
        ]
        for command in commands:
            run(command)
        pipe_data("setup")

    player_ready = False
    marker_re = re.compile(r"MPT_AUTOTEST (.*)$")
    tp_re = re.compile(r"TP (\S+) (\S+) (\S+) (\S+)")
    offset = 0
    buffer = ""
    handled = set()
    deadline = time.time() + 1800
    log("watching %s" % log_path)

    while time.time() < deadline:
        if os.path.exists(log_path):
            if os.path.getsize(log_path) < offset:
                offset = 0
                buffer = ""
            with open(log_path, "rb") as handle:
                handle.seek(offset)
                chunk = handle.read()
            offset += len(chunk)
            if chunk:
                buffer += chunk.decode("utf-8", "replace")
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    match = marker_re.search(line)
                    if not match or match.group(1) in handled:
                        continue
                    marker = match.group(1)
                    handled.add(marker)
                    log("marker: %s" % marker)
                    if marker.startswith("world_ready") and not player_ready:
                        player_ready = True
                        setup_world()
                        for command in [
                            "clear %s" % PLAYER,
                            "gamemode creative %s" % PLAYER,
                            "op %s" % PLAYER,
                            "give %s mekanism:configurator" % PLAYER,
                            "give %s mekanism:basic_mechanical_pipe 64" % PLAYER,
                        ]:
                            run(command)
                    tp_match = tp_re.match(marker)
                    if tp_match:
                        label, x, y, z = tp_match.groups()
                        run("tp %s %s %s %s" % (PLAYER, x, y, z))
                        log("teleported for %s" % label)
                    if marker.startswith("raw C-after"):
                        pipe_data(marker.split(":", 1)[0])
                    if marker.startswith("finished"):
                        pipe_data("final")
                        log("client finished; exiting driver")
                        return
        time.sleep(0.1)
    log("driver timed out")


if __name__ == "__main__":
    main()
