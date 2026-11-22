#!/usr/bin/env python3
"""RCON driver for the 0.2.0 spec-change verification, session 9.

Fixes the session-8 harness bug: the driver used to start reading from offset 0 of an existing
`latest.log`, so it replayed markers from a previous session, ran `/give` before the player had
joined, and then deduplicated the new session's `world_ready`. Here the caller truncates the log
before the client starts, the driver additionally detects rotation (inode/size), and it re-issues
the hotbar setup whenever the harness reports `setup_wait` (missing slots).

On the harness markers it teleports the dev player and reads the connection NBT of every relevant
transmitter from the server side.
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

A_ANCHOR = (10, 4, 10)
A_PLACED = (10, 4, 11)
B_ANCHOR = (14, 4, 20)
B_PLACED = (14, 4, 21)
C_MACHINE = (14, 4, 30)
C_PLACED = (14, 4, 31)
D_PIPE = (14, 4, 40)

CABLE = "mekanism:basic_universal_cable"
MACHINE_PIPE = "mekanism:basic_mechanical_pipe"
CUBE = "mekanism:basic_fluid_tank"


def connect(retries=600, delay=0.5):
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

    def give_setup(reason):
        log("issuing hotbar setup (%s)" % reason)
        for command in [
            "clear %s" % PLAYER,
            "gamemode creative %s" % PLAYER,
            "give %s %s 64" % (PLAYER, CABLE),
            "give %s %s 64" % (PLAYER, MACHINE_PIPE),
            "give %s mekanism:configurator" % PLAYER,
        ]:
            run(command)

    def report_block(label, pos, block_id):
        x, y, z = pos
        out = run("execute if block %d %d %d %s run data get block %d %d %d"
                  % (x, y, z, block_id, x, y, z))
        if "has the following block data" in out:
            log("  SERVER %s %s at %s: %s" % (label, block_id, pos,
                                              out.split("block data:")[-1].strip()[:220]))
        else:
            log("  SERVER %s at %s: not present as %s" % (label, pos, block_id))

    def setup_world():
        commands = [
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "gamerule sendCommandFeedback false",
            "difficulty peaceful",
            "time set noon",
            "weather clear",
            "fill 4 3 4 24 8 56 minecraft:air",
            "fill 4 2 4 24 2 56 minecraft:stone",
            "setblock %d %d %d %s" % (A_ANCHOR + (CABLE,)),
            "setblock %d %d %d minecraft:stone" % (14, 3, 20),
            "setblock %d %d %d %s" % (C_MACHINE + (CUBE,)),
            "setblock %d %d %d %s" % (D_PIPE + (CABLE,)),
            "op %s" % PLAYER,
        ]
        for command in commands:
            run(command)
        give_setup("world_ready")
        report_block("A-anchor", A_ANCHOR, CABLE)
        report_block("C-machine", C_MACHINE, CUBE)
        report_block("D-pipe", D_PIPE, CABLE)

    dedup = set()
    marker_re = re.compile(r"MPT_AUTOTEST (.*)$")
    tp_re = re.compile(r"TP (\S+) (\S+) (\S+) (\S+)")
    offset = 0
    buffer = ""
    inode = None
    deadline = time.time() + 2400
    log("watching %s (from offset 0)" % log_path)

    while time.time() < deadline:
        if os.path.exists(log_path):
            stat = os.stat(log_path)
            if inode is None:
                inode = stat.st_ino
            rotated = stat.st_ino != inode or stat.st_size < offset
            if rotated:
                log("log rotated/truncated; restarting from 0")
                inode = stat.st_ino
                offset = 0
                buffer = ""
                dedup.clear()
            with open(log_path, "rb") as handle:
                handle.seek(offset)
                chunk = handle.read()
            offset += len(chunk)
            if chunk:
                buffer += chunk.decode("utf-8", "replace")
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    match = marker_re.search(line)
                    if not match:
                        continue
                    marker = match.group(1)
                    if marker in dedup:
                        continue
                    dedup.add(marker)
                    log("marker: %s" % marker)

                    if marker.startswith("world_ready"):
                        setup_world()
                    elif marker.startswith("setup_wait"):
                        give_setup("harness reports missing slots: " + marker)
                    elif marker.startswith("setup_done"):
                        give_setup("verify hotbar after setup_done")

                    tp_match = tp_re.match(marker)
                    if tp_match:
                        label, x, y, z = tp_match.groups()
                        run("tp %s %s %s %s" % (PLAYER, x, y, z))
                        log("teleported for %s" % label)

                    if marker.startswith("A_RESULT"):
                        report_block("A-placed", A_PLACED, CABLE)
                        report_block("A-anchor", A_ANCHOR, CABLE)
                    elif marker.startswith("B1_ANCHOR_RAW"):
                        report_block("B-anchor", B_ANCHOR, CABLE)
                    elif marker.startswith("B2_PLACED_RAW"):
                        report_block("B-placed", B_PLACED, CABLE)
                        report_block("B-anchor", B_ANCHOR, CABLE)
                    elif marker.startswith("C_PLACED_RAW"):
                        report_block("C-placed", C_PLACED, MACHINE_PIPE)
                        report_block("C-machine", C_MACHINE, CUBE)
                    elif marker.startswith("E after cycle") or marker.startswith("E_GUIDE_RESULT"):
                        report_block("B-placed", B_PLACED, CABLE)
                    elif marker.startswith("finished"):
                        report_block("final-A-placed", A_PLACED, CABLE)
                        report_block("final-B-placed", B_PLACED, CABLE)
                        report_block("final-B-anchor", B_ANCHOR, CABLE)
                        report_block("final-C-placed", C_PLACED, MACHINE_PIPE)
                        report_block("final-C-machine", C_MACHINE, CUBE)
                        log("client finished; exiting driver")
                        return
        time.sleep(0.1)
    log("driver timed out")


if __name__ == "__main__":
    main()
