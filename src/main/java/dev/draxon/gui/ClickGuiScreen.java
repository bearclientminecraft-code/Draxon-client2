package dev.draxon.gui;
import dev.draxon.core.ConfigManager;
import dev.draxon.core.DraxonClient;
import dev.draxon.core.KeybindManager;
import dev.draxon.gui.AnimationManager.Anim;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.Setting;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** UNVERIFIED against 1.21.11: Click/KeyInput/CharInput signatures, DrawContext matrix + scissor behaviour. */
public class ClickGuiScreen extends Screen {
    private static final int PW = 112, HEAD = 22, ROW = 16, GAP = 8, ROWS = 12, THEME_W = 140, SEARCH_W = 150;
    private final ThemeManager t = DraxonClient.theme;
    private final SettingRenderer r = new SettingRenderer(t);
    private final Anim open = new Anim(0), themeAnim = new Anim(0);
    private boolean closing, themeOpen, openKeyReleased, searchFocus;
    private long last = System.nanoTime();
    private final Map<Module, Anim> toggle = new HashMap<>(), hover = new HashMap<>(), expand = new HashMap<>();
    private final float[] scroll = new float[5], scrollTarget = new float[5];
    private String search = "";

    public ClickGuiScreen() { super(Text.literal("Draxon")); open.target = 1; }
    @Override public boolean shouldPause() { return false; }
    /** ESC and Right Shift both end here: fade out, then setScreen(null). No other screen is opened. */
    @Override public void close() { closing = true; open.target = 0; }

    // ---- geometry (all in "virtual" GUI space, scaled about the screen center) ----
    private int panelH() { return ROWS * ROW + HEAD; }
    private int originX() { return (width - (5 * PW + 4 * GAP)) / 2; }
    private int originY() { return (height - panelH()) / 2 - 12; }
    private float baseScale() { return Math.min(t.scale.get().floatValue(), (width - 16f) / (5 * PW + 4 * GAP)); }
    private float curScale() { return baseScale() * (0.94f + 0.06f * open.value); }
    private int toGuiX(double x) { return (int) ((x - width / 2f) / curScale() + width / 2f); }
    private int toGuiY(double y) { return (int) ((y - height / 2f) / curScale() + height / 2f); }
    private int searchX() { return (width - SEARCH_W) / 2; }
    private int searchY() { return originY() + panelH() + 10; }
    private int themeH() { int h = HEAD + 6; for (Setting<?> s : t.settings) h += SettingRenderer.height(s) + 3; return h; }
    private int themeX() { return (width - THEME_W) / 2; }

    private Anim a(Map<Module, Anim> m, Module k, float init) { return m.computeIfAbsent(k, x -> new Anim(init)); }
    private List<Module> visible(Category c) {
        List<Module> l = new ArrayList<>(); String q = search.toLowerCase();
        for (Module m : DraxonClient.modules.in(c)) if (q.isEmpty() || m.name.toLowerCase().contains(q)) l.add(m);
        return l;
    }
    private static int settingsHeight(Module m) { int h = 3; for (Setting<?> s : m.settings) h += SettingRenderer.height(s) + 1; return h; }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.nanoTime(); float dt = Math.min((now - last) / 1e9f, 0.1f); last = now;
        if (!openKeyReleased && GLFW.glfwGetKey(client.getWindow().getHandle(), KeybindManager.GUI_KEY) != GLFW.GLFW_PRESS) openKeyReleased = true;
        open.update(dt, 14f); themeAnim.target = themeOpen ? 1 : 0; themeAnim.update(dt, 16f);
        if (closing && open.value <= 0.01f) { client.setScreen(null); return; }
        for (int i = 0; i < 5; i++) scroll[i] += (scrollTarget[i] - scroll[i]) * (1f - (float) Math.exp(-16 * dt));
        float al = open.value;
        super.render(c, mx, my, delta); // vanilla background: includes the game's own menu blur when enabled in Video Settings
        c.fill(0, 0, width, height, ThemeManager.argb(t.background, 0.30f * al));
        float sc = curScale();
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(width / 2f, height / 2f); c.getMatrices().scale(sc, sc); c.getMatrices().translate(-width / 2f, -height / 2f);
        int vmx = toGuiX(mx), vmy = toGuiY(my); r.dt = dt;
        boolean modal = themeOpen;
        pass(c, modal ? -9999 : vmx, modal ? -9999 : vmy, al, dt, 0, 0);
        drawSearch(c, al, modal ? -9999 : vmx, modal ? -9999 : vmy);
        if (themeAnim.value > 0.01f) drawTheme(c, al * themeAnim.value);
        c.getMatrices().popMatrix();
    }

    private void drawSearch(DrawContext c, float al, int mx, int my) {
        int sx = searchX(), sy = searchY();
        RenderUtils.roundRect(c, sx, sy, SEARCH_W, 16, t.rad(), ThemeManager.argb(t.panel, t.op() * al));
        RenderUtils.border(c, sx, sy, SEARCH_W, 16, ThemeManager.argb(searchFocus ? t.acc() : 0xFFFFFF, (searchFocus ? 0.6f : 0.08f) * al));
        String shown = search.isEmpty() ? "Search modules..." : search + (searchFocus && System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
        FontManager.draw(c, shown, sx + 6, sy + 4, ThemeManager.argb(search.isEmpty() ? t.disabled : t.text, al));
        if (!search.isEmpty()) FontManager.draw(c, "x", sx + SEARCH_W - 10, sy + 4, ThemeManager.argb(t.textDim, al));
        int gx = sx - 22; boolean over = mx >= gx && mx < gx + 16 && my >= sy && my < sy + 16;
        RenderUtils.roundRect(c, gx, sy, 16, 16, t.rad(), ThemeManager.argb(over || themeOpen ? t.secondary : t.panel, t.op() * al));
        RenderUtils.border(c, gx, sy, 16, 16, ThemeManager.argb(themeOpen ? t.acc() : 0xFFFFFF, (themeOpen ? 0.6f : 0.08f) * al));
        Icons.draw(c, Category.MISC, gx + 1, sy + 1, themeOpen ? t.acc() : t.text, al);
    }

    private void drawTheme(DrawContext c, float a) {
        c.fill(-width, -height, 2 * width, 2 * height, ThemeManager.argb(0x000000, 0.35f * a));
        int x = themeX(), y = originY(), h = themeH();
        RenderUtils.glassPanel(c, x, y, THEME_W, h, t, a);
        FontManager.draw(c, "Interface", x + 8, y + 7, ThemeManager.argb(t.text, a));
        Icons.draw(c, Category.MISC, x + THEME_W - 8 - Icons.SIZE, y + 4, t.text, a);
        int cy = y + HEAD;
        for (Setting<?> s : t.settings) { r.draw(c, s, x + 10, cy, THEME_W - 20, a); cy += SettingRenderer.height(s) + 3; }
    }

    /** action: 0 = render, 1 = click. One traversal for both keeps hit-testing identical to drawing. */
    private boolean pass(DrawContext c, int mx, int my, float al, float dt, int action, int button) {
        int ox = originX(), oy = originY();
        for (Category cat : Category.values()) {
            int i = cat.ordinal(), px = ox + i * (PW + GAP), py = oy;
            if (action == 0) {
                RenderUtils.glassPanel(c, px, py, PW, panelH(), t, al);
                FontManager.draw(c, cat.label, px + 8, py + 7, ThemeManager.argb(t.text, al));
                Icons.draw(c, cat, px + PW - 8 - Icons.SIZE, py + 4, t.text, al);
                c.enableScissor(px, py + HEAD, px + PW, py + panelH());
            }
            float y = py + HEAD - scroll[i]; float total = 0;
            for (Module m : visible(cat)) {
                Anim tg = a(toggle, m, m.isEnabled() ? 1 : 0), hv = a(hover, m, 0), ex = a(expand, m, 0);
                int ry = (int) y; boolean inPanel = my >= py + HEAD && my < py + panelH();
                boolean over = inPanel && mx >= px && mx < px + PW && my >= ry && my < ry + ROW;
                int setBtnX = px + PW - 42, swX = px + PW - 28;
                if (action == 0) {
                    tg.target = m.isEnabled() ? 1 : 0; hv.target = over ? 1 : 0; tg.update(dt, 18); hv.update(dt, 20); ex.update(dt, 14);
                    if (hv.value > 0.01f) RenderUtils.roundRect(c, px + 3, ry, PW - 6, ROW - 1, 4, ThemeManager.argb(t.secondary, 0.9f * hv.value * al));
                    FontManager.draw(c, m.name, px + 8, ry + 4, ThemeManager.argb(m.isEnabled() ? t.text : t.textDim, al));
                    FontManager.draw(c, "...", setBtnX, ry + 3, ThemeManager.argb(ex.target > 0.5f ? t.acc() : t.disabled, al));
                    RenderUtils.roundRect(c, swX, ry + 3, 20, 10, 5, ThemeManager.argb(SettingRenderer.mix(t.track, t.acc(), tg.value), al));
                    RenderUtils.roundRect(c, swX + 1 + (int) (10 * tg.value), ry + 4, 8, 8, 4, ThemeManager.argb(0xFFFFFF, al));
                } else if (over && !closing) {
                    if (button == 1 || (mx >= setBtnX && mx < setBtnX + 14)) { ex.target = ex.target > 0.5f ? 0 : 1; return true; }
                    if (button == 0) { m.toggle(); return true; }
                }
                y += ROW; total += ROW;
                int sh = (int) (settingsHeight(m) * ex.value);
                if (sh > 0) {
                    int sy0 = (int) y;
                    if (action == 0) c.enableScissor(px, Math.max(sy0, py + HEAD), px + PW, Math.max(Math.max(sy0, py + HEAD), Math.min(sy0 + sh, py + panelH())));
                    int cy = sy0 + 2;
                    for (Setting<?> s : m.settings) {
                        if (action == 0) r.draw(c, s, px + 8, cy, PW - 16, al * ex.value);
                        else if (ex.value > 0.9f && inPanel && !closing && r.click(s, px + 8, cy, PW - 16, mx, my, button)) { ConfigManager.markDirty(); return true; }
                        cy += SettingRenderer.height(s) + 1;
                    }
                    if (action == 0) c.disableScissor();
                    y += sh; total += sh;
                }
            }
            if (action == 0) {
                c.disableScissor();
                scrollTarget[i] = Math.max(0, Math.min(Math.max(0, total - (panelH() - HEAD)), scrollTarget[i]));
            }
        }
        return false;
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        int mx = toGuiX(click.x()), my = toGuiY(click.y()), b = click.button();
        int sx = searchX(), sy = searchY(), gx = sx - 22;
        if (mx >= gx && mx < gx + 16 && my >= sy && my < sy + 16) { r.clearFocus(); searchFocus = false; themeOpen = !themeOpen; return true; }
        if (themeOpen) {
            int x = themeX(), y = originY(), h = themeH();
            if (mx < x || mx >= x + THEME_W || my < y || my >= y + h) { themeOpen = false; r.clearFocus(); ConfigManager.markDirty(); return true; }
            r.clearFocus(); int cy = y + HEAD;
            for (Setting<?> s : t.settings) { if (r.click(s, x + 10, cy, THEME_W - 20, mx, my, b)) break; cy += SettingRenderer.height(s) + 3; }
            return true;
        }
        if (mx >= sx && mx < sx + SEARCH_W && my >= sy && my < sy + 16) {
            r.clearFocus(); searchFocus = true;
            if (!search.isEmpty() && mx > sx + SEARCH_W - 14) search = "";
            return true;
        }
        searchFocus = false; r.clearFocus();
        return pass(null, mx, my, 0, 0, 1, b);
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (r.dragging != null) { r.dragTo(toGuiX(click.x())); return true; }
        return super.mouseDragged(click, dx, dy);
    }
    @Override public boolean mouseReleased(Click click) { if (r.dragging != null) ConfigManager.markDirty(); r.release(); return super.mouseReleased(click); }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (themeOpen) return true;
        int gx = toGuiX(mx), ox = originX();
        for (int i = 0; i < 5; i++) { int px = ox + i * (PW + GAP); if (gx >= px && gx < px + PW) scrollTarget[i] -= (float) v * 20; }
        return true;
    }
    @Override public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (r.capturing()) { r.key(k); return true; }
        if (k == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (searchFocus) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); return true; }
            if (k == GLFW.GLFW_KEY_ENTER) { searchFocus = false; return true; }
        }
        // ignore the key repeat of the Right Shift press that opened the GUI until it has been released once
        if (k == KeybindManager.GUI_KEY) { if (openKeyReleased) close(); return true; }
        return super.keyPressed(input);
    }
    @Override public boolean charTyped(CharInput input) {
        String s = input.asString();
        if (r.typed(s)) return true;
        if (searchFocus) { if (search.length() < 24) search += s; return true; }
        return super.charTyped(input);
    }
    @Override public void removed() { r.clearFocus(); ConfigManager.saveAll(t, DraxonClient.modules); }

}
