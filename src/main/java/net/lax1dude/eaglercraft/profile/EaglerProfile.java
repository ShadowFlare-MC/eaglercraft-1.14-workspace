package net.lax1dude.eaglercraft.profile;

import net.lax1dude.eaglercraft.EaglercraftUUID;
import net.lax1dude.eaglercraft.Random;
import net.lax1dude.eaglercraft.opengl.ImageData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EaglerProfile {

    public static final int SKIN_DATA_SIZE = 64 * 32 * 4;
    public static final int SKIN_DATA_SIZE_LARGE = 64 * 64 * 4;
    public static final Random rand;
    public static final String[] defaultOptionsTextures = new String[]{
            "/skins/01.default_steve.png",
            "/skins/02.default_alex.png",
            "/skins/03.Ari.png",
            "/skins/04.Efe.png",
            "/skins/05.Kai.png",
            "/skins/06.Makena.png",
            "/skins/07.Noor.png",
            "/skins/08.Sunny.png",
            "/skins/09.Zuri.png"
    };
    private static final Map<Integer, WaitingSkin> multiplayerWaitingSkinCache = new HashMap();
    private static final Map<String, CachedSkin> multiplayerSkinCache = new HashMap();
    private static final long maxSkinAge = 1000L * 60L * 5L;
    private static final UserSkin defaultSkin = new UserPresetSkin(0);
    public static String username;
    public static int presetSkinId;
    public static int customSkinId;
    public static int presetCapeId;
    public static String myChannel;
    public static ArrayList<EaglerProfileSkin> skins = new ArrayList();
    public static boolean isServerSkinOverride = false;
    public static boolean isServerCapeOverride = false;
    private static int skinRequestId = 0;

    static {
        String[] usernameDefaultWords = new String[]{
                "Del",
                "Delta",
                "Flare",
                "Shadow",
                "Devv",
                "Xeno",
                "Eagler",
                "Eagl",
                "Darver",
                "Darvler",
                "Vool",
                "Vigg",
                "Vigg",
                "Deev",
                "Yigg",
                "Yeeg"
        };

        rand = new Random();

        do {
            username = usernameDefaultWords[rand.nextInt(usernameDefaultWords.length)] + usernameDefaultWords[rand.nextInt(usernameDefaultWords.length)] + (10 + rand.nextInt(90));
        } while (username.length() > 16);

        presetSkinId = rand.nextInt(12);
        presetCapeId = 0;
        myChannel = username + "_" + (100 + rand.nextInt(900));
        customSkinId = -1;
    }

    public static byte[] getSelfSkinPacket() {
        if (presetSkinId == -1) {
            byte[] d = skins.get(customSkinId).data;
            byte[] d2 = new byte[1 + d.length];
            d2[0] = (byte) 1;
            System.arraycopy(d, 0, d2, 1, d.length);
            return d2;
        } else {
            return new byte[]{(byte) 0, (byte) presetSkinId};
        }
    }

    public static String[] concatArrays(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    public static int addSkin(String name, byte[] data, boolean slim) {
        int i = -1;
        for (int j = 0, l = skins.size(); j < l; ++j) {
            if (skins.get(j).name.equalsIgnoreCase(name)) {
                i = j;
                break;
            }
        }

        if (data.length != SKIN_DATA_SIZE && data.length != SKIN_DATA_SIZE_LARGE) {
            return -1;
        }

        ImageData img = ImageData.loadImageFile(data);
        return i;

    }

    public static int beginSkinRequest(String un) {
        int ret = skinRequestId++;
        if (skinRequestId >= 65536) {
            skinRequestId = 0;
        }
        multiplayerWaitingSkinCache.put(ret, new WaitingSkin(ret, un));
        return ret;
    }

    public static boolean skinRequestPending(String un) {
        return multiplayerWaitingSkinCache.containsKey(un);
    }

    public static UserSkin getUserSkin(String un) {
        CachedSkin cs = multiplayerSkinCache.get(un);
        if (cs == null) {
            return null;
        } else {
            cs.age = System.currentTimeMillis();
            return cs.skin;
        }
    }

    public static void processSkinResponse(byte[] dat) {
        if (dat.length >= 3) {
            int cookie = (((int) dat[0] & 0xFF) << 8) | ((int) dat[1] & 0xFF);
            WaitingSkin st = multiplayerWaitingSkinCache.remove(cookie);
            if (st != null) {
                int t = (int) dat[2] & 0xFF;
                if (t == 0) {
                    if (dat.length == 4) {
                        multiplayerSkinCache.put(st.username, new CachedSkin(st.username, new UserPresetSkin((int) dat[3] & 0xFF)));
                    }
                } else if (t == 1) {
                    if (dat.length == 3 + SKIN_DATA_SIZE) {
                        byte[] datt = new byte[SKIN_DATA_SIZE];
                        System.arraycopy(dat, 3, datt, 0, SKIN_DATA_SIZE);
                        multiplayerSkinCache.put(st.username, new CachedSkin(st.username, new UserCustomSkin(datt)));
                    }
                }
            }
        }
    }

    public static void freeSkins() {
        long millis = System.currentTimeMillis();
        if (multiplayerSkinCache != null && !multiplayerSkinCache.isEmpty()) {
            Iterator<CachedSkin> skns = multiplayerSkinCache.values().iterator();
            if (skns != null) {
                while (skns.hasNext()) {
                    CachedSkin cs = skns.next();
                    if (cs != null && millis - cs.age > maxSkinAge) {
                        cs.skin.free();
                        skns.remove();
                    }
                }
            }
        }
        if (multiplayerWaitingSkinCache != null && !multiplayerWaitingSkinCache.isEmpty()) {
            Iterator<WaitingSkin> skns2 = multiplayerWaitingSkinCache.values().iterator();
            if (skns2 != null) {
                while (skns2.hasNext()) {
                    WaitingSkin cs = skns2.next();
                    if (cs != null && millis - cs.requestStartTime > 10000L) {
                        skns2.remove();
                    }
                }
            }
        }
    }

    public static void freeUserSkin(String un) {
        if (multiplayerSkinCache != null) {
            CachedSkin cs = multiplayerSkinCache.remove(un);
            if (cs != null) {
                cs.skin.free();
            }
        }
    }

    public static void freeAllSkins() {
        if (multiplayerSkinCache != null && !multiplayerSkinCache.isEmpty()) {
            Iterator<CachedSkin> skns = multiplayerSkinCache.values().iterator();
            if (skns != null) {
                while (skns.hasNext()) {
                    CachedSkin cs = skns.next();
                    if (cs != null) {
                        cs.skin.free();
                    }
                }
            }
        }
        if (multiplayerWaitingSkinCache != null) {
            multiplayerWaitingSkinCache.clear();
        }
        if (multiplayerSkinCache != null) {
            multiplayerSkinCache.clear();
        }
    }

    public static String getName() {
        return username;
    }

    public static ResourceLocation getActiveSkinResourceLocation() {
        if (presetSkinId == -1) {
            if (customSkinId >= 0 && customSkinId < GuiScreenEditProfile.customSkins.size()) {
                return GuiScreenEditProfile.customSkins.get(customSkinId).getResource();
            }
            customSkinId = -1;
            presetSkinId = 0;
        } else if (presetSkinId >= 0 && presetSkinId < DefaultSkins.defaultSkinsMap.length) {
            return DefaultSkins.defaultSkinsMap[presetSkinId].location;
        } else {
            presetSkinId = 0;
        }
        return DefaultSkins.DEFAULT_STEVE.location;
    }

    public static SkinModel getActiveSkinModel() {
        if (presetSkinId == -1) {
            if (customSkinId >= 0 && customSkinId < GuiScreenEditProfile.customSkins.size()) {
                return GuiScreenEditProfile.customSkins.get(customSkinId).model;
            }
            customSkinId = -1;
            presetSkinId = 0;
        } else if (presetSkinId >= 0 && presetSkinId < DefaultSkins.defaultSkinsMap.length) {
            return DefaultSkins.defaultSkinsMap[presetSkinId].model;
        } else {
            presetSkinId = 0;
        }
        return DefaultSkins.DEFAULT_STEVE.model;
    }

    public static ResourceLocation getActiveCapeResourceLocation() {
        if (presetCapeId >= 0 && presetCapeId < DefaultCapes.defaultCapesMap.length) {
            ResourceLocation loc = DefaultCapes.defaultCapesMap[presetCapeId].location;
            if (loc != null) {
                return loc;
            }
        }
        return DefaultCapes.NO_CAPE.location;
    }

    public static EaglercraftUUID getPlayerUUID() {
        EaglercraftUUID id = Minecraft.getInstance().getSession().getProfile().getId();
        return id != null ? id : new EaglercraftUUID(0L, 0L);
    }

    public static byte[] getSkinPacket(int vers) {
        if (presetSkinId == -1) {
            if (customSkinId >= 0 && customSkinId < GuiScreenEditProfile.customSkins.size()) {
                GuiScreenEditProfile.CustomSkin customSkin = GuiScreenEditProfile.customSkins.get(customSkinId);
                return vers <= 3 ? SkinPackets.writeMySkinCustomV3(customSkin)
                        : SkinPackets.writeMySkinCustomV4(customSkin);
            }
            customSkinId = -1;
            presetSkinId = 0;
        } else if (presetSkinId >= 0 && presetSkinId < DefaultSkins.defaultSkinsMap.length) {
            return SkinPackets.writeMySkinPreset(presetSkinId);
        } else {
            presetSkinId = 0;
        }
        return SkinPackets.writeMySkinPreset(0);
    }

    public static byte[] getCapePacket() {
        if (presetCapeId >= 0 && presetCapeId < DefaultCapes.defaultCapesMap.length) {
            return SkinPackets.writeMyCapePreset(presetCapeId);
        }
        return SkinPackets.writeMyCapePreset(0);
    }

    public static void handleForceSkinPreset(int presetId) {
        // server overrides player skin
    }

    public static void handleForceSkinCustom(int modelId, byte[] skinData) {
        // server overrides player custom skin
    }

    public static void handleForceCapePreset(int presetId) {
        // server overrides player cape
    }

    public static void handleForceCapeCustom(byte[] capeData) {
        // server overrides player custom cape
    }

    public static void clearServerSkinOverride() {
        isServerSkinOverride = false;
    }

    public static void loadFromStorage() {
//		if(!LocalStorageManager.profileSettingsStorage.hasNoTags()) {
//			presetSkinId = LocalStorageManager.profileSettingsStorage.getInteger("ps");
//			customSkinId = LocalStorageManager.profileSettingsStorage.getInteger("cs");
//			username = LocalStorageManager.profileSettingsStorage.getString("name");
//			myChannel = username + "_" + (100 + rand.nextInt(900));
//			NBTTagCompound n = LocalStorageManager.profileSettingsStorage.getCompoundTag("skins");
//			for(Object s : NBTTagCompound.getTagMap(n).keySet()) {
//				String s2 = (String)s;
//				addSkin(s2, n.getByteArray(s2), false);
//			}
//		}
    }

    public enum EnumSkinType {
        PRESET, CUSTOM_LEGACY
    }

    public interface UserSkin {

        EnumSkinType getSkinType();

        int getSkin();

        int getTexture();

        void free();

    }

    public static class EaglerProfileSkin {
        public String name;
        public byte[] data;
        public boolean slim;
        public int glTex;

        public EaglerProfileSkin(String name, byte[] data, boolean slim, int glTex) {
            this.name = name;
            this.data = data;
            this.slim = slim;
            this.glTex = glTex;
        }
    }

    private static class CachedSkin {

        protected final String username;
        protected UserSkin skin;
        protected long age;

        protected CachedSkin(String username, UserSkin skin) {
            this.username = username;
            this.skin = skin;
            this.age = System.currentTimeMillis();
        }

    }

    private static class UserPresetSkin implements UserSkin {

        protected final int skinType;

        protected UserPresetSkin(int skin) {
            this.skinType = skin;
        }

        @Override
        public EnumSkinType getSkinType() {
            return EnumSkinType.PRESET;
        }

        @Override
        public int getSkin() {
            return skinType;
        }

        @Override
        public int getTexture() {
            return 1;
        }

        @Override
        public void free() {
        }

    }

    private static class UserCustomSkin implements UserSkin {

        protected final byte[] data;
        protected int glTexture;

        protected UserCustomSkin(byte[] data) {
            this.data = data;
            this.glTexture = -1;
        }

        @Override
        public EnumSkinType getSkinType() {
            return EnumSkinType.CUSTOM_LEGACY;
        }

        @Override
        public int getSkin() {
            return -1;
        }

        @Override
        public int getTexture() {
            return glTexture;
        }

        @Override
        public void free() {
            glTexture = -1;
        }

    }

    private static class WaitingSkin {

        protected final int cookie;
        protected final String username;
        protected final long requestStartTime;

        protected WaitingSkin(int cookie, String username) {
            this.cookie = cookie;
            this.username = username;
            this.requestStartTime = System.currentTimeMillis();
        }

    }

}
