package si.um.feri.mbusi.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Modern design system for MbusiiMap
 * Inspired by transit apps with a dark, sleek aesthetic
 */
public class DesignSystem {

    // === PRIMARY PALETTE ===
    // Dark base with electric blue accents
    public static final Color BACKGROUND_DARK = new Color(0.067f, 0.075f, 0.098f, 1f);       // #11131A
    public static final Color SURFACE_DARK = new Color(0.098f, 0.110f, 0.141f, 1f);          // #191C24
    public static final Color SURFACE_ELEVATED = new Color(0.129f, 0.145f, 0.184f, 1f);      // #21252F
    public static final Color SURFACE_GLASS = new Color(0.129f, 0.145f, 0.184f, 0.85f);      // Glass effect

    // === ACCENT COLORS ===
    public static final Color ACCENT_PRIMARY = new Color(0.247f, 0.557f, 0.969f, 1f);        // #3F8EF7 Electric Blue
    public static final Color ACCENT_SECONDARY = new Color(0.608f, 0.318f, 0.878f, 1f);      // #9B51E0 Purple
    public static final Color ACCENT_GRADIENT_START = new Color(0.247f, 0.557f, 0.969f, 1f); // Blue
    public static final Color ACCENT_GRADIENT_END = new Color(0.400f, 0.851f, 0.937f, 1f);   // Cyan #66D9EF

    // === SEMANTIC COLORS ===
    public static final Color SUCCESS = new Color(0.180f, 0.800f, 0.443f, 1f);               // #2ECC71 Green
    public static final Color WARNING = new Color(0.945f, 0.769f, 0.059f, 1f);               // #F1C40F Yellow
    public static final Color ERROR = new Color(0.906f, 0.298f, 0.235f, 1f);                 // #E74C3C Red
    public static final Color INFO = new Color(0.204f, 0.596f, 0.859f, 1f);                  // #3498DB

    // === TEXT COLORS ===
    public static final Color TEXT_PRIMARY = new Color(0.961f, 0.965f, 0.980f, 1f);          // #F5F7FA
    public static final Color TEXT_SECONDARY = new Color(0.651f, 0.682f, 0.749f, 1f);        // #A6AEBF
    public static final Color TEXT_MUTED = new Color(0.431f, 0.467f, 0.533f, 1f);            // #6E7788
    public static final Color TEXT_INVERSE = new Color(0.067f, 0.075f, 0.098f, 1f);          // Dark

    // === BORDER & DIVIDER ===
    public static final Color BORDER = new Color(0.200f, 0.220f, 0.271f, 1f);                // #333847
    public static final Color BORDER_LIGHT = new Color(0.200f, 0.220f, 0.271f, 0.5f);
    public static final Color DIVIDER = new Color(1f, 1f, 1f, 0.06f);

    // === SHADOWS ===
    public static final Color SHADOW_LIGHT = new Color(0f, 0f, 0f, 0.15f);
    public static final Color SHADOW_MEDIUM = new Color(0f, 0f, 0f, 0.3f);
    public static final Color SHADOW_HEAVY = new Color(0f, 0f, 0f, 0.5f);

    // === OVERLAYS ===
    public static final Color OVERLAY_DARK = new Color(0f, 0f, 0f, 0.6f);
    public static final Color OVERLAY_LIGHT = new Color(1f, 1f, 1f, 0.05f);

    // === BUS LINE COLORS (Refined Palette) ===
    public static final Color[] LINE_COLORS = {
        new Color(0.247f, 0.557f, 0.969f, 1f),   // Electric Blue
        new Color(0.906f, 0.298f, 0.235f, 1f),   // Coral Red
        new Color(0.180f, 0.800f, 0.443f, 1f),   // Emerald Green
        new Color(0.945f, 0.608f, 0.059f, 1f),   // Amber Orange
        new Color(0.608f, 0.318f, 0.878f, 1f),   // Royal Purple
        new Color(0.000f, 0.737f, 0.831f, 1f),   // Turquoise
        new Color(0.914f, 0.282f, 0.494f, 1f),   // Rose Pink
        new Color(0.537f, 0.753f, 0.286f, 1f),   // Lime Green
        new Color(0.969f, 0.788f, 0.247f, 1f),   // Golden Yellow
        new Color(0.380f, 0.380f, 0.914f, 1f),   // Indigo
        new Color(0.000f, 0.651f, 0.569f, 1f),   // Teal
        new Color(0.878f, 0.412f, 0.122f, 1f),   // Burnt Orange
        new Color(0.745f, 0.224f, 0.533f, 1f),   // Magenta
        new Color(0.400f, 0.851f, 0.937f, 1f),   // Sky Cyan
        new Color(0.620f, 0.565f, 0.435f, 1f),   // Warm Taupe
    };

    // === SPACING SYSTEM (8px grid) ===
    public static final float SPACE_XXS = 4f;
    public static final float SPACE_XS = 8f;
    public static final float SPACE_SM = 12f;
    public static final float SPACE_MD = 16f;
    public static final float SPACE_LG = 24f;
    public static final float SPACE_XL = 32f;
    public static final float SPACE_XXL = 48f;

    // === CORNER RADIUS ===
    public static final float RADIUS_SM = 6f;
    public static final float RADIUS_MD = 10f;
    public static final float RADIUS_LG = 16f;
    public static final float RADIUS_XL = 24f;
    public static final float RADIUS_PILL = 100f;

    // === TYPOGRAPHY SCALE ===
    public static final float FONT_SIZE_XS = 10f;
    public static final float FONT_SIZE_SM = 12f;
    public static final float FONT_SIZE_MD = 14f;
    public static final float FONT_SIZE_LG = 18f;
    public static final float FONT_SIZE_XL = 24f;
    public static final float FONT_SIZE_XXL = 32f;
    public static final float FONT_SIZE_DISPLAY = 48f;

    // === ANIMATION DURATIONS ===
    public static final float ANIM_FAST = 0.15f;
    public static final float ANIM_NORMAL = 0.25f;
    public static final float ANIM_SLOW = 0.4f;
    public static final float ANIM_VERY_SLOW = 0.6f;

    // === UI DIMENSIONS ===
    public static final float PANEL_WIDTH = 280f;
    public static final float HEADER_HEIGHT = 56f;
    public static final float FOOTER_HEIGHT = 48f;
    public static final float BUTTON_HEIGHT = 40f;
    public static final float BUTTON_HEIGHT_SM = 32f;
    public static final float ICON_SIZE_SM = 16f;
    public static final float ICON_SIZE_MD = 24f;
    public static final float ICON_SIZE_LG = 32f;

    // === MAP SPECIFIC ===
    public static final Color MAP_BACKGROUND = new Color(0.118f, 0.133f, 0.165f, 1f);        // #1E2229
    public static final float STATION_GLOW_RADIUS = 12f;
    public static final float BUS_MARKER_SIZE = 28f;

    private DesignSystem() {}

    /**
     * Get a line color by index with wrap-around
     */
    public static Color getLineColor(int index) {
        return LINE_COLORS[index % LINE_COLORS.length];
    }

    /**
     * Create a darkened version of a color
     */
    public static Color darken(Color color, float amount) {
        return new Color(
            Math.max(0, color.r - amount),
            Math.max(0, color.g - amount),
            Math.max(0, color.b - amount),
            color.a
        );
    }

    /**
     * Create a lightened version of a color
     */
    public static Color lighten(Color color, float amount) {
        return new Color(
            Math.min(1, color.r + amount),
            Math.min(1, color.g + amount),
            Math.min(1, color.b + amount),
            color.a
        );
    }

    /**
     * Create a color with modified alpha
     */
    public static Color withAlpha(Color color, float alpha) {
        return new Color(color.r, color.g, color.b, alpha);
    }

    /**
     * Interpolate between two colors
     */
    public static Color lerp(Color from, Color to, float t) {
        return new Color(
            from.r + (to.r - from.r) * t,
            from.g + (to.g - from.g) * t,
            from.b + (to.b - from.b) * t,
            from.a + (to.a - from.a) * t
        );
    }
}
