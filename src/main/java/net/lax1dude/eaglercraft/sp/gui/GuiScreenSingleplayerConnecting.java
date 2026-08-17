package net.lax1dude.eaglercraft.sp.gui;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EaglercraftVersion;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.lax1dude.eaglercraft.socket.ConnectionHandshake;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.socket.NetHandlerSingleplayerLogin;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.SingleplayerNetworkManager;
import net.minecraft.network.login.client.CLoginStartPacket;
import net.minecraft.util.text.StringTextComponent;

public class GuiScreenSingleplayerConnecting extends Screen {

    private Screen menu;
    private String message;
    private SingleplayerNetworkManager networkManager = null;
    private int timer = 0;

    private long startStartTime;
    private boolean hasOpened = false;

    public GuiScreenSingleplayerConnecting(Screen menu, String message) {
        super(new StringTextComponent(""));
        this.menu = menu;
        this.message = message;
    }

    protected void init() {
        if (startStartTime == 0) this.startStartTime = EagRuntime.steadyTimeMillis();
    }

    public void render(int par1, int par2, float par3) {
        this.renderBackground();
        float f = 2.0f;
        int top = this.height / 3;

        long millis = EagRuntime.steadyTimeMillis();

        long dots = (millis / 500l) % 4l;
        this.drawString(font, message + (dots > 0 ? "." : "") + (dots > 1 ? "." : "") + (dots > 2 ? "." : ""), (this.width - this.font.getStringWidth(message)) / 2, top + 10, 0xFFFFFF);

        long elapsed = (millis - startStartTime) / 1000l;
        if (elapsed > 3) {
            this.drawCenteredString(font, "(" + elapsed + "s)", this.width / 2, top + 25, 0xFFFFFF);
        }

        super.render(par1, par2, par3);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void tick() {
        ++timer;
        if (timer > 1) {
            if (this.networkManager == null) {
                this.networkManager = SingleplayerServerController.localPlayerNetworkManager;
                this.networkManager.connect();
            } else {
                if (this.networkManager.isChannelOpen()) {
                    if (!hasOpened) {
                        hasOpened = true;
                        this.networkManager.setConnectionState(ProtocolType.LOGIN);
                        this.networkManager.setNetHandler(new NetHandlerSingleplayerLogin(this.networkManager, this.mc, this.menu));
                        this.networkManager.sendPacket(new CLoginStartPacket(this.mc.getSession().getProfile(),
                                EaglerProfile.getSkinPacket(3), EaglerProfile.getCapePacket(),
                                ConnectionHandshake.getSPHandshakeProtocolData(), EaglercraftVersion.clientBrandUUID));
                    }
                    try {
                        this.networkManager.processReceivedPackets();
                    } catch (Exception e) {
                        // ignore
                    }
                } else {
                    if (this.networkManager.checkDisconnected()) {
                        if (this.mc.currentScreen == this) {
                            this.mc.loadWorld(null);
                            this.mc.displayGuiScreen(new DisconnectedScreen(menu, "connect.failed", new StringTextComponent("Worker Connection Refused")));
                        }
                    }
                }
            }
        }
    }

    public boolean shouldHangupIntegratedServer() {
        return false;
    }

    public boolean canCloseGui() {
        return false;
    }

}