package net.deltaclient.friends;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.internal.EnumCursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AddFriendWidget {

    private static final ResourceLocation ADD_SPRITE = new ResourceLocation("textures/gui/friends/draft_report.png");
    private static final ResourceLocation SEPARATOR_TEX = new ResourceLocation("textures/gui/friends/list_separator_top.png");

    private final FriendsOverlayScreen screen;
    private final TextFieldWidget textField;
    private final ImageButton addButton;
    private int x;
    private int y;
    private final int width;
    private final int fieldWidth;
    private int height;

    public AddFriendWidget(FriendsOverlayScreen screen, int x, int y, int width) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.fieldWidth = width - 20 - 3 - 16;

        Minecraft mc = Minecraft.getInstance();
        this.textField = new TextFieldWidget(mc.fontRenderer, x + 8, y + 3, fieldWidth, 20, "");
        this.textField.setMaxStringLength(32);
        this.textField.setEnableBackgroundDrawing(true);
        this.textField.setText("");

        this.addButton = new ImageButton(0, 0, 20, 20,
                0, 0, 0, ADD_SPRITE, 15, 15,
                btn -> {
                    String name = getValue();
                    if (!name.isEmpty()) {
                        btn.active = false;
                        NetworkHandler.addFriend(name);
                        textField.setText("");
                    }
                }, "") {
            @Override
            public void renderButton(int mx, int my, float pt) {
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                int i = this.getYImage(this.isHovered());
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, this.alpha);
                GlStateManager.enableBlend();
                GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                this.blit(this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                this.blit(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
                mc.getTextureManager().bindTexture(ADD_SPRITE);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int iconX = this.x + (this.width - 15) / 2;
                int iconY = this.y + (this.height - 15) / 2;
                blit(iconX, iconY, 0.0F, 0.0F, 15, 15, 15, 15);
            }
        };

        recalcHeight();
    }

    private void recalcHeight() {
        this.height = 50;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.textField.x = x + 8;
        this.textField.y = y + 3;
    }

    public int getHeight() {
        return height;
    }

    public String getValue() {
        return textField.getText().trim();
    }

    public void setValue(String value) {
        textField.setText(value);
    }

    public void applySending(boolean isSending) {
        this.addButton.active = !isSending;
        this.textField.setEnabled(!isSending);
        if (isSending) {
            this.textField.setFocused2(false);
        }
    }

    public void tick() {
        textField.tick();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                if (addButton.active && !getValue().isEmpty()) {
                    addButton.onPress();
                }
                return true;
            }
            return textField.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (textField.isFocused()) {
            return textField.charTyped(codePoint, modifiers);
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (textField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return addButton.mouseClicked(mouseX, mouseY, button);
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        FontRenderer font = mc.fontRenderer;

        // Draw the player name label
        String profileName = NetworkHandler.loggedInUsername;
        String label = I18n.format("gui.friends.my_profile_name");
        font.drawStringWithShadow(label, x + 8, y + 26, 0xFFAA0000);
        font.drawStringWithShadow(profileName, x + 8 + font.getStringWidth(label) + 4, y + 26, 0xFFFFFFFF);

        // Draw separator
        mc.getTextureManager().bindTexture(SEPARATOR_TEX);
        screen.blit(x + 8, y + 38, 0, 0, width - 16, 2, 32, 2);

        // Position and render text field + add button
        int addBtnX = x + 8 + fieldWidth + 3;
        int addBtnY = y + 3;

        textField.x = x + 8;
        textField.y = y + 3;
        textField.render(mouseX, mouseY, partialTicks);

        addButton.active = !getValue().isEmpty();
        addButton.x = addBtnX;
        addButton.y = addBtnY;
        addButton.render(mouseX, mouseY, partialTicks);

        if (textField.isHovered()) {
            screen.setCursor(EnumCursorType.TEXT);
        } else if (addButton.isHovered()) {
            screen.setCursor(EnumCursorType.HAND);
        }
    }
}
