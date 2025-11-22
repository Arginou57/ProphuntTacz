package com.yourname.prophunt.game;

import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores map corner points for each player when creating maps
 */
public class PlayerMapPoints {
    private static final Map<UUID, MapCorners> playerPoints = new HashMap<>();

    public static class MapCorners {
        public BlockPos point1;
        public BlockPos point2;
        public BlockPos point3;
        public BlockPos point4;

        public MapCorners(BlockPos point1, BlockPos point2, BlockPos point3, BlockPos point4) {
            this.point1 = point1;
            this.point2 = point2;
            this.point3 = point3;
            this.point4 = point4;
        }

        public boolean isAllSet() {
            return point1 != null && point2 != null && point3 != null && point4 != null;
        }

        public int getMinX() {
            int min = Integer.MAX_VALUE;
            if (point1 != null) min = Math.min(min, point1.getX());
            if (point2 != null) min = Math.min(min, point2.getX());
            if (point3 != null) min = Math.min(min, point3.getX());
            if (point4 != null) min = Math.min(min, point4.getX());
            return min == Integer.MAX_VALUE ? 0 : min;
        }

        public int getMaxX() {
            int max = Integer.MIN_VALUE;
            if (point1 != null) max = Math.max(max, point1.getX());
            if (point2 != null) max = Math.max(max, point2.getX());
            if (point3 != null) max = Math.max(max, point3.getX());
            if (point4 != null) max = Math.max(max, point4.getX());
            return max == Integer.MIN_VALUE ? 0 : max;
        }

        public int getMinY() {
            int min = Integer.MAX_VALUE;
            if (point1 != null) min = Math.min(min, point1.getY());
            if (point2 != null) min = Math.min(min, point2.getY());
            if (point3 != null) min = Math.min(min, point3.getY());
            if (point4 != null) min = Math.min(min, point4.getY());
            return min == Integer.MAX_VALUE ? 0 : min;
        }

        public int getMaxY() {
            int max = Integer.MIN_VALUE;
            if (point1 != null) max = Math.max(max, point1.getY());
            if (point2 != null) max = Math.max(max, point2.getY());
            if (point3 != null) max = Math.max(max, point3.getY());
            if (point4 != null) max = Math.max(max, point4.getY());
            return max == Integer.MIN_VALUE ? 0 : max;
        }

        public int getMinZ() {
            int min = Integer.MAX_VALUE;
            if (point1 != null) min = Math.min(min, point1.getZ());
            if (point2 != null) min = Math.min(min, point2.getZ());
            if (point3 != null) min = Math.min(min, point3.getZ());
            if (point4 != null) min = Math.min(min, point4.getZ());
            return min == Integer.MAX_VALUE ? 0 : min;
        }

        public int getMaxZ() {
            int max = Integer.MIN_VALUE;
            if (point1 != null) max = Math.max(max, point1.getZ());
            if (point2 != null) max = Math.max(max, point2.getZ());
            if (point3 != null) max = Math.max(max, point3.getZ());
            if (point4 != null) max = Math.max(max, point4.getZ());
            return max == Integer.MIN_VALUE ? 0 : max;
        }
    }

    /**
     * Set point 1 for a player
     */
    public static void setPoint1(UUID playerId, BlockPos pos) {
        MapCorners corners = playerPoints.computeIfAbsent(playerId, k -> new MapCorners(null, null, null, null));
        corners.point1 = pos;
    }

    /**
     * Set point 2 for a player
     */
    public static void setPoint2(UUID playerId, BlockPos pos) {
        MapCorners corners = playerPoints.computeIfAbsent(playerId, k -> new MapCorners(null, null, null, null));
        corners.point2 = pos;
    }

    /**
     * Set point 3 for a player
     */
    public static void setPoint3(UUID playerId, BlockPos pos) {
        MapCorners corners = playerPoints.computeIfAbsent(playerId, k -> new MapCorners(null, null, null, null));
        corners.point3 = pos;
    }

    /**
     * Set point 4 for a player
     */
    public static void setPoint4(UUID playerId, BlockPos pos) {
        MapCorners corners = playerPoints.computeIfAbsent(playerId, k -> new MapCorners(null, null, null, null));
        corners.point4 = pos;
    }

    /**
     * Get stored points for a player
     */
    public static MapCorners getPoints(UUID playerId) {
        return playerPoints.get(playerId);
    }

    /**
     * Clear points for a player
     */
    public static void clearPoints(UUID playerId) {
        playerPoints.remove(playerId);
    }

    /**
     * Check if a player has all 4 points set
     */
    public static boolean hasAllPoints(UUID playerId) {
        MapCorners corners = playerPoints.get(playerId);
        return corners != null && corners.isAllSet();
    }

    /**
     * Get points info display
     */
    public static String getPointsInfo(UUID playerId) {
        MapCorners corners = playerPoints.get(playerId);
        if (corners == null) {
            return "§cNo points set";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(corners.point1 != null ? "§aPoint 1: " + formatPos(corners.point1) : "§cPoint 1: not set");
        sb.append(" §e| ");
        sb.append(corners.point2 != null ? "§aPoint 2: " + formatPos(corners.point2) : "§cPoint 2: not set");
        sb.append("\n");
        sb.append(corners.point3 != null ? "§aPoint 3: " + formatPos(corners.point3) : "§cPoint 3: not set");
        sb.append(" §e| ");
        sb.append(corners.point4 != null ? "§aPoint 4: " + formatPos(corners.point4) : "§cPoint 4: not set");

        if (corners.isAllSet()) {
            int sizeX = corners.getMaxX() - corners.getMinX() + 1;
            int sizeY = corners.getMaxY() - corners.getMinY() + 1;
            int sizeZ = corners.getMaxZ() - corners.getMinZ() + 1;
            sb.append(" §e(§f").append(sizeX).append("x").append(sizeY).append("x").append(sizeZ).append("§e)");
        }

        return sb.toString();
    }

    private static String formatPos(BlockPos pos) {
        return "§f(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}
