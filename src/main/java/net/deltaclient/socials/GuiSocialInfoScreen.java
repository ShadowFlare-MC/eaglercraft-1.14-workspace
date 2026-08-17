package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiSocialInfoScreen extends Screen {

    public static final RenderSkyboxCube PANORAMA_RESOURCES = new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama"));
    private static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");

    private final RenderSkybox panorama = new RenderSkybox(PANORAMA_RESOURCES);

    private final String titleText;
    private final String descriptionText;
    private final Screen nextScreen;

    public GuiSocialInfoScreen(String title, String description, Screen nextScreen) {
        super(new StringTextComponent(title));
        this.titleText = title;
        this.descriptionText = description;
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        int panelX = this.width / 2 - 120;
        int panelY = this.height / 2 - 60;
        
        this.addButton(new Button(this.width / 2 - 50, panelY + 90, 100, 20, "OK", (btn) -> {
            this.mc.displayGuiScreen(nextScreen);
        }));
    }

    @Override
    public void render(int mx, int my, float partialTicks) {
        GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend();
        this.panorama.render(partialTicks, 1.0F);
        this.mc.getTextureManager().bindTexture(PANORAMA_OVERLAY_TEXTURES);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
        GlStateManager.enableAlphaTest();

        fillGradient(0, 0, this.width, this.height, 0xAA000000, 0xDD000000);

        int panelW = 240;
        int panelH = 130;
        int panelX = this.width / 2 - panelW / 2;
        int panelY = this.height / 2 - panelH / 2;

        fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE111111, 0xEE222222);
        fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF444444);
        fill(panelX, panelY + panelH, panelX + panelW, panelY + panelH + 1, 0xFF444444);
        fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF444444);
        fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, 0xFF444444);

        drawCenteredString(font, "§l" + titleText, this.width / 2, panelY + 15, 0xFFFFFF);

        List<String> lines = font.listFormattedStringToWidth(descriptionText, panelW - 20);
        int textY = panelY + 45;
        for (String line : lines) {
            drawCenteredString(font, line, this.width / 2, textY, 0xAAAAAA);
            textY += font.FONT_HEIGHT + 2;
        }

        super.render(mx, my, partialTicks);
    }
}
