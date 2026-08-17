package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.friends.FriendsOverlayScreen;
import net.deltaclient.network.NetworkHandler;import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSocialRegisterScreen extends Screen {

    public static final RenderSkyboxCube PANORAMA_RESOURCES = new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama"));
    private static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");

    private final Screen loginScreen;
    private final RenderSkybox panorama = new RenderSkybox(PANORAMA_RESOURCES);

    private TextFieldWidget emailField;
    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;

    public GuiSocialRegisterScreen(Screen loginScreen) {
        super(new TranslationTextComponent("socials.registerTitle"));
        this.loginScreen = loginScreen;
    }

    @Override
    protected void init() {
        this.mc.keyboardListener.enableRepeatEvents(true);

        int panelX = this.width / 2 - 100;
        int panelY = this.height / 2 - 100;
        
        emailField = new TextFieldWidget(font, panelX + 10, panelY + 30, 180, 20, I18n.format("socials.email"));
        emailField.setMaxStringLength(64);
        emailField.setEnableBackgroundDrawing(true);
        
        usernameField = new TextFieldWidget(font, panelX + 10, panelY + 70, 180, 20, I18n.format("socials.username"));
        usernameField.setMaxStringLength(16);
        usernameField.setEnableBackgroundDrawing(true);

        passwordField = new TextFieldWidget(font, panelX + 10, panelY + 110, 180, 20, I18n.format("socials.password"));
        passwordField.setMaxStringLength(32);
        passwordField.setEnableBackgroundDrawing(true);
        passwordField.setTextFormatter((str, offset) -> str.replaceAll(".", "*"));

        this.addButton(new Button(panelX + 10, panelY + 140, 180, 20, I18n.format("socials.register"), (btn) -> {
            String e = emailField.getText().trim();
            String u = usernameField.getText().trim();
            String p = passwordField.getText().trim();
            if (!e.isEmpty() && !u.isEmpty() && !p.isEmpty()) {
                NetworkHandler.register(u, e, p);
            }
        }));

        this.addButton(new Button(panelX + 10, panelY + 165, 180, 20, I18n.format("socials.backToLogin"), (btn) -> {
            this.mc.displayGuiScreen(loginScreen);
        }));
    }

    @Override
    public void tick() {
        super.tick();
        emailField.tick();
        usernameField.tick();
        passwordField.tick();
        NetworkHandler.tick();

        if (NetworkHandler.isAuthenticated) {
            this.mc.displayGuiScreen(new FriendsOverlayScreen(loginScreen));
        }
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

        int panelX = this.width / 2 - 100;
        int panelY = this.height / 2 - 100;
        int panelW = 200;
        int panelH = 200;

        fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE111111, 0xEE222222);
        fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF444444);
        fill(panelX, panelY + panelH, panelX + panelW, panelY + panelH + 1, 0xFF444444);
        fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF444444);
        fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, 0xFF444444);

        drawCenteredString(font, I18n.format("socials.registerTitle"), this.width / 2, panelY + 10, 0xFFFFFF);
        
        drawString(font, I18n.format("socials.email"), panelX + 10, panelY + 20, 0xAAAAAA);
        drawString(font, I18n.format("socials.username"), panelX + 10, panelY + 60, 0xAAAAAA);
        drawString(font, I18n.format("socials.password"), panelX + 10, panelY + 100, 0xAAAAAA);

        if (NetworkHandler.isConnecting) {
            drawCenteredString(font, I18n.format("socials.connecting"), this.width / 2, panelY + 185, 0xAAAAAA);
        } else if (NetworkHandler.lastAuthError != null) {
            drawCenteredString(font, "§c" + I18n.format(NetworkHandler.lastAuthError), this.width / 2, panelY + 185, 0xFF5555);
        }

        emailField.render(mx, my, partialTicks);
        usernameField.render(mx, my, partialTicks);
        passwordField.render(mx, my, partialTicks);

        super.render(mx, my, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        emailField.mouseClicked(mx, my, button);
        usernameField.mouseClicked(mx, my, button);
        passwordField.mouseClicked(mx, my, button);
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.mc.displayGuiScreen(loginScreen);
            return true;
        }
        if (emailField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (usernameField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (passwordField.keyPressed(keyCode, scanCode, modifiers)) return true;
        
        if (keyCode == 258) {
            if (emailField.isFocused()) {
                emailField.setFocused2(false);
                usernameField.setFocused2(true);
            } else if (usernameField.isFocused()) {
                usernameField.setFocused2(false);
                passwordField.setFocused2(true);
            } else {
                passwordField.setFocused2(false);
                emailField.setFocused2(true);
            }
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (emailField.charTyped(codePoint, modifiers)) return true;
        if (usernameField.charTyped(codePoint, modifiers)) return true;
        if (passwordField.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void removed() {
        this.mc.keyboardListener.enableRepeatEvents(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
