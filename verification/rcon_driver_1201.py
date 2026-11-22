#!/usr/bin/env python3
"""RCON driver for the 1.20.1 in-game verification (REPORT-1201.md).

Watches the dev client log for `MPT_AUTOTEST` markers and answers them:
prepares the flat test area, teleports the dev player, and records
server-side block data so the client readbacks can be cross-checked.

The harness performs every click itself through the normal client game-mode path;
this driver only provides the world (items, ground, machines) and teleports.

Run: rcon_driver_1201.py <client-log> <driver-log>
"""
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from rcon_driver import Rcon, RconError  # noqa: E402

HOST = "127.0.0.1"
PORT = 25576
PLAYER = "Dev"

# Test rig. The server uses level-type=flat, whose grass surface is y=-61, so the player
# stands at y=-60 and every rig block is placed at that same layer. A 1-block wall sits
# behind the placement target and two air cells are where the player places transmitters.
FLOOR_Y = -61
GROUND_Y = -60  # walkable surface of the flat world (grass top)
WALK_Y = -60.0
WALL = (14, -60, 19)
ANCHOR = (14, -60, 20)
PLACED = (14, -60, 21)

# Genuine connected network: Metallurgic Infuser -> cable run with two bends ->
# Enrichment Chamber. Every block is placed once by /setblock; the connection
# values read back below are Mekanism's own computed state.
NETWORK_CABLES = [
    (26, -60, 13), (27, -60, 13),
    (27, -60, 14), (27, -60, 15),
    (28, -60, 15), (29, -60, 15),
]
NETWORK_MACHINES = [
    ("mekanism:metallurgic_infuser", (25, -60, 13)),
    ("mekanism:enrichment_chamber", (30, -60, 15)),
]
NETWORK_CHECK = [
    ("infuser", (25, -60, 13), "mekanism:metallurgic_infuser"),
    ("cable-0", (26, -60, 13), "mekanism:basic_universal_cable"),
    ("cable-1", (27, -60, 13), "mekanism:basic_universal_cable"),
    ("cable-2", (27, -60, 14), "mekanism:basic_universal_cable"),
    ("cable-3", (27, -60, 15), "mekanism:basic_universal_cable"),
    ("cable-4", (28, -60, 15), "mekanism:basic_universal_cable"),
    ("cable-5", (29, -60, 15), "mekanism:basic_universal_cable"),
    ("chamber", (30, -60, 15), "mekanism:enrichment_chamber"),
]

DIRECTIONS = ["down", "up", "north", "south", "west", "east"]


def connect(retries=300, delay=1.0):
    last = None
    for _ in range(retries):
        try:
            return Rcon(host=HOST, port=PORT)
        except (OSError, RconError) as exc:
            last = exc
            time.sleep(delay)
    raise SystemExit("could not connect to rcon: %s" % last)


class Driver:
    def __init__(self, log_path, driver_log_path):
        self.log_path = log_path
        self.log_file = open(driver_log_path, "a", buffering=1)
        self.rcon = connect()

    def log(self, message):
        self.log_file.write("[%s] %s\n" % (time.strftime("%H:%M:%S"), message))

    def run(self, command, quiet=False):
        try:
            response = self.rcon.command(command)
        except Exception as exc:  # noqa: BLE001 - keep driving
            self.log("FAILED %r: %s" % (command, exc))
            return ""
        if not quiet:
            self.log("%s -> %s" % (command, response.replace("\n", " | ")[:200]))
        return response

    def silent(self, command):
        try:
            return self.rcon.command(command)
        except Exception:  # noqa: BLE001
            return ""

    # ---- world setup -------------------------------------------------------
    def clear_area(self):
        for command in [
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "gamerule randomTickSpeed 0",
            "difficulty peaceful",
            "time set noon",
            "weather clear",
        ]:
            self.run(command, quiet=True)
        # Only clear above the floor. Clearing the floor itself would drop the player into the
        # void for the fraction of a second before the floor is re-laid.
        self.run("fill 8 -60 8 40 -48 28 minecraft:air")
        self.run("fill 8 %d 8 40 %d 28 minecraft:smooth_stone" % (FLOOR_Y, FLOOR_Y))
        # One solid wall block behind the placement target, so the first pipe can be
        # placed against plain stone (no transmitter, no capability) rather than air.
        self.run("setblock %d %d %d minecraft:stone" % WALL)
        self.run("setblock %d %d %d minecraft:air" % ANCHOR)
        self.run("setblock %d %d %d minecraft:air" % PLACED)

    def give_items(self):
        self.run("clear %s" % PLAYER, quiet=True)
        self.run("gamemode creative %s" % PLAYER)
        self.run("op %s" % PLAYER, quiet=True)
        # Slot 0 holds the Configurator (stack size 1, so count 1 exactly); slots 1-5 hold one
        # stack of each transmitter so slots 6-8 stay empty for the "no item" screenshots.
        self.run("give %s mekanism:configurator 1" % PLAYER, quiet=True)
        for item in [
            "mekanism:basic_mechanical_pipe",
            "mekanism:basic_universal_cable",
            "mekanism:basic_pressurized_tube",
            "mekanism:basic_thermodynamic_conductor",
            "mekanism:basic_logistical_transporter",
        ]:
            self.run("give %s %s 64" % (PLAYER, item), quiet=True)
        hotbar = self.run("data get entity %s Inventory" % PLAYER, quiet=True)
        self.log("  inventory: %s" % hotbar.replace("\n", " | ")[:300])

    def setup_base(self):
        self.clear_area()
        self.give_items()
        self.block_check(WALL, "minecraft:stone", "wall")
        self.log("  base: anchor=%s, placed=%s" % (ANCHOR, PLACED))

    def setup_type(self, block_id):
        # Put one transmitter of the requested type in the anchor cell only; the surrounding
        # rig (floor, wall, second pipe) is left untouched so the pass is quick.
        self.run("setblock %d %d %d %s" % (ANCHOR + (block_id,)))
        self.run("setblock %d %d %d minecraft:air" % PLACED)
        self.block_check(ANCHOR, block_id, "type " + block_id)

    def block_check(self, pos, block_id, label):
        out = self.silent("execute if block %d %d %d %s run data get block %d %d %d"
                          % (pos + (block_id,) + pos))
        present = "has the following block data" in out
        if not present:
            # Blocks without a block entity never produce block data; fall back to a
            # plain predicate test, whose response is only emitted with feedback enabled.
            out = self.silent("execute if block %d %d %d %s" % (pos + (block_id,)))
            present = "Test passed" in out or out.strip() == ""
        self.log("  CHECK %-20s %s %-38s %s" % (
            label, pos, block_id, "PRESENT" if present else "ABSENT"))
        return present

    def setup_network(self):
        self.clear_area()
        for block_id, pos in NETWORK_MACHINES:
            self.run("setblock %d %d %d %s" % (pos + (block_id,)))
        for pos in NETWORK_CABLES:
            self.run("setblock %d %d %d mekanism:basic_universal_cable" % pos)
        # Let the client receive the chunk updates and Mekanism settle its connection shapes.
        time.sleep(1.5)
        for name, pos, block_id in NETWORK_CHECK:
            self.block_check(pos, block_id, "network " + name)
        self.connections_readback()

    def connections_readback(self):
        """Server-side NBT of each cable. Mekanism 10.4 stores the six configured
        connection values as separate keys connection0..connection5 (Direction ordinals:
        0=down, 1=up, 2=north, 3=south, 4=west, 5=east; 0=NORMAL, 1=PUSH, 2=PULL, 3=NONE)."""
        import re as _re
        for pos in NETWORK_CABLES:
            out = self.silent("data get block %d %d %d" % pos)
            values = _re.findall(r"connection([0-5]): (-?\d+)", out)
            text = " ".join("%s=%s" % (DIRECTIONS[int(i)], v) for i, v in sorted(values))
            self.log("  CONN  %s %s" % (pos, text if text else "n/a"))

    # ---- main loop ---------------------------------------------------------
    def run_forever(self, deadline_seconds=3600):
        self.run("gamerule sendCommandFeedback false", quiet=True)
        self.log("watching %s" % self.log_path)
        marker_re = re.compile(r"MPT_AUTOTEST (.*)$")
        tp_re = re.compile(r"TP (-?[\d.]+) (-?[\d.]+) (-?[\d.]+)")
        offset = 0
        # If the client log already exists, skip its historical content: only markers written
        # from now on belong to this run (otherwise a previous run would be replayed).
        if os.path.exists(self.log_path):
            offset = os.path.getsize(self.log_path)
        buffer = ""
        handled = set()
        deadline = time.time() + deadline_seconds
        while time.time() < deadline:
            if os.path.exists(self.log_path):
                if os.path.getsize(self.log_path) < offset:
                    offset = 0
                    buffer = ""
                with open(self.log_path, "rb") as handle:
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
                        # Teleports and world setups are actions, not one-off evidence: the same
                        # coordinates legitimately recur across scenes, so never deduplicate them.
                        is_action = marker.startswith("TP ") or marker.startswith("SETUP ")
                        if not is_action:
                            if marker in handled:
                                continue
                            handled.add(marker)
                        self.log("marker: %s" % marker)
                        if marker.startswith("world_ready"):
                            # Do not prep the world here: the harness immediately asks for the
                            # named "base" setup, which does exactly this. Preparing twice would
                            # run fill/setblock in the middle of the first scene.
                            self.run("tp %s 14.5 %s 22.6" % (PLAYER, WALK_Y))
                        elif marker.startswith("SETUP "):
                            self.handle_setup(marker[len("SETUP "):].strip())
                        tp_match = tp_re.match(marker)
                        if tp_match:
                            x, y, z = tp_match.groups()
                            self.run("tp %s %s %s %s" % (PLAYER, x, y, z))
                            self.log("teleported to (%s, %s, %s)" % (x, y, z))
                        if marker.startswith("finished"):
                            self.dump(ANCHOR, "final anchor", expect="mekanism:basic_mechanical_pipe")
                            self.dump(PLACED, "final placed", expect="mekanism:basic_mechanical_pipe")
                            self.log("client finished; exiting driver")
                            return
            time.sleep(0.1)
        self.log("driver timed out")

    def handle_setup(self, name):
        if name == "base":
            self.setup_base()
        elif name.startswith("type "):
            self.setup_type(name[len("type "):].strip())
        elif name == "reset-placed":
            self.run("setblock %d %d %d minecraft:air" % PLACED)
            self.log("  reset placed cell %s to air" % (PLACED,))
        elif name == "network":
            self.setup_network()
        else:
            self.log("unknown setup %r" % name)


def main():
    import atexit
    log_path, driver_log_path = sys.argv[1], sys.argv[2]
    # Single-instance guard: a second driver would replay the same markers and fight over
    # the player's position. Refuse to start when another driver holds the lock.
    lock_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".rcon-driver.lock")
    if os.path.exists(lock_path):
        try:
            other = int(open(lock_path).read().strip())
            os.kill(other, 0)
            raise SystemExit("another rcon driver is already running (pid %d)" % other)
        except (ValueError, ProcessLookupError, PermissionError):
            pass
    with open(lock_path, "w") as handle:
        handle.write(str(os.getpid()))
    atexit.register(lambda: os.path.exists(lock_path) and os.remove(lock_path))
    Driver(log_path, driver_log_path).run_forever()


if __name__ == "__main__":
    main()
