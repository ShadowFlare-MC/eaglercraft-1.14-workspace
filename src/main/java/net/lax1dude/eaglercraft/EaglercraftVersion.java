package net.lax1dude.eaglercraft;

public class EaglercraftVersion {

    //////////////////////////////////////////////////////////////////////

    /// Customize these to fit your fork:

    public static final String projectForkName = "Minecraft 1.14.4";
    public static final String projectForkVersion = "u0";
    public static final String projectForkVendor = "deltaclient Teams";

    public static final String projectForkURL = "";

    /// ///////////////////////////////////////////////////////////////////

    public static final String projectOriginName = "Minecraft 1.14.4";
    public static final String projectOriginAuthor = "deltaclient Teams";
    public static final String projectOriginVersion = "u0";
    public static final String projectOriginServerVersion = "";

    public static final String projectOriginURL = "";

    // EPK Version Identifier

    public static final String EPKVersionIdentifier = null; // Set to null to disable EPK version check

    // Client brand identification system configuration

    public static final EaglercraftUUID clientBrandUUID = EagUtils.makeClientBrandUUID(projectForkName);

    public static final EaglercraftUUID legacyClientUUIDInSharedWorld = EagUtils
            .makeClientBrandUUIDLegacy(projectOriginName);

    // Miscellaneous variables:

    public static final boolean forceDemoMode = false;

    public static final String localStorageNamespace = "_eaglercraft_1.0_";

    public static final String screenRecordingFilePrefix = projectOriginName + " " + projectOriginVersion;

}
