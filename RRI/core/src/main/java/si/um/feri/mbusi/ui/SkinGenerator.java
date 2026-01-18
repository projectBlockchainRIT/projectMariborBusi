package si.um.feri.mbusi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import si.um.feri.mbusi.ui.DesignSystem;

public class SkinGenerator {

  public static Skin generateModernSkin() {
    Skin skin = new Skin();

    generateFonts(skin);
    addColors(skin);
    generateTextures(skin);
    registerStyles(skin);

    return skin;
  }

  private static void generateFonts(Skin skin) {
    FileHandle fontFile = Gdx.files.internal("fonts/Inter-Regular.ttf");
    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);

    FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

    parameter.size = 14;
    parameter.color = Color.WHITE;
    parameter.borderWidth = 0;
    parameter.shadowOffsetX = 0;
    parameter.shadowOffsetY = 0;
    parameter.minFilter = Texture.TextureFilter.Linear;
    parameter.magFilter = Texture.TextureFilter.Linear;
    BitmapFont defaultFont = generator.generateFont(parameter);
    skin.add("default", defaultFont);

    parameter.size = 12;
    BitmapFont smallFont = generator.generateFont(parameter);
    skin.add("small", smallFont);

    // Medium font (use Inter-Medium if available, else Regular)
    FileHandle mediumFontFile = Gdx.files.internal("fonts/Inter-Medium.ttf");
    if (!mediumFontFile.exists()) {
      mediumFontFile = fontFile;
    }
    FreeTypeFontGenerator mediumGenerator = new FreeTypeFontGenerator(mediumFontFile);
    parameter.size = 16;
    BitmapFont mediumFont = mediumGenerator.generateFont(parameter);
    skin.add("medium", mediumFont);
    mediumGenerator.dispose();

    // Large font (use Inter-Bold if available, else Regular)
    FileHandle boldFontFile = Gdx.files.internal("fonts/Inter-Bold.ttf");
    if (!boldFontFile.exists()) {
      boldFontFile = fontFile;
    }
    FreeTypeFontGenerator boldGenerator = new FreeTypeFontGenerator(boldFontFile);
    parameter.size = 20;
    BitmapFont largeFont = boldGenerator.generateFont(parameter);
    skin.add("large", largeFont);
    boldGenerator.dispose();

    generator.dispose();
  }

  private static void addColors(Skin skin) {
    skin.add("white", Color.WHITE);
    skin.add("black", Color.BLACK);
    skin.add("accent", DesignSystem.ACCENT_PRIMARY);
    skin.add("surface", DesignSystem.SURFACE_GLASS);
    skin.add("surfaceLight", new Color(0.114f, 0.125f, 0.153f, 1f));
    skin.add("text", DesignSystem.TEXT_PRIMARY);
    skin.add("textSecondary", DesignSystem.TEXT_SECONDARY);
    skin.add("success", DesignSystem.SUCCESS);
    skin.add("warning", DesignSystem.WARNING);
    skin.add("error", DesignSystem.ERROR);
  }

  private static void generateTextures(Skin skin) {
    Pixmap whitePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    whitePixmap.setColor(Color.WHITE);
    whitePixmap.fill();
    skin.add("white", new Texture(whitePixmap));
    whitePixmap.dispose();

    skin.add("button-up", createRoundedButton(120, 40, 12, new Color(0.114f, 0.125f, 0.153f, 1f)), Drawable.class);
    skin.add("button-over", createRoundedButton(120, 40, 12, new Color(0.157f, 0.173f, 0.212f, 1f)), Drawable.class);
    skin.add("button-down", createRoundedButton(120, 40, 12, new Color(0.090f, 0.098f, 0.125f, 1f)), Drawable.class);

    skin.add("button-primary-up", createRoundedButton(120, 40, 12, DesignSystem.ACCENT_PRIMARY), Drawable.class);
    skin.add("button-primary-over", createRoundedButton(120, 40, 12, new Color(0.31f, 0.62f, 1f, 1f)), Drawable.class);
    skin.add("button-primary-down", createRoundedButton(120, 40, 12, new Color(0.20f, 0.48f, 0.89f, 1f)), Drawable.class);

    skin.add("play-button-up", createCircleButton(40, DesignSystem.ACCENT_PRIMARY), Drawable.class);
    skin.add("play-button-over", createCircleButton(40, new Color(0.31f, 0.62f, 1f, 1f)), Drawable.class);
    skin.add("play-button-down", createCircleButton(40, new Color(0.20f, 0.48f, 0.89f, 1f)), Drawable.class);

    skin.add("slider-bg", createRoundedRect(200, 4, 2, new Color(1f, 1f, 1f, 0.2f)), Drawable.class);
    skin.add("slider-fill", createRoundedRect(200, 4, 2, DesignSystem.ACCENT_PRIMARY), Drawable.class);
    skin.add("slider-knob", createCircle(16, Color.WHITE), Drawable.class);

    skin.add("window-bg", createRoundedPanel(400, 300, 16, DesignSystem.SURFACE_GLASS), Drawable.class);

    skin.add("textfield-bg", createRoundedButton(200, 36, 8, new Color(0.114f, 0.125f, 0.153f, 0.8f)), Drawable.class);
    skin.add("cursor", createRect(2, 20, Color.WHITE), Drawable.class);
    skin.add("selection", createRect(1, 20, new Color(DesignSystem.ACCENT_PRIMARY.r, DesignSystem.ACCENT_PRIMARY.g, DesignSystem.ACCENT_PRIMARY.b, 0.4f)), Drawable.class);
  }

  private static void registerStyles(Skin skin) {
    com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle textButtonStyle;
    com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle sliderStyle;
    com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle labelStyle;

    textButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
    textButtonStyle.up = skin.getDrawable("button-up");
    textButtonStyle.down = skin.getDrawable("button-down");
    textButtonStyle.over = skin.getDrawable("button-over");
    textButtonStyle.font = skin.getFont("medium");
    textButtonStyle.fontColor = skin.getColor("text");
    textButtonStyle.downFontColor = Color.WHITE;
    skin.add("default", textButtonStyle);

    textButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
    textButtonStyle.up = skin.getDrawable("button-primary-up");
    textButtonStyle.down = skin.getDrawable("button-primary-down");
    textButtonStyle.over = skin.getDrawable("button-primary-over");
    textButtonStyle.font = skin.getFont("medium");
    textButtonStyle.fontColor = Color.WHITE;
    textButtonStyle.downFontColor = Color.WHITE;
    skin.add("primary", textButtonStyle);

    textButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
    textButtonStyle.up = skin.getDrawable("play-button-up");
    textButtonStyle.down = skin.getDrawable("play-button-down");
    textButtonStyle.over = skin.getDrawable("play-button-over");
    textButtonStyle.font = skin.getFont("default");
    textButtonStyle.fontColor = Color.WHITE;
    textButtonStyle.downFontColor = Color.WHITE;
    skin.add("play", textButtonStyle);

    sliderStyle = new com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle();
    sliderStyle.background = skin.getDrawable("slider-bg");
    sliderStyle.knob = skin.getDrawable("slider-knob");
    sliderStyle.knobBefore = skin.getDrawable("slider-fill");
    skin.add("default-horizontal", sliderStyle);

    labelStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
    labelStyle.font = skin.getFont("default");
    labelStyle.fontColor = skin.getColor("text");
    skin.add("default", labelStyle);

    labelStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
    labelStyle.font = skin.getFont("small");
    labelStyle.fontColor = skin.getColor("textSecondary");
    skin.add("small", labelStyle);

    labelStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
    labelStyle.font = skin.getFont("medium");
    labelStyle.fontColor = skin.getColor("text");
    skin.add("medium", labelStyle);

    labelStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
    labelStyle.font = skin.getFont("large");
    labelStyle.fontColor = skin.getColor("text");
    skin.add("large", labelStyle);
  }

  private static NinePatchDrawable createRoundedButton(int width, int height, int cornerRadius, Color color) {
    int padding = cornerRadius + 2;
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    fillRoundedRect(pixmap, 0, 0, width, height, cornerRadius);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    NinePatch ninePatch = new NinePatch(texture, padding, padding, padding, padding);
    return new NinePatchDrawable(ninePatch);
  }

  private static NinePatchDrawable createRoundedPanel(int width, int height, int cornerRadius, Color color) {
    int padding = cornerRadius + 2;
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    fillRoundedRect(pixmap, 0, 0, width, height, cornerRadius);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    NinePatch ninePatch = new NinePatch(texture, padding, padding, padding, padding);
    return new NinePatchDrawable(ninePatch);
  }

  private static NinePatchDrawable createRoundedRect(int width, int height, int cornerRadius, Color color) {
    int padding = cornerRadius + 1;
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    fillRoundedRect(pixmap, 0, 0, width, height, cornerRadius);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    NinePatch ninePatch = new NinePatch(texture, padding, padding, padding, padding);
    return new NinePatchDrawable(ninePatch);
  }

  private static NinePatchDrawable createCircleButton(int diameter, Color color) {
    Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    int padding = diameter / 2;
    NinePatch ninePatch = new NinePatch(texture, padding, padding, padding, padding);
    return new NinePatchDrawable(ninePatch);
  }

  private static NinePatchDrawable createCircle(int diameter, Color color) {
    Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    int padding = diameter / 2;
    NinePatch ninePatch = new NinePatch(texture, padding, padding, padding, padding);
    return new NinePatchDrawable(ninePatch);
  }

  private static NinePatchDrawable createRect(int width, int height, Color color) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    NinePatch ninePatch = new NinePatch(texture, 1, 1, 1, 1);
    return new NinePatchDrawable(ninePatch);
  }

  private static void fillRoundedRect(Pixmap pixmap, int x, int y, int width, int height, int radius) {
    pixmap.fillRectangle(x + radius, y, width - 2 * radius, height);
    pixmap.fillRectangle(x, y + radius, width, height - 2 * radius);

    pixmap.fillCircle(x + radius, y + radius, radius);
    pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
    pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
    pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
  }
}
