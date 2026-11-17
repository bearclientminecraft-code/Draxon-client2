package dev.draxon.gui;
/** border == 0 means "default thin white border". */
public record Theme(String name, int bg, int accent, int flower, int border) {
    public static final Theme[] ALL = {
        new Theme("Sakura", 0x2A0F26, 0xE83E8C, 0xF06292, 0),
        new Theme("Rose", 0x2B0A14, 0xD81B4A, 0xE53950, 0),
        new Theme("Sunflower", 0x2E1C0C, 0xF5A623, 0xF5A623, 0xE08A1E),
        new Theme("Lavender", 0x1F1A33, 0x9C6BFF, 0xA78BFA, 0),
        new Theme("Cornflower", 0x0F1B2E, 0x3B8BEB, 0x4DA3FF, 0),
        new Theme("Snowdrop", 0x0F1F1A, 0x22C55E, 0x34D399, 0)};
    public static Theme find(String n) { for (Theme t : ALL) if (t.name.equals(n)) return t; return null; }
}
