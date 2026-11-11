package online.farmsteadminecraft.footprints.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import online.farmsteadminecraft.footprints.Footprints;
import online.farmsteadminecraft.footprints.ModLoaderAccess;

/**
 * The print texture (the hollow's shadow and the lip around it, side by side), built at runtime: the mod's own
 * 13 shapes plus every picture the player put in {@code config/farmstead_footprints/textures/}.
 *
 * <p>A player's picture can be anything with a foot on it: dark on white, or a shape on a transparent
 * background, toes pointing up. It is trimmed, fitted into a 64 px cell and turned into the same two
 * layers the built-in prints have (a deeper middle, an occluded inner wall, a lip just outside), so it
 * looks pressed into the ground like the rest. The file name becomes the shape name for config
 * profiles. Everything stays one texture per layer, so all prints still draw in one batch each.</p>
 *
 * <p>The textures are read straight from the mod jar and registered by hand, so this needs no
 * resource-loading API from the loader.</p>
 */
public final class FootprintAtlas {
    static final int CELL = 64;
    private static final int BUILT_IN_GRID = 4;
    /** Ink below this is paper, not print. */
    private static final int NOISE = 18;
    private static final float FILL = 0.80f;

    /** Crumbling decay: the atlas holds this many copies of every print, each worn further from the edges. */
    private static int stages = 6;
    private static int grid = BUILT_IN_GRID;
    private static boolean built;
    private static int generation;
    /** One texture, two halves: the hollow's shadow on the left, the lip on the right (one draw for both). */
    private static ResourceLocation textureId = Footprints.id("textures/footprints_shadow.png");
    private static final List<String> customNames = new ArrayList<>();

    private FootprintAtlas() {
    }

    public static int grid() {
        return grid;
    }

    /** Decay stages stacked downwards in the texture; stage 0 (the top) is the fresh print. */
    public static int stages() {
        return stages;
    }

    public static ResourceLocation texture() {
        return textureId;
    }

    /** Names of the player's pictures found by the last build. */
    public static List<String> customNames() {
        return customNames;
    }

    public static Path folder() {
        return ModLoaderAccess.get().configDir().resolve("farmstead_footprints").resolve("textures");
    }

    /** Rebuild on the next {@link #ensure()} (config reload: new or changed pictures). */
    public static void invalidate() {
        built = false;
    }

    /** Builds the textures if needed; render thread only. */
    public static void ensure() {
        if (built) return;
        built = true;
        try {
            build();
        } catch (Exception e) {
            Footprints.LOGGER.error("footprint textures could not be built", e);
        }
    }

    private static void build() throws IOException {
        List<Path> pictures = listPictures();
        int cells = FootShape.FIRST_CUSTOM_CELL + pictures.size();
        int g = BUILT_IN_GRID;
        while (g * g < cells && g < 16) g *= 2;
        int size = g * CELL;
        NativeImage shadow = new NativeImage(size, size, true);
        NativeImage rim = new NativeImage(size, size, true);
        try (NativeImage baseShadow = readResource("footprints_shadow.png");
             NativeImage baseRim = readResource("footprints_rim.png")) {
            for (int c = 0; c < BUILT_IN_GRID * BUILT_IN_GRID; c++) {
                copyCell(baseShadow, c, BUILT_IN_GRID, shadow, c, g);
                copyCell(baseRim, c, BUILT_IN_GRID, rim, c, g);
            }
        }
        grid = g;
        FootShape.clearCustom();
        customNames.clear();
        int cell = FootShape.FIRST_CUSTOM_CELL;
        for (Path file : pictures) {
            if (cell >= g * g) {
                Footprints.LOGGER.warn("textures: more than {} pictures, {} and later are skipped", g * g - FootShape.FIRST_CUSTOM_CELL, file.getFileName());
                break;
            }
            String name = file.getFileName().toString();
            name = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
            try (InputStream in = Files.newInputStream(file); NativeImage src = NativeImage.read(in)) {
                float aspect = drawCustom(src, shadow, rim, cell, g, name.hashCode());
                // the longer side is half a block, like a bare foot; "size:" in a profile scales it
                float length = aspect <= 1f ? 0.5f : 0.5f / aspect, width = aspect <= 1f ? 0.5f * aspect : 0.5f;
                FootShape.custom(name, cell, width, length);
                customNames.add(name);
                cell++;
            } catch (Exception e) {
                Footprints.LOGGER.warn("textures: {} is not a readable PNG ({})", file.getFileName(), e.toString());
            }
        }
        generation++;
        // a big atlas of players' own pictures gets fewer stages, to keep the texture small
        stages = g >= 16 ? 3 : 6;
        NativeImage both = new NativeImage(size * 2, size * stages, true);
        float[] shadowOrder = new float[size * size], rimOrder = new float[size * size];
        for (int c = 0; c < g * g; c++) erosionOrder(shadow, rim, c, g, shadowOrder, rimOrder);
        for (int s = 0; s < stages; s++) {
            // stage s wears away every pixel whose order is below t: the lip first, then the hollow from its edge in
            float t = s / (float) stages;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int i = y * size + x;
                    set(both, x, s * size + y, wear(get(shadow, x, y), shadowOrder[i], t));
                    set(both, size + x, s * size + y, wear(get(rim, x, y), rimOrder[i], Math.min(1f, t * 1.7f)));
                }
            }
        }
        shadow.close();
        rim.close();
        textureId = register("prints", both);
        // cached texture coordinates and resolved profiles point at the old layout
        FootprintsClient.POOL.forEachAlive(Footprint::prepare);
        FootProfiles.reset();
        if (!customNames.isEmpty()) Footprints.LOGGER.info("textures: {} own picture(s): {}", customNames.size(), customNames);
    }

    /** Keeps a pixel while its erosion order is above the stage threshold, with a soft edge. */
    private static int wear(int pixel, float order, float threshold) {
        if (threshold <= 0f) return pixel;
        float keep = Math.max(0f, Math.min(1f, (order - threshold) / 0.07f));
        int a = (int) (((pixel >>> 24) & 0xFF) * keep);
        return (a << 24) | (pixel & 0xFFFFFF);
    }

    /**
     * When each pixel of one cell crumbles away, 0 (first) .. 1 (last). Inside the hollow it goes by the distance from
     * the print's edge, broken up by coarse noise so the edge crumbles in bites rather than shrinking evenly; the lip
     * crumbles in noisy patches.
     */
    private static void erosionOrder(NativeImage shadow, NativeImage rim, int cell, int g, float[] shadowOrder, float[] rimOrder) {
        int size = g * CELL, ox = (cell % g) * CELL, oy = (cell / g) * CELL;
        // city-block distance from the outside of the print, two passes
        int[] d = new int[CELL * CELL];
        int max = 0;
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                boolean in = ((get(shadow, ox + x, oy + y) >>> 24) & 0xFF) > 60;
                int v = 0;
                if (in) {
                    int left = x > 0 ? d[y * CELL + x - 1] : 0, up = y > 0 ? d[(y - 1) * CELL + x] : 0;
                    v = Math.min(left, up) + 1;
                }
                d[y * CELL + x] = v;
            }
        }
        for (int y = CELL - 1; y >= 0; y--) {
            for (int x = CELL - 1; x >= 0; x--) {
                int i = y * CELL + x, v = d[i];
                if (v == 0) continue;
                int right = x < CELL - 1 ? d[i + 1] : 0, down = y < CELL - 1 ? d[i + CELL] : 0;
                v = Math.min(v, Math.min(right, down) + 1);
                d[i] = v;
                max = Math.max(max, v);
            }
        }
        Random rnd = new Random(cell * 7919L + 17);
        // coarse value noise on a 9 px lattice: the edge goes in bites, not as an even shrink
        int lattice = 9, n = CELL / lattice + 2;
        float[] lat = new float[n * n];
        for (int i = 0; i < lat.length; i++) lat[i] = rnd.nextFloat();
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                float fx = x / (float) lattice, fy = y / (float) lattice;
                int ix = (int) fx, iy = (int) fy;
                float tx = fx - ix, ty = fy - iy;
                tx = tx * tx * (3 - 2 * tx);
                ty = ty * ty * (3 - 2 * ty);
                float a = lat[iy * n + ix], b = lat[iy * n + ix + 1], c = lat[(iy + 1) * n + ix], e = lat[(iy + 1) * n + ix + 1];
                float top = a + (b - a) * tx, bottom = c + (e - c) * tx;
                float coarse = top + (bottom - top) * ty;
                float fine = rnd.nextFloat();
                float depth = max == 0 ? 0f : d[y * CELL + x] / (float) max;
                int i = (oy + y) * size + ox + x;
                shadowOrder[i] = 0.62f * depth + 0.30f * coarse + 0.08f * fine;
                rimOrder[i] = 0.75f * coarse + 0.25f * fine;
            }
        }
        // ranks, not raw values: stage s of n then wears away s/n of the print, whatever its shape
        rank(shadow, ox, oy, size, shadowOrder);
        rank(rim, ox, oy, size, rimOrder);
    }

    private static void rank(NativeImage layer, int ox, int oy, int size, float[] order) {
        List<int[]> px = new ArrayList<>();
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                int i = (oy + y) * size + ox + x;
                if (((get(layer, ox + x, oy + y) >>> 24) & 0xFF) > 6) px.add(new int[]{i, Float.floatToIntBits(order[i])});
                else order[i] = 0.001f;
            }
        }
        px.sort((a, b) -> Float.compare(Float.intBitsToFloat(a[1]), Float.intBitsToFloat(b[1])));
        for (int k = 0; k < px.size(); k++) order[px.get(k)[0]] = (k + 1f) / px.size();
    }

    private static List<Path> listPictures() throws IOException {
        Path dir = folder();
        Files.createDirectories(dir);
        Path readme = dir.resolve("README.txt");
        if (!Files.exists(readme)) Files.writeString(readme, README);
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.{png,PNG}")) {
            for (Path f : files) out.add(f);
        }
        out.sort(null);
        return out;
    }

    private static ResourceLocation register(String layer, NativeImage image) {
        ResourceLocation id = Footprints.id("runtime/" + layer + "_" + generation);
        //? if >=26.1 {
        /*DynamicTexture texture = new DynamicTexture(() -> "footprints " + layer, image);
        *///?} else
        DynamicTexture texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    private static NativeImage readResource(String file) throws IOException {
        String path = "/assets/" + Footprints.MOD_ID + "/textures/" + file;
        try (InputStream in = FootprintAtlas.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path + " in the mod jar");
            return NativeImage.read(in);
        }
    }

    private static void copyCell(NativeImage src, int srcCell, int srcGrid, NativeImage dst, int dstCell, int dstGrid) {
        int sx = (srcCell % srcGrid) * CELL, sy = (srcCell / srcGrid) * CELL;
        int dx = (dstCell % dstGrid) * CELL, dy = (dstCell / dstGrid) * CELL;
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) set(dst, dx + x, dy + y, get(src, sx + x, sy + y));
        }
    }

    /**
     * Player picture -> ink (how much "foot" each pixel is), trimmed and fitted into the cell, then the two
     * layers. Returns width/length of the trimmed picture.
     */
    private static float drawCustom(NativeImage src, NativeImage shadow, NativeImage rim, int cell, int g, int seed) {
        int w = src.getWidth(), h = src.getHeight();
        boolean hasAlpha = false;
        for (int y = 0; y < h && !hasAlpha; y += Math.max(1, h / 64)) {
            for (int x = 0; x < w; x += Math.max(1, w / 64)) {
                if (((get(src, x, y) >>> 24) & 0xFF) < 250) {
                    hasAlpha = true;
                    break;
                }
            }
        }
        float[] ink = new float[w * h];
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = get(src, x, y);
                int a = (p >>> 24) & 0xFF;
                // channel order differs between versions; an average of the three is the same either way
                float lum = (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 765f;
                float v = hasAlpha ? a / 255f * (1f - 0.5f * lum) * 2f : 1f - lum;
                v = Math.min(1f, v);
                if (v * 255 < NOISE) v = 0f;
                ink[y * w + x] = v;
                if (v > 0f) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) throw new IllegalArgumentException("the picture is empty");
        int bw = maxX - minX + 1, bh = maxY - minY + 1;
        float scale = FILL * CELL / Math.max(bw, bh);
        int nw = Math.max(1, Math.round(bw * scale)), nh = Math.max(1, Math.round(bh * scale));
        int ox = (CELL - nw) / 2, oy = (CELL - nh) / 2;
        float[] fitted = new float[CELL * CELL];
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                // area average of the source pixels this cell pixel covers
                int x0 = minX + (int) (x / scale), x1 = Math.max(x0 + 1, minX + (int) ((x + 1) / scale));
                int y0 = minY + (int) (y / scale), y1 = Math.max(y0 + 1, minY + (int) ((y + 1) / scale));
                float sum = 0;
                int n = 0;
                for (int yy = y0; yy < Math.min(y1, h); yy++) {
                    for (int xx = x0; xx < Math.min(x1, w); xx++) {
                        sum += ink[yy * w + xx];
                        n++;
                    }
                }
                fitted[(oy + y) * CELL + ox + x] = n == 0 ? 0 : sum / n;
            }
        }
        int[][] layers = layers(fitted, new Random(seed));
        int dx = (cell % g) * CELL, dy = (cell / g) * CELL;
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                set(shadow, dx + x, dy + y, (layers[0][y * CELL + x] << 24) | 0xFFFFFF);
                set(rim, dx + x, dy + y, (layers[1][y * CELL + x] << 24) | 0xFFFFFF);
            }
        }
        return bw / (float) bh;
    }

    /** Same recipe as tools/build_footprints.py: shadow alpha and lip alpha, 0..255. */
    static int[][] layers(float[] ink, Random rnd) {
        int n = CELL * CELL;
        float[] mask = new float[n];
        for (int i = 0; i < n; i++) mask[i] = ink[i] > 40 / 255f ? 1f : 0f;
        mask = blur(mask, 0.7f);
        float[] wall = blur(sub(mask, minFilter(mask, 2)), 1.0f);
        float[] lip = sub(blur(maxFilter(mask, 3), 1.6f), maxFilter(mask, 1));
        int[] shadow = new int[n], rim = new int[n];
        for (int i = 0; i < n; i++) {
            float grain = (rnd.nextFloat() - 0.5f) * 0.12f;
            float a = mask[i] * (0.28f + 0.62f * ink[i] + grain) + 0.22f * wall[i];
            shadow[i] = clamp255(a);
            rim[i] = clamp255(lip[i] * (0.55f + (rnd.nextFloat() - 0.5f) * 0.3f));
        }
        return new int[][]{shadow, rim};
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255)));
    }

    private static float[] sub(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Math.max(0f, a[i] - b[i]);
        return out;
    }

    private static float[] minFilter(float[] a, int r) {
        return rankFilter(a, r, true);
    }

    private static float[] maxFilter(float[] a, int r) {
        return rankFilter(a, r, false);
    }

    private static float[] rankFilter(float[] a, int r, boolean min) {
        float[] out = new float[a.length];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                float v = min ? 1f : 0f;
                for (int yy = Math.max(0, y - r); yy <= Math.min(CELL - 1, y + r); yy++) {
                    for (int xx = Math.max(0, x - r); xx <= Math.min(CELL - 1, x + r); xx++) {
                        float s = a[yy * CELL + xx];
                        v = min ? Math.min(v, s) : Math.max(v, s);
                    }
                }
                out[y * CELL + x] = v;
            }
        }
        return out;
    }

    private static float[] blur(float[] a, float sigma) {
        int r = (int) Math.ceil(sigma * 3);
        float[] k = new float[2 * r + 1];
        float sum = 0;
        for (int i = -r; i <= r; i++) sum += k[i + r] = (float) Math.exp(-(i * i) / (2 * sigma * sigma));
        for (int i = 0; i < k.length; i++) k[i] /= sum;
        float[] tmp = new float[a.length], out = new float[a.length];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                float v = 0;
                for (int i = -r; i <= r; i++) v += k[i + r] * a[y * CELL + Math.max(0, Math.min(CELL - 1, x + i))];
                tmp[y * CELL + x] = v;
            }
        }
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                float v = 0;
                for (int i = -r; i <= r; i++) v += k[i + r] * tmp[Math.max(0, Math.min(CELL - 1, y + i)) * CELL + x];
                out[y * CELL + x] = v;
            }
        }
        return out;
    }

    private static int get(NativeImage img, int x, int y) {
        //? if >=26.1 {
        /*return img.getPixel(x, y);
        *///?} else
        return img.getPixelRGBA(x, y);
    }

    private static void set(NativeImage img, int x, int y, int pixel) {
        //? if >=26.1 {
        /*img.setPixel(x, y, pixel);
        *///?} else
        img.setPixelRGBA(x, y, pixel);
    }

    private static final String README = """
        Your own footprints / Свои следы
        ================================

        Put PNG pictures here and use them for any player or mob.

        * One foot per picture, toes pointing UP. Dark on white, or a shape on a transparent background.
          Any size; it is trimmed and fitted automatically, and the mod draws the hollow and the raised lip.
        * The file name is the shape name: wolf_paw.png -> wolf_paw.
        * In config/farmstead_footprints-client.toml, footprints.profiles, e.g.
              profiles = ["minecraft:player = shape:wolf_paw", "#minecraft:skeletons = shape:bone size:0.8"]
        * Type /footprints reload in game to pick up new or changed pictures.

        Положите сюда PNG и назначьте их любому мобу или игроку.

        * Одна стопа на картинку, пальцы смотрят ВВЕРХ. Тёмное на белом или фигура на прозрачном фоне.
          Размер любой: мод сам обрежет, впишет и нарисует впадину и край.
        * Имя файла — имя формы: wolf_paw.png -> wolf_paw.
        * В config/farmstead_footprints-client.toml, ключ footprints.profiles, например
              profiles = ["minecraft:player = shape:wolf_paw", "#minecraft:skeletons = shape:bone size:0.8"]
        * Команда /footprints reload в игре подхватит новые и изменённые картинки.
        """;
}
