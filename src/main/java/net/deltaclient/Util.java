package net.deltaclient;

import com.mojang.blaze3d.platform.GlStateManager;
import net.deltaclient.network.NetworkHandler;
import net.lax1dude.eaglercraft.EaglercraftVersion;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static void dontMakeTheTextureBlurry(){
        GlStateManager.texParameter(3553, 10241, 9728); // GL_TEXTURE_MIN_FILTER -> GL_NEAREST
        GlStateManager.texParameter(3553, 10240, 9728); // GL_TEXTURE_MAG_FILTER -> GL_NEAREST
    }

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s]+)");
    /**
     * Added this because clicking links wasnt doing anything
     */
    public static void addClickableUrls(List<ITextComponent> siblings) {
        for (int i = 0; i < siblings.size(); i++) {
            ITextComponent comp = siblings.get(i);
            // Don't override existing click events
            if (comp.getStyle() != null && comp.getStyle().getClickEvent() != null) {
                continue;
            }
            String text = comp.getUnformattedComponentText();
            if (text == null || text.isEmpty() || !text.contains("://")) {
                continue;
            }
            Matcher matcher = URL_PATTERN.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            siblings.remove(i);
            int lastEnd = 0;
            matcher.reset();
            while (matcher.find()) {
                String url = matcher.group();
                int start = matcher.start();
                // Text before URL
                if (start > lastEnd) {
                    String before = text.substring(lastEnd, start);
                    ITextComponent beforeComp = new StringTextComponent(before);
                    beforeComp.setStyle(comp.getStyle().createShallowCopy());
                    siblings.add(i++, beforeComp);
                }
                ITextComponent urlComp = new StringTextComponent(url);
                Style urlStyle = comp.getStyle().createShallowCopy();
                urlStyle.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                urlStyle.setColor(TextFormatting.BLUE);
                urlStyle.setUnderlined(true);
                urlComp.setStyle(urlStyle);
                siblings.add(i++, urlComp);
                lastEnd = matcher.end();
            }
            if (lastEnd < text.length()) {
                String after = text.substring(lastEnd);
                ITextComponent afterComp = new StringTextComponent(after);
                afterComp.setStyle(comp.getStyle().createShallowCopy());
                siblings.add(i, afterComp);
            }
        }
    }

    /**
     * Checks if the server has a newer version of the client available.
     * Compares the local EaglercraftVersion's projectForkVerison with the server's latest version.
     *
     * @return true if a newer version is available, false if same version or no info yet
     */
    public static boolean checkForUpdates() {
        if (!NetworkHandler.versionCheckDone || NetworkHandler.latestVersion == null) {
            return false;
        }
        String localVersion = EaglercraftVersion.projectForkVersion;
        String serverVersion = NetworkHandler.latestVersion;
        return !localVersion.equals(serverVersion);
    }
}
