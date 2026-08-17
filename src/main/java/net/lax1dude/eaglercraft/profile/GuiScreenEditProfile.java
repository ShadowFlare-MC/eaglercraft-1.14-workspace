package net.lax1dude.eaglercraft.profile;

import net.deltaclient.network.NetworkHandler;import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.internal.FileChooserResult;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.ImageData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;

import static net.lax1dude.eaglercraft.opengl.RealOpenGLEnums.*;

public class GuiScreenEditProfile extends Screen {

    private static final ResourceLocation eaglerGui = new ResourceLocation("eagler:gui/eagler_gui.png");
    public static ArrayList<CustomSkin> customSkins = new ArrayList<>();
    public static ArrayList<CustomCape> customCapes = new ArrayList<>();
    public static int customSkinId = -1;
    public static int customCapeId = -1;
    private final Screen parent;
    protected int selectedSlot = 0;
    protected String screenTitle = "Edit Profile";
    private TextFieldWidget usernameField;
    private boolean dropDownOpen = false;
    private String[] dropDownOptions;
    private int slotsVisible = 0;
    private int scrollPos = -1;
    private int skinsHeight = 0;
    private boolean dragging = false;
    private int mousex = 0;
    private int mousey = 0;
    private boolean newSkinWaitSteveOrAlex = false;

    public GuiScreenEditProfile(Screen parent) {
        super(new TranslationTextComponent("editProfile.title"));
        this.parent = parent;
    }

    public static int addCustomSkin(String name, byte[] rawSkin) {
        CustomSkin skin = new CustomSkin(name, rawSkin);
        customSkins.add(skin);
        return customSkins.size() - 1;
    }

    public static int addCustomCape(String name, byte[] rawCape) {
        CustomCape cape = new CustomCape(name, rawCape);
        customCapes.add(cape);
        return customCapes.size() - 1;
    }

    public static void clearCustomSkins() {
        customSkins.clear();
        customSkinId = -1;
        EaglerProfile.customSkinId = -1;
        if (EaglerProfile.presetSkinId == -1) {
            EaglerProfile.presetSkinId = 0;
        }
    }

    public static void clearCustomCapes() {
        customCapes.clear();
        customCapeId = -1;
    }

    @Override
    protected void init() {
        this.mc.keyboardListener.enableRepeatEvents(true);
        screenTitle = I18n.format("editProfile.title");
        usernameField = new TextFieldWidget(font, width / 2 - 20 + 1, height / 6 + 24 + 1, 138, 20, I18n.format("editProfile.username"));
        usernameField.setFocused2(true);
        usernameField.setText(EaglerProfile.username);
        usernameField.setMaxStringLength(16);
        selectedSlot = EaglerProfile.presetSkinId == -1 ? customSkinId : (EaglerProfile.presetSkinId + customSkins.size());
        addButton(new Button(width / 2 - 100, height / 6 + 168, 200, 20, I18n.format("gui.done"), (btn) -> {
            safeProfile();
            this.mc.gameSettings.saveOptions();
            NetworkHandler.sendSkinUpdate();
            this.mc.displayGuiScreen(parent);
        }));
        addButton(new Button(width / 2 - 21, height / 6 + 110, 71, 20, I18n.format("editProfile.addSkin"), (btn) -> {
            EagRuntime.displayFileChooser("image/png", "png");
        }));
        addButton(new Button(width / 2 - 21 + 71, height / 6 + 110, 72, 20, I18n.format("editProfile.clearSkin"), (btn) -> {
            clearCustomSkins();
            safeProfile();
            this.mc.gameSettings.saveOptions();
            updateOptions();
            selectedSlot = 0;
        }));
        updateOptions();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void updateOptions() {
        DefaultSkins[] arr = DefaultSkins.defaultSkinsMap;
        if (!EagRuntime.getConfiguration().isAllowFNAWSkins()) {
            DefaultSkins[] arrNoFNAW = new DefaultSkins[arr.length - 5];
            System.arraycopy(arr, 0, arrNoFNAW, 0, arrNoFNAW.length);
            arr = arrNoFNAW;
        }
        int numCustom = customSkins.size();
        String[] n = new String[numCustom + arr.length];
        for (int i = 0; i < numCustom; ++i) {
            n[i] = customSkins.get(i).name;
        }
        int numDefault = arr.length;
        for (int j = 0; j < numDefault; ++j) {
            n[numCustom + j] = arr[j].name;
        }
        dropDownOptions = n;
    }

    @Override
    public void render(int mx, int my, float partialTicks) {
        renderBackground();
        drawCenteredString(font, screenTitle, width / 2, 15, 16777215);
        drawString(font, I18n.format("editProfile.username"), width / 2 - 20, height / 6 + 8, 10526880);
        drawString(font, I18n.format("editProfile.playerSkin"), width / 2 - 20, height / 6 + 66, 10526880);

        mousex = mx;
        mousey = my;

        int skinX = width / 2 - 120;
        int skinY = height / 6 + 8;
        int skinWidth = 80;
        int skinHeight = 130;

        fill(skinX, skinY, skinX + skinWidth, skinY + skinHeight, 0xFFA0A0A0);
        fill(skinX + 1, skinY + 1, skinX + skinWidth - 1, skinY + skinHeight - 1, 0xFF000015);

        GlStateManager.pushMatrix();
        GlStateManager.translate(skinX + 2, skinY - 9, 0.0f);
        GlStateManager.scale(0.75f, 0.75f, 0.75f);

        if (selectedSlot > dropDownOptions.length - 1 || selectedSlot < 0) {
            selectedSlot = 0;
        }

        int numberOfCustomSkins = customSkins.size();
        int skid = selectedSlot - numberOfCustomSkins;
        SkinModel selectedSkinModel = skid < 0 ? customSkins.get(selectedSlot).model : DefaultSkins.getSkinFromId(skid).model;
        if (selectedSkinModel == SkinModel.STEVE || selectedSkinModel == SkinModel.ALEX || (selectedSkinModel.highPoly != null && !this.mc.gameSettings.enableFNAWSkins)) {
            String capesText = I18n.format("editProfile.capes");
            int color = 10526880;
            if (mx > skinX - 10 && my > skinY - 16 && mx < skinX + (font.getStringWidth(capesText) * 0.75f) + 10 && my < skinY + 7) {
                color = 0xFFCCCC44;
            }
            this.drawString(this.font, TextFormatting.UNDERLINE + capesText, 0, 0, color);
        }

        GlStateManager.popMatrix();

        usernameField.render(mx, my, partialTicks);
        if (!dropDownOpen && !newSkinWaitSteveOrAlex) {
            super.render(mx, my, partialTicks);
        } else {
            super.render(0, 0, partialTicks);
        }

        if (selectedSkinModel.highPoly != null) {
            drawCenteredString(font, I18n.format(this.mc.gameSettings.enableFNAWSkins ? "editProfile.disableFNAW" : "editProfile.enableFNAW"), width / 2, height / 6 + 150, 10526880);
        }

        skinX = width / 2 - 20;
        skinY = height / 6 + 82;
        skinWidth = 140;
        skinHeight = 22;

        fill(skinX, skinY, skinX + skinWidth, skinY + skinHeight, -6250336);
        fill(skinX + 1, skinY + 1, skinX + skinWidth - 21, skinY + skinHeight - 1, -16777216);
        fill(skinX + skinWidth - 20, skinY + 1, skinX + skinWidth - 1, skinY + skinHeight - 1, -16777216);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        mc.getTextureManager().bindTexture(eaglerGui);
        blit(skinX + skinWidth - 18, skinY + 3, 0, 0, 16, 16);

        drawString(font, dropDownOptions[selectedSlot], skinX + 5, skinY + 7, 14737632);

        skinX = width / 2 - 20;
        skinY = height / 6 + 103;
        skinWidth = 140;
        skinHeight = (height - skinY - 10);
        slotsVisible = (skinHeight / 10);
        if (slotsVisible > dropDownOptions.length) slotsVisible = dropDownOptions.length;
        skinHeight = slotsVisible * 10 + 7;
        skinsHeight = skinHeight;
        if (scrollPos == -1) {
            scrollPos = selectedSlot - 2;
        }
        if (scrollPos > (dropDownOptions.length - slotsVisible)) {
            scrollPos = (dropDownOptions.length - slotsVisible);
        }
        if (scrollPos < 0) {
            scrollPos = 0;
        }
        if (dropDownOpen) {
            fill(skinX, skinY, skinX + skinWidth, skinY + skinHeight, -6250336);
            fill(skinX + 1, skinY + 1, skinX + skinWidth - 1, skinY + skinHeight - 1, -16777216);
            for (int i = 0; i < slotsVisible; i++) {
                if (i + scrollPos < dropDownOptions.length) {
                    if (selectedSlot == i + scrollPos) {
                        fill(skinX + 1, skinY + i * 10 + 4, skinX + skinWidth - 1, skinY + i * 10 + 14, 0x77ffffff);
                    } else if (mx >= skinX && mx < (skinX + skinWidth - 10) && my >= (skinY + i * 10 + 5) && my < (skinY + i * 10 + 15)) {
                        fill(skinX + 1, skinY + i * 10 + 4, skinX + skinWidth - 1, skinY + i * 10 + 14, 0x55ffffff);
                    }
                    drawString(font, dropDownOptions[i + scrollPos], skinX + 5, skinY + 5 + i * 10, 14737632);
                }
            }
            int scrollerSize = skinHeight * slotsVisible / dropDownOptions.length;
            int scrollerPos = skinHeight * scrollPos / dropDownOptions.length;
            fill(skinX + skinWidth - 4, skinY + scrollerPos + 1, skinX + skinWidth - 1, skinY + scrollerPos + scrollerSize, 0xff888888);
        }

        if (!EagRuntime.getConfiguration().isDemo()) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75f, 0.75f, 0.75f);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            String text = I18n.format("editProfile.importExport");

            int w = font.getStringWidth(text);
            boolean hover = mx > 1 && my > 1 && mx < (w * 3 / 4) + 7 && my < 12;

            drawString(font, TextFormatting.UNDERLINE + text, 5, 5, hover ? 0xFFEEEE22 : 0xFFCCCCCC);

            GlStateManager.popMatrix();
        }

        int xx = width / 2 - 80;
        int yy = height / 6 + 130;

        if (newSkinWaitSteveOrAlex && selectedSlot < numberOfCustomSkins) {
            skinWidth = 70;
            skinHeight = 120;

            CustomSkin newSkin = customSkins.get(selectedSlot);

            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);

            skinX = width / 2 - 90;
            skinY = height / 4;
            xx = skinX + 35;
            yy = skinY + 117;

            boolean mouseOver = mx >= skinX && my >= skinY && mx < skinX + skinWidth && my < skinY + skinHeight;
            int cc = mouseOver ? 0xFFDDDD99 : 0xFF555555;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            fill(0, 0, width, height, 0xbb000000);
            fill(skinX, skinY, skinX + skinWidth, skinY + skinHeight, 0xbb000000);
            GlStateManager.disableBlend();

            fill(skinX, skinY, skinX + 1, skinY + skinHeight, cc);
            fill(skinX, skinY, skinX + skinWidth, skinY + 1, cc);
            fill(skinX + skinWidth - 1, skinY, skinX + skinWidth, skinY + skinHeight, cc);
            fill(skinX, skinY + skinHeight - 1, skinX + skinWidth, skinY + skinHeight, cc);

            if (mouseOver) {
                drawCenteredString(font, "Steve", skinX + skinWidth / 2, skinY + skinHeight + 6, cc);
            }

            SkinPreviewRenderer.renderPreview(xx, yy, mx, my, false, SkinModel.STEVE, newSkin.getResource(),
                    EaglerProfile.getActiveCapeResourceLocation());

            skinX = width / 2 + 20;
            skinY = height / 4;
            xx = skinX + 35;
            yy = skinY + 117;

            mouseOver = mx >= skinX && my >= skinY && mx < skinX + skinWidth && my < skinY + skinHeight;
            cc = mouseOver ? 0xFFDDDD99 : 0xFF555555;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            fill(skinX, skinY, skinX + skinWidth, skinY + skinHeight, 0xbb000000);
            GlStateManager.disableBlend();

            fill(skinX, skinY, skinX + 1, skinY + skinHeight, cc);
            fill(skinX, skinY, skinX + skinWidth, skinY + 1, cc);
            fill(skinX + skinWidth - 1, skinY, skinX + skinWidth, skinY + skinHeight, cc);
            fill(skinX, skinY + skinHeight - 1, skinX + skinWidth, skinY + skinHeight, cc);

            if (mouseOver) {
                drawCenteredString(font, "Alex", skinX + skinWidth / 2, skinY + skinHeight + 8, cc);
            }

            SkinPreviewRenderer.renderPreview(xx, yy, mx, my, false, SkinModel.ALEX, newSkin.getResource(),
                    EaglerProfile.getActiveCapeResourceLocation());
        } else {
            skinX = this.width / 2 - 120;
            skinY = this.height / 6 + 8;
            skinWidth = 80;
            skinHeight = 130;

            ResourceLocation texture;
            if (skid < 0) {
                texture = customSkins.get(selectedSlot).getResource();
            } else {
                texture = DefaultSkins.getSkinFromId(skid).location;
            }

            SkinPreviewRenderer.renderPreview(xx, yy, newSkinWaitSteveOrAlex ? width / 2 : mx,
                    newSkinWaitSteveOrAlex ? height / 2 : my, false, selectedSkinModel, texture,
                    EaglerProfile.getActiveCapeResourceLocation());
        }

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (dropDownOpen) {
            if (amount < 0) {
                scrollPos += 3;
            }
            if (amount > 0) {
                scrollPos -= 3;
                if (scrollPos < 0) {
                    scrollPos = 0;
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void tick() {
        if (usernameField != null) {
            usernameField.tick();
        }
        if (EagRuntime.fileChooserHasResult()) {
            FileChooserResult result = EagRuntime.getFileChooserResult();
            if (result != null) {
                ImageData loadedSkin = ImageData.loadImageFile(result.fileData, ImageData.getMimeFromType(result.fileName));
                if (loadedSkin != null) {
                    boolean isLegacy = loadedSkin.width == 64 && loadedSkin.height == 32;
                    boolean isModern = loadedSkin.width == 64 && loadedSkin.height == 64;
                    if (isLegacy) {
                        ImageData newSkin = new ImageData(64, 64, true);
                        SkinConverter.convert64x32to64x64(loadedSkin, newSkin);
                        loadedSkin = newSkin;
                        isModern = true;
                    }
                    if (isModern) {
                        byte[] rawSkin = new byte[16384];
                        for (int i = 0, j, k; i < 4096; ++i) {
                            j = i << 2;
                            k = loadedSkin.pixels[i];
                            rawSkin[j] = (byte) (k >>> 24);
                            rawSkin[j + 1] = (byte) (k >>> 16);
                            rawSkin[j + 2] = (byte) (k >>> 8);
                            rawSkin[j + 3] = (byte) (k & 0xFF);
                        }
                        for (int y = 20; y < 32; ++y) {
                            for (int x = 16; x < 40; ++x) {
                                rawSkin[(y << 8) | (x << 2)] = (byte) 0xff;
                            }
                        }
                        int k;
                        if ((k = addCustomSkin(result.fileName, rawSkin)) != -1) {
                            selectedSlot = k;
                            newSkinWaitSteveOrAlex = true;
                            updateOptions();
                            safeProfile();
                            this.mc.gameSettings.saveOptions();
                        }
                    } else {
                        EagRuntime.showPopup("The selected image '" + result.fileName + "' is not the right size!\nEaglercraft only supports 64x32 or 64x64 skins");
                    }
                } else {
                    EagRuntime.showPopup("The selected file '" + result.fileName + "' is not a supported format!");
                }
            }
        }
        if (dropDownOpen) {
            if (Mouse.isButtonDown(0)) {
                int skinX = width / 2 - 20;
                int skinY = height / 6 + 103;
                int skinWidth = 140;
                if (mousex >= (skinX + skinWidth - 10) && mousex < (skinX + skinWidth) && mousey >= skinY && mousey < (skinY + skinsHeight)) {
                    dragging = true;
                }
                if (dragging) {
                    int scrollerSize = skinsHeight * slotsVisible / dropDownOptions.length;
                    scrollPos = (mousey - skinY - (scrollerSize / 2)) * dropDownOptions.length / skinsHeight;
                }
            } else {
                dragging = false;
            }
        } else {
            dragging = false;
        }
    }

    @Override
    public void removed() {
        if (this.mc != null) {
            this.mc.keyboardListener.enableRepeatEvents(false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (usernameField != null && usernameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int k) {
        if (usernameField != null && usernameField.charTyped(c, k)) {
            String text = usernameField.getText();
            if (text.length() > 16) text = text.substring(0, 16);
            text = text.replaceAll("[^A-Za-z0-9]", "_");
            usernameField.setText(text);
            return true;
        }
        if (k == 200 && selectedSlot > 0) {
            --selectedSlot;
            scrollPos = selectedSlot - 2;
            return true;
        }
        if (k == 208 && selectedSlot < (dropDownOptions.length - 1)) {
            ++selectedSlot;
            scrollPos = selectedSlot - 2;
            return true;
        }
        return super.charTyped(c, k);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (usernameField != null) {
            usernameField.mouseClicked(mx, my, button);
        }
        if (button == 0) {
            if (!EagRuntime.getConfiguration().isDemo()) {
                int w = font.getStringWidth(I18n.format("editProfile.importExport"));
                if (mx > 1 && my > 1 && mx < (w * 3 / 4) + 7 && my < 12) {
                    safeProfile();
                    this.mc.gameSettings.saveOptions();
                    return true;
                }
            }

            int skinX, skinY;
            int skid = selectedSlot - customSkins.size();
            SkinModel selectedSkinModel = skid < 0 ? customSkins.get(selectedSlot).model : DefaultSkins.getSkinFromId(skid).model;
            if (selectedSkinModel == SkinModel.STEVE || selectedSkinModel == SkinModel.ALEX || (selectedSkinModel.highPoly != null && !this.mc.gameSettings.enableFNAWSkins)) {
                skinX = this.width / 2 - 120;
                skinY = this.height / 6 + 8;
                String capesText = I18n.format("editProfile.capes");
                if (mx > skinX - 10 && my > skinY - 16 && mx < skinX + (font.getStringWidth(capesText) * 0.75f) + 10 && my < skinY + 7) {
                    safeProfile();
                    this.mc.displayGuiScreen(new GuiScreenEditCape(this));
                    return true;
                }
            }

            if (newSkinWaitSteveOrAlex) {
                skinX = width / 2 - 90;
                skinY = height / 4;
                int skinWidth = 70;
                int skinHeight = 120;
                if (mx >= skinX && my >= skinY && mx < skinX + skinWidth && my < skinY + skinHeight) {
                    if (selectedSlot < customSkins.size()) {
                        newSkinWaitSteveOrAlex = false;
                        customSkins.get(selectedSlot).model = SkinModel.STEVE;
                        safeProfile();
                    }
                    return true;
                }
                skinX = width / 2 + 20;
                skinY = height / 4;
                if (mx >= skinX && my >= skinY && mx < skinX + skinWidth && my < skinY + skinHeight) {
                    if (selectedSlot < customSkins.size()) {
                        customSkins.get(selectedSlot).model = SkinModel.ALEX;
                        newSkinWaitSteveOrAlex = false;
                        safeProfile();
                    }
                    return true;
                }
                return true;
            } else if (selectedSlot < customSkins.size()) {
                skinX = width / 2 - 120;
                skinY = height / 6 + 18;
                int skinWidth = 80;
                int skinHeight = 120;
                if (mx >= skinX && my >= skinY && mx < skinX + skinWidth && my < skinY + skinHeight) {
                    if (selectedSlot < customSkins.size()) {
                        newSkinWaitSteveOrAlex = true;
                        return true;
                    }
                }
            }
            skinX = width / 2 + 140 - 40;
            skinY = height / 6 + 82;

            if (mx >= skinX && mx < (skinX + 20) && my >= skinY && my < (skinY + 22)) {
                dropDownOpen = !dropDownOpen;
                return true;
            }

            skinX = width / 2 - 20;
            skinY = height / 6 + 82;
            int skinWidth = 140;

            if (!(mx >= skinX && mx < (skinX + skinWidth) && my >= skinY && my < (skinY + skinsHeight + 22))) {
                dragging = false;
                if (dropDownOpen) {
                    dropDownOpen = false;
                    return true;
                }
            } else if (dropDownOpen && !dragging) {
                skinY += 21;
                for (int i = 0; i < slotsVisible; i++) {
                    if (i + scrollPos < dropDownOptions.length) {
                        if (mx >= skinX && mx < (skinX + skinWidth - 10) && my >= (skinY + i * 10 + 5) && my < (skinY + i * 10 + 15)) {
                            selectedSlot = i + scrollPos;
                            dropDownOpen = false;
                            dragging = false;
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    protected void safeProfile() {
        int customLen = customSkins.size();
        if (selectedSlot < customLen) {
            EaglerProfile.presetSkinId = -1;
            EaglerProfile.customSkinId = customSkinId = selectedSlot;
        } else {
            EaglerProfile.presetSkinId = selectedSlot - customLen;
            EaglerProfile.customSkinId = customSkinId = -1;
        }
        ServerSkinCache.needReloadClientSkin = true;
        String name = usernameField.getText().trim();
        while (name.length() < 3) {
            name = name + "_";
        }
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        EaglerProfile.username = name;
    }

    public static class CustomSkin {
        public String name;
        public byte[] data;
        public SkinModel model;
        private ResourceLocation resource;

        public CustomSkin(String name, byte[] data) {
            this.name = name;
            this.data = data;
            this.model = SkinModel.STEVE;
        }

        public ResourceLocation getResource() {
            if (resource == null) {
                resource = new ResourceLocation("eagler:skins/custom/" + Integer.toHexString(System.identityHashCode(this)));
                Minecraft.getInstance().getTextureManager().loadTexture(resource, new EaglerSkinTexture(data, 64, 64));
            }
            return resource;
        }
    }

    public static class CustomCape {
        public String name;
        public byte[] data;
        private ResourceLocation resource;

        public CustomCape(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public ResourceLocation getResource() {
            if (resource == null) {
                resource = new ResourceLocation("eagler:capes/custom/" + Integer.toHexString(System.identityHashCode(this)));
                byte[] dataRGBA = new byte[4096];
                SkinConverter.convertCape23x17RGBto32x32RGBA(data, dataRGBA);
                Minecraft.getInstance().getTextureManager().loadTexture(resource, new EaglerSkinTexture(dataRGBA, 32, 32));
            }
            return resource;
        }
    }
}
