package dev.draxon.render;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
/** Interpolated entity box projected to the screen. Static buffers: no per-frame allocation except the lerped position. */
public final class BoundingBoxUtils {
    public static final float[] px = new float[8], py = new float[8];
    public static float minX, minY, maxX, maxY;
    public static final int[] HULL = new int[10];
    private static final double[] out = new double[3];
    /** Corner index bits: 1 = +x, 2 = +z, 4 = +y. */
    public static final int[][] EDGES = {{0,1},{1,3},{3,2},{2,0},{4,5},{5,7},{7,6},{6,4},{0,4},{1,5},{2,6},{3,7}};
    private BoundingBoxUtils() {}
    public static boolean compute(Entity e, float td) {
        Vec3d p = e.getLerpedPos(td); double hw = e.getWidth() / 2.0, h = e.getHeight();
        minX = minY = Float.MAX_VALUE; maxX = maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double x = p.x + ((i & 1) == 0 ? -hw : hw), z = p.z + ((i & 2) == 0 ? -hw : hw), y = p.y + ((i & 4) == 0 ? 0 : h);
            if (!ProjectionUtils.project(x, y, z, out)) return false;
            px[i] = (float) out[0]; py[i] = (float) out[1];
            minX = Math.min(minX, px[i]); maxX = Math.max(maxX, px[i]); minY = Math.min(minY, py[i]); maxY = Math.max(maxY, py[i]);
        }
        return maxX - minX < 4000 && maxY - minY < 4000;
    }
    /** Convex hull (gift wrapping) of the 8 projected corners into HULL; returns vertex count. */
    public static int hull() {
        int start = 0;
        for (int i = 1; i < 8; i++) if (px[i] < px[start] || (px[i] == px[start] && py[i] < py[start])) start = i;
        int n = 0, cur = start;
        do {
            HULL[n++] = cur; int next = (cur + 1) % 8;
            for (int i = 0; i < 8; i++) {
                if (i == cur) continue;
                float cross = (px[next] - px[cur]) * (py[i] - py[cur]) - (py[next] - py[cur]) * (px[i] - px[cur]);
                if (next == cur || cross < 0 || (cross == 0 && dist2(cur, i) > dist2(cur, next))) next = i;
            }
            cur = next;
        } while (cur != start && n < 9);
        return n;
    }
    private static float dist2(int a, int b) { float dx = px[a] - px[b], dy = py[a] - py[b]; return dx * dx + dy * dy; }
}
