package dev.codeonigiri.mekanism_pipe_tweaks.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class GuideGeometry {
    public static final double CELL_BOUNDARY = 0.25;

    private GuideGeometry() {
    }

    public record Basis(Vec3 right, Vec3 up, Vec3 normal) {
    }

    public record Cell(Direction side, int col, int row) {
    }

    public static Basis basis(Direction face) {
        return switch (face) {
            case UP -> new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, -1), new Vec3(0, 1, 0));
            case DOWN -> new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, -1, 0));
            case NORTH -> new Basis(new Vec3(-1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, -1));
            case SOUTH -> new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
            case WEST -> new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0), new Vec3(-1, 0, 0));
            case EAST -> new Basis(new Vec3(0, 0, -1), new Vec3(0, 1, 0), new Vec3(1, 0, 0));
        };
    }

    public static Cell cellAt(Direction face, Vec3 hitLocation, BlockPos pos) {
        Basis basis = basis(face);
        Vec3 offset = hitLocation.subtract(Vec3.atCenterOf(pos));
        double u = offset.dot(basis.right());
        double v = offset.dot(basis.up());
        int col = u < -CELL_BOUNDARY ? 0 : u > CELL_BOUNDARY ? 2 : 1;
        int row = v > CELL_BOUNDARY ? 0 : v < -CELL_BOUNDARY ? 2 : 1;
        return new Cell(sideFor(face, col, row), col, row);
    }

    public static Direction sideFor(Direction face, int col, int row) {
        Basis basis = basis(face);
        if (col == 1 && row == 1) {
            return face;
        }
        if (col != 1 && row != 1) {
            return face.getOpposite();
        }
        if (row == 0) {
            return directionOf(basis.up());
        }
        if (row == 2) {
            return directionOf(basis.up().scale(-1));
        }
        if (col == 0) {
            return directionOf(basis.right().scale(-1));
        }
        return directionOf(basis.right());
    }

    public static Direction directionOf(Vec3 axisAligned) {
        for (Direction direction : Direction.values()) {
            if (axisAligned.dot(Vec3.atLowerCornerOf(direction.getNormal())) > 0.5) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Not axis aligned: " + axisAligned);
    }

    public static double cellU(int col) {
        return col == 0 ? -0.375 : col == 2 ? 0.375 : 0.0;
    }

    public static double cellV(int row) {
        return row == 0 ? 0.375 : row == 2 ? -0.375 : 0.0;
    }
}
