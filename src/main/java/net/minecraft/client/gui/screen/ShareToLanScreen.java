package net.minecraft.client.gui.screen;

import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.internal.PlatformWebRTC;
import net.lax1dude.eaglercraft.sp.gui.GuiNetworkSettingsButton;
import net.lax1dude.eaglercraft.sp.lan.LANServerController;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShareToLanScreen extends Screen {

    private final Screen lastScreen;

    private String networkScope = "OFF"; // "OFF", "LAN", "ONLINE"
    private String gameMode = "survival";
    private boolean allowCommands;

    private String initialNetworkScope = "OFF";
    private String initialGameMode = "survival";
    private boolean initialAllowCommands;

    private Button networkScopeButton;
    private Button gameModeButton;
    private Button allowCommandsButton;
    private Button applyChangesButton;
    private TextFieldWidget worldNameField;

    private final GuiNetworkSettingsButton relaysButton;

    public ShareToLanScreen(Screen lastScreenIn) {
        super(new TranslationTextComponent("options.multiplayer.title"));
        this.relaysButton = new GuiNetworkSettingsButton(this);

        this.lastScreen = lastScreenIn;
    }

    @Override
    protected void init() {
        // Detect current state
        boolean lanOpen = LANServerController.isLANOpen();
        boolean onlineOpen = NetworkHandler.ourOpenWorld != null;
        if (onlineOpen) {
            this.networkScope = "ONLINE";
        } else if (lanOpen) {
            this.networkScope = "LAN";
        } else {
            this.networkScope = "OFF";
        }
        this.initialNetworkScope = this.networkScope;
        this.initialGameMode = this.gameMode;
        this.initialAllowCommands = this.allowCommands;

        int midX = this.width / 2;


        this.networkScopeButton = this.addButton(new Button(midX - 100, 68, 200, 20,
                getScopeDisplayText(), (btn) -> {
            if ("OFF".equals(this.networkScope)) {
                this.networkScope = "LAN";
            } else if ("LAN".equals(this.networkScope)) {
                this.networkScope = "ONLINE";
            } else {
                this.networkScope = "OFF";
            }
            btn.setMessage(getScopeDisplayText());
            updateApplyButton();
        }));

        this.worldNameField = new TextFieldWidget(this.font, midX - 100, 106, 200, 20, "");
        this.worldNameField.setText(this.mc.getSession().getUsername() + "'s World");
        this.worldNameField.setMaxStringLength(252);
        this.children.add(this.worldNameField);


        this.gameModeButton = this.addButton(new Button(midX - 155, 150, 150, 20,
                getGameModeDisplayText(), (btn) -> {
            if ("spectator".equals(this.gameMode)) {
                this.gameMode = "creative";
            } else if ("creative".equals(this.gameMode)) {
                this.gameMode = "adventure";
            } else if ("adventure".equals(this.gameMode)) {
                this.gameMode = "survival";
            } else {
                this.gameMode = "spectator";
            }
            btn.setMessage(getGameModeDisplayText());
            updateApplyButton();
        }));
        this.allowCommandsButton = this.addButton(new Button(midX + 5, 150, 150, 20,
                getAllowCommandsDisplayText(), (btn) -> {
            this.allowCommands = !this.allowCommands;
            btn.setMessage(getAllowCommandsDisplayText());
            updateApplyButton();
        }));

        int footerBtnY = this.height - 28;
        this.applyChangesButton = this.addButton(new Button(midX - 155, footerBtnY, 150, 20,
                I18n.format("menu.multiplayerOptions.applyChanges"), (btn) -> {
            applyChanges();
        }));
        this.applyChangesButton.active = hasSettingsChanges();
        this.addButton(new Button(midX + 5, footerBtnY, 150, 20,
                I18n.format("gui.cancel"), (btn) -> {
            this.mc.displayGuiScreen(this.lastScreen);
        }));

        updateApplyButton();
    }

    private String getScopeDisplayText() {
        return I18n.format("lanServer.mode") + ": "
                + I18n.format("lanServer.mode." + this.networkScope.toLowerCase());
    }

    private String getGameModeDisplayText() {
        return I18n.format("selectWorld.gameMode") + ": "
                + I18n.format("selectWorld.gameMode." + this.gameMode);
    }

    private String getAllowCommandsDisplayText() {
        return I18n.format("selectWorld.allowCommands") + " "
                + I18n.format(this.allowCommands ? "options.on" : "options.off");
    }

    private void updateApplyButton() {
        if (this.applyChangesButton != null) {
            this.applyChangesButton.active = hasSettingsChanges();
        }
    }

    private boolean hasSettingsChanges() {
        return !this.networkScope.equals(this.initialNetworkScope)
                || !this.gameMode.equals(this.initialGameMode)
                || this.allowCommands != this.initialAllowCommands;
    }

    private void applyChanges() {
        String worldName = this.worldNameField.getText().trim();
        if (worldName.isEmpty()) {
            worldName = this.mc.getSession().getUsername() + "'s World";
        }
        if (worldName.length() >= 252) {
            worldName = worldName.substring(0, 252);
        }

        if ("OFF".equals(this.networkScope)) {
            if (LANServerController.isLANOpen()) {
                LANServerController.closeLAN();
            }
            if (NetworkHandler.ourOpenWorld != null) {
                NetworkHandler.closeWorld();
            }
            this.mc.displayGuiScreen(this.lastScreen);
        } else if ("LAN".equals(this.networkScope)) {
            if (LANServerController.isLANOpen()) {
                return;
            }
            if (NetworkHandler.ourOpenWorld != null) {
                NetworkHandler.closeWorld();
            }
            PlatformWebRTC.supported();
            PlatformWebRTC.startRTCLANServer();
            LANHostConnectingScreen ls = new LANHostConnectingScreen(
                    this.lastScreen, this.gameMode, this.allowCommands, worldName,
                    false, false
            );
            this.mc.displayGuiScreen(ls);
        } else if ("ONLINE".equals(this.networkScope)) {
            if (LANServerController.isLANOpen()) {
                LANServerController.closeLAN();
            }
            PlatformWebRTC.supported();
            PlatformWebRTC.startRTCLANServer();
            LANHostConnectingScreen ls = new LANHostConnectingScreen(
                    this.lastScreen, this.gameMode, this.allowCommands, worldName,
                    false, true
            );
            this.mc.displayGuiScreen(ls);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        int midX = this.width / 2;

        this.drawCenteredString(this.font, I18n.format("options.multiplayer.title"), midX, 30, 0xFFFFFF);

        int sepY = 45;
        AbstractGui.fill(0, sepY, this.width, sepY + 2, 0xFF555555);
        this.drawCenteredString(this.font, "§n§l" + I18n.format("menu.multiplayerOptions.network.header"), midX, 55, 0xFFFFFF);

        this.drawCenteredString(this.font, I18n.format("lanServer.worldName"), midX, 93, 0xA0A0A0);
        this.worldNameField.render(mouseX, mouseY, partialTicks);

        this.drawCenteredString(this.font, "§n§l" + I18n.format("menu.multiplayerOptions.otherPlayers.header"), midX, 140, 0xFFFFFF);

        int footerSepY = this.height - 36;
        AbstractGui.fill(0, footerSepY, this.width, footerSepY + 2, 0xFF555555);

        this.relaysButton.drawScreen(mouseX, mouseY);

        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (worldNameField.isFocused() && keyCode == 257) {
            if (applyChangesButton.active) {
                applyChanges();
            }
            return true;
        }
        if (worldNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (worldNameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        this.mc.displayGuiScreen(this.lastScreen);
    }

    @Override
    public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
        relaysButton.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
        return super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
