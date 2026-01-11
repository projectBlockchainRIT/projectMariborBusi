package si.um.feri.mbusi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class UIRenderer implements Disposable {

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont fontRegular;
    private BitmapFont fontMedium;
    private BitmapFont fontBold;
    private BitmapFont fontSmall;
    private BitmapFont fontLarge;
    private BitmapFont fontDisplay;
    private GlyphLayout glyphLayout;

    private static final int CORNER_SEGMENTS = 8;

    public UIRenderer() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        glyphLayout = new GlyphLayout();

        initializeFonts();
    }

    private void initializeFonts() {
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/Inter-Regular.ttf"));

            FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();

            
            params.color = DesignSystem.TEXT_PRIMARY;
            params.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            params.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            params.hinting = FreeTypeFontGenerator.Hinting.Full;
            params.renderCount = 2; 
            params.gamma = 1.8f; 

            
            params.shadowColor = new Color(0, 0, 0, 0.25f);
            params.shadowOffsetX = 0;
            params.shadowOffsetY = 1;

            
            params.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "čćžšđČĆŽŠĐ";

            params.size = 14;
            fontRegular = generator.generateFont(params);

            params.size = 12;
            fontSmall = generator.generateFont(params);

            params.size = 18;
            fontMedium = generator.generateFont(params);

            params.size = 24;
            fontLarge = generator.generateFont(params);

            params.size = 36;
            fontDisplay = generator.generateFont(params);

            generator.dispose();

            try {
                FreeTypeFontGenerator boldGen = new FreeTypeFontGenerator(
                    Gdx.files.internal("fonts/Inter-Bold.ttf"));
                params.size = 14;
                fontBold = boldGen.generateFont(params);
                boldGen.dispose();
            } catch (Exception e) {
                fontBold = fontRegular;
            }

        } catch (Exception e) {
            fontRegular = new BitmapFont();
            fontSmall = new BitmapFont();
            fontSmall.getData().setScale(0.85f);
            fontMedium = new BitmapFont();
            fontMedium.getData().setScale(1.2f);
            fontBold = fontRegular;
            fontLarge = new BitmapFont();
            fontLarge.getData().setScale(1.5f);
            fontDisplay = new BitmapFont();
            fontDisplay.getData().setScale(2.2f);
        }
    }

    

    public void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    public void endShapes() {
        shapeRenderer.end();
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        shapeRenderer.setColor(color);

        radius = Math.min(radius, Math.min(width, height) / 2f);

        shapeRenderer.rect(x + radius, y, width - 2 * radius, height);

        shapeRenderer.rect(x, y + radius, radius, height - 2 * radius);
        shapeRenderer.rect(x + width - radius, y + radius, radius, height - 2 * radius);

        drawArc(x + radius, y + radius, radius, 180, 270);
        drawArc(x + width - radius, y + radius, radius, 270, 360);
        drawArc(x + width - radius, y + height - radius, radius, 0, 90);
        drawArc(x + radius, y + height - radius, radius, 90, 180);
    }

    public void drawGradientRoundedRect(float x, float y, float width, float height, float radius,
                                        Color topColor, Color bottomColor) {
        int strips = 10;
        float stripHeight = height / strips;

        for (int i = 0; i < strips; i++) {
            float t = (float) i / (strips - 1);
            Color stripColor = DesignSystem.lerp(bottomColor, topColor, t);
            float stripY = y + i * stripHeight;
            float stripRadius = (i == 0 || i == strips - 1) ? radius : 0;

            if (i == 0) {
                shapeRenderer.setColor(stripColor);
                shapeRenderer.rect(x + radius, stripY, width - 2 * radius, stripHeight);
                shapeRenderer.rect(x, stripY + radius, radius, stripHeight - radius);
                shapeRenderer.rect(x + width - radius, stripY + radius, radius, stripHeight - radius);
                drawArc(x + radius, stripY + radius, radius, 180, 270);
                drawArc(x + width - radius, stripY + radius, radius, 270, 360);
            } else if (i == strips - 1) {
                shapeRenderer.setColor(stripColor);
                shapeRenderer.rect(x + radius, stripY, width - 2 * radius, stripHeight);
                shapeRenderer.rect(x, stripY, radius, stripHeight - radius);
                shapeRenderer.rect(x + width - radius, stripY, radius, stripHeight - radius);
                drawArc(x + width - radius, stripY + stripHeight - radius, radius, 0, 90);
                drawArc(x + radius, stripY + stripHeight - radius, radius, 90, 180);
            } else {
                shapeRenderer.setColor(stripColor);
                shapeRenderer.rect(x, stripY, width, stripHeight);
            }
        }
    }

    public void drawPill(float x, float y, float width, float height, Color color) {
        drawRoundedRect(x, y, width, height, height / 2f, color);
    }

    public void drawCircle(float x, float y, float radius, Color color) {
        shapeRenderer.setColor(color);
        shapeRenderer.circle(x, y, radius, 32);
    }

    public void drawCircleWithGlow(float x, float y, float radius, Color color, float glowRadius) {
        
        for (int i = 3; i >= 0; i--) {
            float t = (float) i / 3f;
            float r = radius + glowRadius * (1 - t);
            Color glowColor = DesignSystem.withAlpha(color, 0.1f * (1 - t));
            shapeRenderer.setColor(glowColor);
            shapeRenderer.circle(x, y, r, 32);
        }
        
        shapeRenderer.setColor(color);
        shapeRenderer.circle(x, y, radius, 32);
    }

    public void drawRing(float x, float y, float radius, float thickness, Color color) {
        shapeRenderer.setColor(color);
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) i / segments * MathUtils.PI2;
            float angle2 = (float) (i + 1) / segments * MathUtils.PI2;

            float x1Out = x + MathUtils.cos(angle1) * radius;
            float y1Out = y + MathUtils.sin(angle1) * radius;
            float x2Out = x + MathUtils.cos(angle2) * radius;
            float y2Out = y + MathUtils.sin(angle2) * radius;

            float x1In = x + MathUtils.cos(angle1) * (radius - thickness);
            float y1In = y + MathUtils.sin(angle1) * (radius - thickness);
            float x2In = x + MathUtils.cos(angle2) * (radius - thickness);
            float y2In = y + MathUtils.sin(angle2) * (radius - thickness);

            shapeRenderer.triangle(x1Out, y1Out, x2Out, y2Out, x1In, y1In);
            shapeRenderer.triangle(x1In, y1In, x2Out, y2Out, x2In, y2In);
        }
    }

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, Color color) {
        shapeRenderer.setColor(color);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.1f) return;

        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        shapeRenderer.rect(x1, y1 - thickness / 2f, 0, thickness / 2f,
            length, thickness, 1, 1, angle);

        shapeRenderer.circle(x1, y1, thickness / 2f, 16);
        shapeRenderer.circle(x2, y2, thickness / 2f, 16);
    }

    public void drawProgressBar(float x, float y, float width, float height, float progress,
                                Color bgColor, Color fgColor) {
        drawRoundedRect(x, y, width, height, height / 2f, bgColor);

        float fgWidth = Math.max(height, width * MathUtils.clamp(progress, 0, 1));
        drawRoundedRect(x, y, fgWidth, height, height / 2f, fgColor);
    }

    private void drawArc(float cx, float cy, float radius, float startAngle, float endAngle) {
        float step = (endAngle - startAngle) / CORNER_SEGMENTS;
        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a1 = (startAngle + i * step) * MathUtils.degreesToRadians;
            float a2 = (startAngle + (i + 1) * step) * MathUtils.degreesToRadians;
            shapeRenderer.triangle(
                cx, cy,
                cx + MathUtils.cos(a1) * radius, cy + MathUtils.sin(a1) * radius,
                cx + MathUtils.cos(a2) * radius, cy + MathUtils.sin(a2) * radius
            );
        }
    }

    

    public void beginText() {
        batch.begin();
    }

    public void endText() {
        batch.end();
    }

    public void drawText(String text, float x, float y, Color color) {
        fontRegular.setColor(color);
        fontRegular.draw(batch, text, x, y);
    }

    public void drawTextSmall(String text, float x, float y, Color color) {
        fontSmall.setColor(color);
        fontSmall.draw(batch, text, x, y);
    }

    public void drawTextMedium(String text, float x, float y, Color color) {
        fontMedium.setColor(color);
        fontMedium.draw(batch, text, x, y);
    }

    public void drawTextBold(String text, float x, float y, Color color) {
        fontBold.setColor(color);
        fontBold.draw(batch, text, x, y);
    }

    public void drawTextLarge(String text, float x, float y, Color color) {
        fontLarge.setColor(color);
        fontLarge.draw(batch, text, x, y);
    }

    public void drawTextDisplay(String text, float x, float y, Color color) {
        fontDisplay.setColor(color);
        fontDisplay.draw(batch, text, x, y);
    }

    public void drawTextCentered(String text, float x, float y, float width, Color color, BitmapFont font) {
        glyphLayout.setText(font, text);
        float textX = x + (width - glyphLayout.width) / 2f;
        font.setColor(color);
        font.draw(batch, text, textX, y);
    }

    public float getTextWidth(String text, BitmapFont font) {
        glyphLayout.setText(font, text);
        return glyphLayout.width;
    }

    public float getTextHeight(String text, BitmapFont font) {
        glyphLayout.setText(font, text);
        return glyphLayout.height;
    }

    

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public BitmapFont getFontRegular() {
        return fontRegular;
    }

    public BitmapFont getFontSmall() {
        return fontSmall;
    }

    public BitmapFont getFontMedium() {
        return fontMedium;
    }

    public BitmapFont getFontBold() {
        return fontBold;
    }

    public BitmapFont getFontLarge() {
        return fontLarge;
    }

    public BitmapFont getFontDisplay() {
        return fontDisplay;
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (fontRegular != null) fontRegular.dispose();
        if (fontSmall != null && fontSmall != fontRegular) fontSmall.dispose();
        if (fontMedium != null && fontMedium != fontRegular) fontMedium.dispose();
        if (fontBold != null && fontBold != fontRegular) fontBold.dispose();
        if (fontLarge != null && fontLarge != fontRegular) fontLarge.dispose();
        if (fontDisplay != null && fontDisplay != fontRegular) fontDisplay.dispose();
    }
}
