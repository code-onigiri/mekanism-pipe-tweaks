#!/usr/bin/env python3
"""Tiny stdlib RCON driver for the Mekanism Pipe Tweaks in-game verification.

Phase 1 (immediately): prepare the flat-world test area.
Phase 2 (on the client's "world_ready" marker): creative mode + items.
Phase 3 (on every "MPT_AUTOTEST TP ..." marker): teleport the dev player.

Markers are read from the running client's log file, so no mod-side network
trickery is needed.
"""
import os
import re
import socket
import struct
import sys
import time

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = os.environ.get("MPT_RCON_PASSWORD", "changeme")
PLAYER = "Dev"


class RconError(RuntimeError):
    pass


class Rcon:
    def __init__(self, host=HOST, port=PORT, password=PASSWORD, timeout=10.0):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.sock.settimeout(timeout)
        self.req_id = 1
        self._send(3, password)
        rid, _typ, _body = self._recv()
        if rid == -1:
            raise RconError("rcon authentication failed")
        self.req_id = 1

    def _send(self, typ, body):
        payload = struct.pack("<ii", self.req_id, typ) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        rid = self.req_id
        self.req_id += 1
        return rid

    def _recv_exact(self, count):
        data = b""
        while len(data) < count:
            chunk = self.sock.recv(count - len(data))
            if not chunk:
                raise RconError("connection closed")
            data += chunk
        return data

    def _recv(self):
        length = struct.unpack("<i", self._recv_exact(4))[0]
        data = self._recv_exact(length)
        rid, typ = struct.unpack("<ii", data[:8])
        body = data[8:-2].decode("utf-8", "replace")
        return rid, typ, body

    def command(self, command):
        rid = self._send(2, command)
        collected = ""
        deadline = time.time() + 15
        while time.time() < deadline:
            resp_id, _typ, body = self._recv()
            if resp_id == rid:
                collected += body
                # A command response may be split over several packets; a short
                # read timeout means we got everything.
                try:
                    self.sock.settimeout(0.15)
                    resp_id, _typ, more = self._recv()
                    if resp_id == rid:
                        collected += more
                except socket.timeout:
                    pass
                finally:
                    self.sock.settimeout(10.0)
                return collected.strip()
            # response for a previous (unmatched) request - ignore
        raise RconError("timeout waiting for rcon response to: " + command)


def connect(retries=60, delay=1.0):
    last = None
    for attempt in range(retries):
        try:
            return Rcon()
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

    log("connected to rcon")
    check_positions = [(13, 4, 14), (14, 4, 14), (14, 4, 15), (14, 4, 16), (14, 5, 14), (15, 4, 15), (14, 5, 50)]
    repeat_anchor_z = [20, 26, 32, 38, 44]

    def setup_world():
        for command in [
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "gamerule sendCommandFeedback false",
            "difficulty peaceful",
            "time set noon",
            "weather clear",
            "fill 8 3 8 18 8 50 minecraft:air",
            "fill 8 2 8 18 2 50 minecraft:stone",
            "setblock 14 4 14 mekanism:basic_mechanical_pipe",
            "setblock 14 4 16 mekanism:basic_mechanical_pipe",
            "setblock 14 4 50 mekanism:basic_mechanical_pipe",
            "setblock 12 3 50 minecraft:stone",
        ] + ["setblock 14 4 %d mekanism:basic_mechanical_pipe" % z for z in repeat_anchor_z]:
            run(command)

    player_ready = False
    marker_re = re.compile(r"MPT_AUTOTEST (.*)$")
    tp_re = re.compile(r"TP (\S+) (\S+) (\S+) (\S+)")
    check_cell_re = re.compile(r"CHECK_CELL (\S+) (-?\d+) (-?\d+) (-?\d+)")
    offset = 0
    buffer = ""
    handled = set()
    deadline = time.time() + 900
    log("watching %s" % log_path)

    while time.time() < deadline:
        if os.path.exists(log_path):
            if os.path.getsize(log_path) < offset:
                # latest.log was rotated/truncated by a client restart; start over.
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
                        run("tp %s 14.5 3.0 14.5" % PLAYER)
                        setup_world()
                        for command in [
                            "clear %s" % PLAYER,
                            "gamemode creative %s" % PLAYER,
                            "op %s" % PLAYER,
                            "give %s mekanism:configurator" % PLAYER,
                            "give %s mekanism:basic_mechanical_pipe 64" % PLAYER,
                            "give %s mekanism:basic_universal_cable 64" % PLAYER,
                        ]:
                            run(command)
                    tp_match = tp_re.match(marker)
                    if tp_match:
                        label, x, y, z = tp_match.groups()
                        run("tp %s %s %s %s" % (PLAYER, x, y, z))
                        log("teleported for %s" % label)
                        for pos in check_positions:
                            out = run("execute if block %d %d %d mekanism:basic_mechanical_pipe run data get block %d %d %d"
                                      % (pos + pos))
                            if "has the following block data" in out:
                                log("  pipe at %s: %s" % (pos, out.split("block data:")[-1].strip()[:120]))
                    check_match = check_cell_re.match(marker)
                    if check_match:
                        label, ax, ay, az = check_match.groups()
                        ax, ay, az = int(ax), int(ay), int(az)
                        checks = [
                            ("anchor", ax, ay, az),
                            ("cell", ax, ay + 1, az),
                            ("duplicate", ax - 1, ay, az),
                        ]
                        for name, x, y, z in checks:
                            out = run("execute if block %d %d %d mekanism:basic_mechanical_pipe run data get block %d %d %d"
                                      % (x, y, z, x, y, z))
                            present = "has the following block data" in out
                            log("  CHECK %s %s at (%d,%d,%d): %s%s"
                                % (label, name, x, y, z, "PIPE" if present else "air",
                                   (" " + out.split("block data:")[-1].strip()[:90]) if present else ""))
                    if marker.startswith("finished"):
                        log("client finished; exiting driver")
                        return
        time.sleep(0.1)
    log("driver timed out")


if __name__ == "__main__":
    main()
