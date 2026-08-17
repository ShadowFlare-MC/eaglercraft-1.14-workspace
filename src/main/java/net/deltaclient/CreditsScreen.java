package net.deltaclient;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import net.lax1dude.eaglercraft.EagRuntime;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CreditsScreen extends Screen {

    private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
    private static final int LINE_HEIGHT = 12;
    private static final int WSPEED = 300;
    private static final int HSPEED = 220;
    private static final int TEXT_PAD_X = 14;
    private static final int TEXT_PAD_TOP = 8;

    private final RenderSkybox panorama;
    private final List<String> wrappedLines = Lists.newArrayList();
    private float scrollOffset;
    private int maxScroll;

    private int popupX, popupY;
    private int textLeft, textTop, textRight, textBottom;

    public CreditsScreen() {
        super(new StringTextComponent("Credits"));
        this.panorama = new RenderSkybox(MainMenuScreen.PANORAMA_RESOURCES);
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();

        popupX = (this.width - WSPEED) / 2;
        popupY = Math.max(15, Math.min((this.height - HSPEED) / 2, this.height - HSPEED - 15));

        int titleBarHeight = 28;
        int bottomBarHeight = 8;

        textLeft = popupX + TEXT_PAD_X;
        textTop = popupY + titleBarHeight;
        textRight = popupX + WSPEED - TEXT_PAD_X;
        textBottom = popupY + HSPEED - bottomBarHeight;

        //close buton
        this.addButton(new Button(popupX + WSPEED - 22, popupY + 4, 18, 18, "X", (p_213077_1_) -> {
            this.mc.displayGuiScreen((Screen) null);
        }));

        loadCredits();
    }

    private void loadCredits() {
        wrappedLines.clear();

        //load original eagler credits
        String creditsText = EagRuntime.getResourceString("/assets/eagler/CREDITS.txt");
        if (creditsText == null || creditsText.isEmpty()) {
            wrappedLines.add("(Credits file not found)");
            maxScroll = 0;
            return;
        }

        String[] lines = creditsText.split("\n");
        int availWidth = textRight - textLeft;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                wrappedLines.add("");
                continue;
            }

            if (line.matches("^[~%#=\\-\\s]{3,}$")) {
                wrappedLines.add(line);
                continue;
            }

            List<String> wrapped = this.font.listFormattedStringToWidth(line, availWidth);
            wrappedLines.addAll(wrapped);
        }

        int totalHeight = wrappedLines.size() * LINE_HEIGHT;
        int visibleHeight = textBottom - textTop;
        maxScroll = Math.max(0, totalHeight - visibleHeight);

        scrollOffset = MathHelper.clamp(scrollOffset, 0.0F, (float) maxScroll);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.panorama.render(partialTicks, 1.0F);

        this.mc.getTextureManager().bindTexture(PANORAMA_OVERLAY);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);

        fill(0, 0, this.width, this.height, 0x88000000);
        fill(popupX, popupY, popupX + WSPEED, popupY + HSPEED, 0xF0101010);
        fill(popupX, popupY, popupX + WSPEED, popupY + 1, 0x55FFFFFF);
        fill(popupX, popupY + HSPEED - 1, popupX + WSPEED, popupY + HSPEED, 0x55000000);
        fill(popupX, popupY, popupX + 1, popupY + HSPEED, 0x33FFFFFF);
        fill(popupX + WSPEED - 1, popupY, popupX + WSPEED, popupY + HSPEED, 0x33000000);

        if (this.font != null) {
            this.font.drawString(this.title.getFormattedText(),
                    (float) (popupX + (WSPEED - this.font.getStringWidth(this.title.getFormattedText())) / 2),
                    (float) (popupY + 8), 0xFFFFFF);
        }

        fill(textLeft, popupY + 26, textRight, popupY + 27, 0x33FFFFFF);

        //scissor it for cool looking
        double scale = this.mc.mainWindow.getGuiScaleFactor();
        int fbH = this.mc.mainWindow.getFramebufferHeight();
        int scW = textRight - textLeft;
        int scH = textBottom - textTop;

        GlStateManager.enableScissorTest();
        GlStateManager.scissor(
                (int) (textLeft * scale),
                (int) (fbH - (textTop + scH) * scale),
                (int) (scW * scale),
                (int) (scH * scale)
        );

        int drawY = textTop + TEXT_PAD_TOP - (int) scrollOffset;

        for (int i = 0; i < wrappedLines.size(); i++) {
            int y = drawY + i * LINE_HEIGHT;

            if (y + LINE_HEIGHT < textTop || y > textBottom) {
                continue;
            }

            String line = wrappedLines.get(i);
            if (line.isEmpty()) {
                continue;
            }

            if (line.matches("^[~%#=\\-]{3,}$")) {
                int divY = y + LINE_HEIGHT / 2;
                fill(textLeft, divY, textRight, divY + 1, 0x55AAAAAA);
                continue;
            }

            this.font.drawString(line, (float) textLeft, (float) y, getLineColor(line));
        }

        GlStateManager.disableScissorTest();

        if (maxScroll > 0) {
            int sbX = popupX + WSPEED - 6;
            int sbH = textBottom - textTop;

            fill(sbX, textTop, sbX + 2, textTop + sbH, 0x33FFFFFF);

            float progress = scrollOffset / (float) maxScroll;
            int thumbH = Math.max(20, sbH * sbH / (sbH + wrappedLines.size() * LINE_HEIGHT));
            int thumbY = textTop + (int) ((float) (sbH - thumbH) * progress);
            fill(sbX, thumbY, sbX + 2, thumbY + thumbH, 0xAAFFFFFF);
        }

        fillGradient(textLeft, textTop, textRight, textTop + 4, 0xF0101010, 0x00101010);
        fillGradient(textLeft, textBottom - 4, textRight, textBottom, 0x00101010, 0xF0101010);

        super.render(mouseX, mouseY, partialTicks);
    }

    private int getLineColor(String line) {
        String t = line.trim();

        //color it for cool looking
        if (t.startsWith("Project Name:") || t.startsWith("Project Author:")
                || t.startsWith("Project URL:") || t.startsWith("Used For:")
                || t.startsWith("Copyright")) {
            return 0xFFAA00;
        }

        if (t.endsWith(":") && t.indexOf(' ') > 0 && t.length() < 50) {
            return 0x55FF55;
        }

        if (t.startsWith("http://") || t.startsWith("https://")) {
            return 0x55AAFF;
        }

        if (t.matches("^[A-Za-z0-9 /]{3,50}$") && !t.contains(":") && !t.contains(".") && t.length() > 5) {
            if (t.equals(t.toUpperCase()) || Character.isUpperCase(t.charAt(0))) {
                return 0xFFFFFF;
            }
        }

        return 0xAAAAAA;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (maxScroll > 0 && mouseX >= textLeft && mouseX <= textRight
                && mouseY >= textTop && mouseY <= textBottom) {
            scrollOffset = MathHelper.clamp(
                    scrollOffset - (float) scrollDelta * 18.0F,
                    0.0F, (float) maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll > 0) {
            int sbX = popupX + WSPEED - 6;
            if (mouseX >= sbX - 3 && mouseX <= sbX + 5
                    && mouseY >= textTop && mouseY <= textBottom) {
                float progress = (float) ((mouseY - textTop) / (double) (textBottom - textTop));
                scrollOffset = MathHelper.clamp(progress * (float) maxScroll, 0.0F, (float) maxScroll);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && maxScroll > 0) {
            int sbX = popupX + WSPEED - 6;
            if (mouseX >= sbX - 10 && mouseX <= sbX + 10
                    && mouseY >= textTop && mouseY <= textBottom) {
                float progress = (float) ((mouseY - textTop) / (double) (textBottom - textTop));
                scrollOffset = MathHelper.clamp(progress * (float) maxScroll, 0.0F, (float) maxScroll);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
