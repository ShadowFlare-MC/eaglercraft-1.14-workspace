package net.deltaclient;

import com.mojang.blaze3d.platform.GlStateManager;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

public class EditProfileScreen extends Screen {

    private final Screen parentScreen;
    private final int dropdownWidth = 180;
    private final int dropdownHeight = 20;
    private final int itemHeight = 14;
    private final int maxVisibleItems = 5;
    private final int capeDropdownWidth = 180;
    private final int capeDropdownHeight = 20;
    private final int capeItemHeight = 14;
    private final int capeMaxVisibleItems = 5;
    private TextFieldWidget nameField;
    private int currentPresetId;
    private int currentCapeId;
    private float rotationYaw = 0.0F;
    private boolean isDropdownOpen = false;
    private Button dropdownToggleBtn;
    private int dropdownX;
    private int dropdownY;
    private int scrollOffset = 0;
    private boolean isCapeDropdownOpen = false;
    private Button capeDropdownToggleBtn;
    private int capeDropdownX;
    private int capeDropdownY;
    private int capeScrollOffset = 0;

    public EditProfileScreen(Screen parentScreen) {
        super(new TranslationTextComponent("Edit Profile"));
        this.parentScreen = parentScreen;
        this.currentPresetId = EaglerProfile.presetSkinId;
        this.currentCapeId = EaglerProfile.presetCapeId;
    }

    @Override
    protected void init() {
        if (this.mc == null || this.mc.keyboardListener == null) {
            return;
        }
        this.mc.keyboardListener.enableRepeatEvents(true);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameField = new TextFieldWidget(this.font, centerX - 90, centerY - 55, 180, 20, I18n.format("selectWorld.enterName"));
        this.nameField.setMaxStringLength(16);
        this.nameField.setText(EaglerProfile.username);
        this.nameField.setFocused2(true);
        this.children.add(this.nameField);

        this.dropdownX = centerX - 90;
        this.dropdownY = centerY - 10;

        this.dropdownToggleBtn = new Button(this.dropdownX, this.dropdownY, this.dropdownWidth, this.dropdownHeight, getSkinName(this.currentPresetId), (button) -> {
            this.isDropdownOpen = !this.isDropdownOpen;
            if (this.isDropdownOpen) {
                this.scrollToVisible();
            }
        });
        this.addButton(this.dropdownToggleBtn);

        this.capeDropdownX = centerX - 90;
        this.capeDropdownY = centerY + 35;

        this.capeDropdownToggleBtn = new Button(this.capeDropdownX, this.capeDropdownY, this.capeDropdownWidth, this.capeDropdownHeight, getCapeName(this.currentCapeId), (button) -> {
            this.isCapeDropdownOpen = !this.isCapeDropdownOpen;
            if (this.isCapeDropdownOpen) {
                this.capeScrollToVisible();
            }
        });
        this.addButton(this.capeDropdownToggleBtn);

        this.addButton(new Button(centerX - 100, this.height - 40, 95, 20, I18n.format("gui.done"), (button) -> {
            this.saveAndClose();
        }));

        this.addButton(new Button(centerX + 5, this.height - 40, 95, 20, I18n.format("gui.cancel"), (button) -> {
            this.mc.displayGuiScreen(this.parentScreen);
        }));
    }

    private String getSkinName(int id) {
        return net.lax1dude.eaglercraft.profile.DefaultSkins.getSkinFromId(id).name;
    }

    private String getCapeName(int id) {
        return net.lax1dude.eaglercraft.profile.DefaultCapes.getCapeFromId(id).name;
    }

    private void scrollToVisible() {

        if (this.currentPresetId < this.scrollOffset) {
            this.scrollOffset = this.currentPresetId;
        } else if (this.currentPresetId >= this.scrollOffset + this.maxVisibleItems) {
            this.scrollOffset = this.currentPresetId - this.maxVisibleItems + 1;
        }
    }

    private void capeScrollToVisible() {
        if (this.currentCapeId < this.capeScrollOffset) {
            this.capeScrollOffset = this.currentCapeId;
        } else if (this.currentCapeId >= this.capeScrollOffset + this.capeMaxVisibleItems) {
            this.capeScrollOffset = this.currentCapeId - this.capeMaxVisibleItems + 1;
        }
    }

    private void saveAndClose() {
        String trimmedName = this.nameField.getText().trim();
        if (!trimmedName.isEmpty()) {
            EaglerProfile.username = trimmedName;
        }

        EaglerProfile.presetSkinId = this.currentPresetId;
        EaglerProfile.presetCapeId = this.currentCapeId;

        net.lax1dude.eaglercraft.profile.ServerSkinCache.needReloadClientSkin = true;

        this.mc.gameSettings.saveOptions();
        this.mc.displayGuiScreen(this.parentScreen);
    }

    @Override
    public void tick() {
        if (this.nameField != null) {
            this.nameField.tick();
        }
        this.rotationYaw += 1.5F;
    }

    @Override
    public void removed() {
        if (this.mc != null && this.mc.keyboardListener != null) {
            this.mc.keyboardListener.enableRepeatEvents(false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {

        if (this.isDropdownOpen) {
            int totalPresets = net.lax1dude.eaglercraft.profile.DefaultSkins.defaultSkinsMap.length;
            if (mouseX >= this.dropdownX && mouseX <= this.dropdownX + this.dropdownWidth) {

                this.scrollOffset = MathHelper.clamp(this.scrollOffset - (int) amount, 0, Math.max(0, totalPresets - this.maxVisibleItems));
                return true;
            }
        }
        if (this.isCapeDropdownOpen) {
            int totalCapes = net.lax1dude.eaglercraft.profile.DefaultCapes.defaultCapesMap.length;
            if (mouseX >= this.capeDropdownX && mouseX <= this.capeDropdownX + this.capeDropdownWidth) {
                this.capeScrollOffset = MathHelper.clamp(this.capeScrollOffset - (int) amount, 0, Math.max(0, totalCapes - this.capeMaxVisibleItems));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isDropdownOpen && button == 0) {
            int totalPresets = net.lax1dude.eaglercraft.profile.DefaultSkins.defaultSkinsMap.length;
            int listTop = this.dropdownY + this.dropdownHeight;
            int visibleCount = Math.min(totalPresets, this.maxVisibleItems);


            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = this.scrollOffset + i;
                int itemY = listTop + (i * this.itemHeight);

                if (mouseX >= this.dropdownX && mouseX <= this.dropdownX + this.dropdownWidth &&
                        mouseY >= itemY && mouseY <= itemY + this.itemHeight) {

                    this.currentPresetId = actualIndex;
                    EaglerProfile.presetSkinId = actualIndex;
                    this.dropdownToggleBtn.setMessage(getSkinName(actualIndex));
                    this.isDropdownOpen = false;
                    return true;
                }
            }


            int listBottom = listTop + (visibleCount * this.itemHeight);
            if (!(mouseX >= this.dropdownX && mouseX <= this.dropdownX + this.dropdownWidth &&
                    mouseY >= this.dropdownY && mouseY <= listBottom)) {
                this.isDropdownOpen = false;
            }
        }
        if (this.isCapeDropdownOpen && button == 0) {
            int totalCapes = net.lax1dude.eaglercraft.profile.DefaultCapes.defaultCapesMap.length;
            int listTop = this.capeDropdownY + this.capeDropdownHeight;
            int visibleCount = Math.min(totalCapes, this.capeMaxVisibleItems);

            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = this.capeScrollOffset + i;
                int itemY = listTop + (i * this.capeItemHeight);

                if (mouseX >= this.capeDropdownX && mouseX <= this.capeDropdownX + this.capeDropdownWidth &&
                        mouseY >= itemY && mouseY <= itemY + this.capeItemHeight) {

                    this.currentCapeId = actualIndex;
                    EaglerProfile.presetCapeId = actualIndex;
                    this.capeDropdownToggleBtn.setMessage(getCapeName(actualIndex));
                    this.isCapeDropdownOpen = false;
                    return true;
                }
            }

            int listBottom = listTop + (visibleCount * this.capeItemHeight);
            if (!(mouseX >= this.capeDropdownX && mouseX <= this.capeDropdownX + this.capeDropdownWidth &&
                    mouseY >= this.capeDropdownY && mouseY <= listBottom)) {
                this.isCapeDropdownOpen = false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField != null && this.nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameField != null && this.nameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();

        int centerX = this.width / 2;


        this.drawCenteredString(this.font, this.title.getFormattedText(), centerX, 15, 16777215);
        this.drawCenteredString(this.font, "Username", centerX, this.height / 2 - 68, 10526880);
        this.drawCenteredString(this.font, "Select Skin", centerX, this.height / 2 - 23, 10526880);
        this.drawCenteredString(this.font, "Select Cape", centerX, this.height / 2 + 22, 10526880);

        this.nameField.render(mouseX, mouseY, partialTicks);


        GlStateManager.disableDepthTest();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);


        super.render(mouseX, mouseY, partialTicks);


        if (this.isDropdownOpen) {
            int totalPresets = net.lax1dude.eaglercraft.profile.DefaultSkins.defaultSkinsMap.length;
            int listTop = this.dropdownY + this.dropdownHeight;
            int visibleCount = Math.min(totalPresets, this.maxVisibleItems);
            int listBottom = listTop + (visibleCount * this.itemHeight);


            fill(this.dropdownX - 1, listTop, this.dropdownX + this.dropdownWidth + 1, listBottom + 1, -6250336);
            fill(this.dropdownX, listTop, this.dropdownX + this.dropdownWidth, listBottom, -16777216);

            GlStateManager.enableTexture();
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);


            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = this.scrollOffset + i;
                int itemY = listTop + (i * this.itemHeight);

                boolean isHovered = mouseX >= this.dropdownX && mouseX <= this.dropdownX + this.dropdownWidth &&
                        mouseY >= itemY && mouseY <= itemY + this.itemHeight;

                if (isHovered) {
                    fill(this.dropdownX, itemY, this.dropdownX + this.dropdownWidth, itemY + this.itemHeight, -14380314);
                } else if (actualIndex == this.currentPresetId) {
                    fill(this.dropdownX, itemY, this.dropdownX + this.dropdownWidth, itemY + this.itemHeight, -12632257);
                }

                String skinName = getSkinName(actualIndex);
                int textColor = isHovered ? 16777120 : (actualIndex == this.currentPresetId ? 65535 : 14737632);
                this.drawString(this.font, skinName, this.dropdownX + 6, itemY + 3, textColor);
            }


            if (totalPresets > this.maxVisibleItems) {
                int scrollbarWidth = 4;
                int scrollbarX = this.dropdownX + this.dropdownWidth - scrollbarWidth - 1;
                int totalHeight = listBottom - listTop;


                int barHeight = (int) ((float) totalHeight * ((float) this.maxVisibleItems / (float) totalPresets));
                int barY = listTop + (int) ((float) (totalHeight - barHeight) * ((float) this.scrollOffset / (float) (totalPresets - this.maxVisibleItems)));


                fill(scrollbarX, listTop, scrollbarX + scrollbarWidth, listBottom, -13421773);
                fill(scrollbarX, barY, scrollbarX + scrollbarWidth, barY + barHeight, -8355712);
            }

            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }


        if (this.isCapeDropdownOpen) {
            int totalCapes = net.lax1dude.eaglercraft.profile.DefaultCapes.defaultCapesMap.length;
            int listTop = this.capeDropdownY + this.capeDropdownHeight;
            int visibleCount = Math.min(totalCapes, this.capeMaxVisibleItems);
            int listBottom = listTop + (visibleCount * this.capeItemHeight);

            fill(this.capeDropdownX - 1, listTop, this.capeDropdownX + this.capeDropdownWidth + 1, listBottom + 1, -6250336);
            fill(this.capeDropdownX, listTop, this.capeDropdownX + this.capeDropdownWidth, listBottom, -16777216);

            GlStateManager.enableTexture();
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = this.capeScrollOffset + i;
                int itemY = listTop + (i * this.capeItemHeight);

                boolean isHovered = mouseX >= this.capeDropdownX && mouseX <= this.capeDropdownX + this.capeDropdownWidth &&
                        mouseY >= itemY && mouseY <= itemY + this.capeItemHeight;

                if (isHovered) {
                    fill(this.capeDropdownX, itemY, this.capeDropdownX + this.capeDropdownWidth, itemY + this.capeItemHeight, -14380314);
                } else if (actualIndex == this.currentCapeId) {
                    fill(this.capeDropdownX, itemY, this.capeDropdownX + this.capeDropdownWidth, itemY + this.capeItemHeight, -12632257);
                }

                String capeName = getCapeName(actualIndex);
                int textColor = isHovered ? 16777120 : (actualIndex == this.currentCapeId ? 65535 : 14737632);
                this.drawString(this.font, capeName, this.capeDropdownX + 6, itemY + 3, textColor);
            }

            if (totalCapes > this.capeMaxVisibleItems) {
                int scrollbarWidth = 4;
                int scrollbarX = this.capeDropdownX + this.capeDropdownWidth - scrollbarWidth - 1;
                int totalHeight = listBottom - listTop;

                int barHeight = (int) ((float) totalHeight * ((float) this.capeMaxVisibleItems / (float) totalCapes));
                int barY = listTop + (int) ((float) (totalHeight - barHeight) * ((float) this.capeScrollOffset / (float) (totalCapes - this.capeMaxVisibleItems)));

                fill(scrollbarX, listTop, scrollbarX + scrollbarWidth, listBottom, -13421773);
                fill(scrollbarX, barY, scrollbarX + scrollbarWidth, barY + barHeight, -8355712);
            }

            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}