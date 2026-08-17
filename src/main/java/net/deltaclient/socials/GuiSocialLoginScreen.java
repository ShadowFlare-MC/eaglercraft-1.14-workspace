package net.deltaclient.socials;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.lax1dude.eaglercraft.EagRuntime;
import java.nio.charset.StandardCharsets;

@OnlyIn(Dist.CLIENT)
public class GuiSocialLoginScreen extends Screen {

    public static final RenderSkyboxCube PANORAMA_RESOURCES = new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama"));
    private static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");

    private final Screen parentScreen;
    private final RenderSkybox panorama = new RenderSkybox(PANORAMA_RESOURCES);

    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;
    private CheckboxButton rememberMeCheckbox;


    public GuiSocialLoginScreen(Screen parent) {
        super(new TranslationTextComponent("socials.loginTitle"));
        this.parentScreen = parent;
        NetworkHandler.connect();
    }

    @Override
    protected void init() {
        this.mc.keyboardListener.enableRepeatEvents(true);

        int panelX = this.width / 2 - 100;
        int panelY = this.height / 2 - 80;
        
        usernameField = new TextFieldWidget(font, panelX + 10, panelY + 30, 180, 20, I18n.format("socials.username"));
        usernameField.setMaxStringLength(16);
        usernameField.setEnableBackgroundDrawing(true);

        passwordField = new TextFieldWidget(font, panelX + 10, panelY + 70, 180, 20, I18n.format("socials.password"));
        passwordField.setMaxStringLength(32);
        passwordField.setEnableBackgroundDrawing(true);

        byte[] savedData = EagRuntime.getStorage("socials_remember_me");
        boolean isRemembered = false;
        String savedUser = "";
        if (savedData != null) {
            String str = new String(savedData, StandardCharsets.UTF_8);
            if (str.contains(":")) {
                String[] parts = str.split(":", 2);
                savedUser = parts[0];
                usernameField.setText(savedUser);
                try {
                    String p = new String(java.util.Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    passwordField.setText(p);
                    isRemembered = true;
                } catch (Exception ex) {
                    passwordField.setText("");
                }
            }
        }
        
        final String finalSavedUser = savedUser;
        final boolean finalIsRemembered = isRemembered;
        usernameField.func_212954_a(text -> {
            if (finalIsRemembered && !text.equals(finalSavedUser)) {
                EagRuntime.setStorage("socials_remember_me", null);
                passwordField.setText("");
            }
        });

        passwordField.setTextFormatter((str, offset) -> str.replaceAll(".", "*"));

        rememberMeCheckbox = new CheckboxButton(panelX + 10, panelY + 95, 20, 20, "Remember Me", isRemembered);
        this.addButton(rememberMeCheckbox);

        this.addButton(new Button(panelX + 10, panelY + 120, 180, 20, I18n.format("socials.login"), (btn) -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText().trim();
            if (!u.isEmpty() && !p.isEmpty()) {
                if (rememberMeCheckbox.func_212942_a()) {
                    String combo = u + ":" + java.util.Base64.getEncoder().encodeToString(p.getBytes(StandardCharsets.UTF_8));
                    EagRuntime.setStorage("socials_remember_me", combo.getBytes(StandardCharsets.UTF_8));
                } else {
                    EagRuntime.setStorage("socials_remember_me", null);
                }
                
                NetworkHandler.login(u, p, false);
            }
        }));

        this.addButton(new Button(panelX + 10, panelY + 145, 180, 20, I18n.format("socials.noAccount"), (btn) -> {
            this.mc.displayGuiScreen(new GuiSocialRegisterScreen(parentScreen));
        }));

        this.addButton(new Button(this.width / 2 - 50, this.height - 40, 100, 20, I18n.format("gui.cancel"), (btn) -> {
            this.mc.displayGuiScreen(parentScreen);
        }));
    }

    @Override
    public void tick() {
        super.tick();
        usernameField.tick();
        passwordField.tick();
        NetworkHandler.tick();

        if (NetworkHandler.isAuthenticated) {
            this.mc.displayGuiScreen(new net.deltaclient.friends.FriendsOverlayScreen(parentScreen));
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
        int panelY = this.height / 2 - 90;
        int panelW = 200;
        int panelH = 180;

        fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE111111, 0xEE222222);
        fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF444444);
        fill(panelX, panelY + panelH, panelX + panelW, panelY + panelH + 1, 0xFF444444);
        fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF444444);
        fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, 0xFF444444);

        drawCenteredString(font, I18n.format("socials.loginTitle"), this.width / 2, panelY + 10, 0xFFFFFF);
        
        drawString(font, I18n.format("socials.username"), panelX + 10, panelY + 30, 0xAAAAAA);
        drawString(font, I18n.format("socials.password"), panelX + 10, panelY + 70, 0xAAAAAA);

        if (NetworkHandler.isConnecting) {
            drawCenteredString(font, I18n.format("socials.connecting"), this.width / 2, panelY + 165, 0xAAAAAA);
        } else if (NetworkHandler.lastAuthError != null) {
            drawCenteredString(font, "§c" + I18n.format(NetworkHandler.lastAuthError), this.width / 2, panelY + 165, 0xFF5555);
        }

        usernameField.render(mx, my, partialTicks);
        passwordField.render(mx, my, partialTicks);

        super.render(mx, my, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        usernameField.mouseClicked(mx, my, button);
        passwordField.mouseClicked(mx, my, button);
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.mc.displayGuiScreen(parentScreen);
            return true;
        }
        if (usernameField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (passwordField.keyPressed(keyCode, scanCode, modifiers)) return true;
        
        if (keyCode == 258) {
            if (usernameField.isFocused()) {
                usernameField.setFocused2(false);
                passwordField.setFocused2(true);
            } else {
                passwordField.setFocused2(false);
                usernameField.setFocused2(true);
            }
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
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

    public static void handleToken(String token) {
        // Token logic is disabled for Remember Me; we save Base64 password instead.
    }
}
