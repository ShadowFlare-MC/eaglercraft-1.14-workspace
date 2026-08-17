package net.deltaclient.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.deltaclient.socials.GuiSocialLoginScreen;import net.deltaclient.socials.GuiSocialScreen;import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.EnumEaglerConnectionState;
import net.minecraft.client.gui.screen.Screen;
import net.lax1dude.eaglercraft.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.internal.PlatformNetworking;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nodejs handler
 * @author deltaclient
 */
public class NetworkHandler {

    public static class OpenWorldInfo {
        public final String worldName;
        public final String code;
        public OpenWorldInfo(String worldName, String code) {
            this.worldName = worldName;
            this.code = code;
        }
    }

    // Tracks whether WE have an open world
    public static OpenWorldInfo ourOpenWorld = null;

    // Pending join requests FROM other players (we are the host)
    public static final List<JoinRequestInfo> pendingJoinRequests = new CopyOnWriteArrayList<>();

    // Pending invites FROM us (or invites we received — reusing JoinRequestInfo)
    public static final List<JoinRequestInfo> pendingInvites = new CopyOnWriteArrayList<>();

    public static class JoinRequestInfo {
        public final String from;
        public final String worldName;
        public final String code;
        public JoinRequestInfo(String from, String worldName, String code) {
            this.from = from;
            this.worldName = worldName;
            this.code = code;
        }
    }
    
    private static IWebSocketClient webSocket;
    private static final String SERVER_URI = "wss://git.deltaclient.site";
    
    public static final List<GuiSocialScreen.SocialPlayerEntry> friends = new CopyOnWriteArrayList<>();
    public static final List<GuiSocialScreen.SocialPlayerEntry> pendingRequests = new CopyOnWriteArrayList<>();
    public static final List<GuiSocialScreen.SocialPlayerEntry> pendingRequestsOutgoing = new CopyOnWriteArrayList<>();
    public static final List<GuiSocialScreen.SocialPlayerEntry> searchResults = new CopyOnWriteArrayList<>();
    public static final Map<String, List<GuiSocialScreen.SocialMessage>> conversations = new ConcurrentHashMap<>();
    
    public static boolean isConnecting = false;
    public static boolean isAuthenticated = false;
    public static String loggedInUsername = null;
    public static String currentToken = null;
    private static String currentSaveKey = null;

    // === Update system ===
    public static String latestVersion = null;
    public static String latestDownloadUrl = null;
    public static boolean versionCheckDone = false;

    // feedback
    public static String lastAuthError = null;

    public static class SocialNotification {
        public String title;
        public String text;
        public long timeStarted;
        public SocialNotification(String title, String text) {
            this.title = title;
            this.text = text;
            this.timeStarted = System.currentTimeMillis();
        }
    }
    
    public static final List<SocialNotification> activeNotifications = new CopyOnWriteArrayList<>();

    public static void connect() {
        if (webSocket != null && webSocket.isOpen()) return;
        if (isConnecting) return;

        isConnecting = true;
        isAuthenticated = false;
        loggedInUsername = null;
        currentToken = null;
        currentSaveKey = null;
        lastAuthError = null;
        friends.clear();
        pendingRequests.clear();
        pendingRequestsOutgoing.clear();
        conversations.clear();

        webSocket = PlatformNetworking.openWebSocket(SERVER_URI);
        if (webSocket == null) {
            System.err.println("[Socials] Failed to open WebSocket to " + SERVER_URI);
            isConnecting = false;
        }
    }

    public static boolean isConnected() {
        return webSocket != null && webSocket.isOpen();
    }

    public static void requestVersionCheck() {
        if (versionCheckDone) return;
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "version_check");
            webSocket.send(req.toString());
        }
    }
    
    public static void login(String username, String password, boolean isToken) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "login");
            req.addProperty("username", username);
            req.addProperty("password", password);
            req.addProperty("isToken", isToken);
            webSocket.send(req.toString());
        } else {
            lastAuthError = "socials.error.notConnected";
        }
    }
    
    public static void register(String username, String email, String password) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "register");
            req.addProperty("username", username);
            req.addProperty("email", email);
            req.addProperty("password", password);
            webSocket.send(req.toString());
        } else {
            lastAuthError = "socials.error.notConnected";
        }
    }
    
    public static void disconnect() {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.close();
            webSocket = null;
        }
        isAuthenticated = false;
        currentToken = null;
        friends.clear();
        pendingRequests.clear();
        conversations.clear();
        ourOpenWorld = null;
        pendingJoinRequests.clear();
        pendingInvites.clear();
    }

    public static void tick() {
        if (webSocket == null) {
            return;
        }
        if (isConnecting) {
            EnumEaglerConnectionState state = webSocket.getState();
            if (state != EnumEaglerConnectionState.CONNECTING) {
                isConnecting = false;
                if (state == EnumEaglerConnectionState.CONNECTED) {
                    System.out.println("[Socials] Connected to Socials server.");
                } else {
                    System.out.println("[Socials] Failed to connect.");
                    webSocket = null;
                    return;
                }
            }
        }

        if (!webSocket.isOpen()) return;

        net.lax1dude.eaglercraft.internal.IWebSocketFrame frame = webSocket.getNextStringFrame();
        while (frame != null) {
            handleMessage(frame.getString());
            frame = webSocket.getNextStringFrame();
        }
    }

    private static void addToken(JsonObject req) {
        if (currentToken != null) {
            req.addProperty("token", currentToken);
        }
    }

    /*
    Server handler
     */
    private static void handleMessage(String jsonStr) {
        try {
            JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
            String type = json.get("type").getAsString();
            
            switch (type) {
                case "login_success":
                    isAuthenticated = true;
                    loggedInUsername = json.get("username").getAsString();
                    isConnecting = false;
                    lastAuthError = null;
                    currentSaveKey = "socials_messages_" + loggedInUsername;
                    loadMessages();
                    if (json.has("token")) {
                        currentToken = json.get("token").getAsString();
                        GuiSocialLoginScreen.handleToken(json.get("token").getAsString());
                    }
                    sendSkinUpdate();
                    break;
                    
                case "login_failed":
                    lastAuthError = json.get("message").getAsString();
                    break;
                case "register_success":
                    lastAuthError = "socials.success.registered";
                    break;
                case "register_failed":
                    lastAuthError = json.get("message").getAsString();
                    break;
                case "initial_data":
                    friends.clear();
                    pendingJoinRequests.clear();
                    pendingInvites.clear();
                    JsonArray friendsArr = json.getAsJsonArray("friends");
                    for (JsonElement e : friendsArr) {
                        JsonObject obj = e.getAsJsonObject();
                        OpenWorldInfo ow = null;
                        if (obj.has("openWorld") && !obj.get("openWorld").isJsonNull()) {
                            JsonObject owObj = obj.getAsJsonObject("openWorld");
                            ow = new OpenWorldInfo(
                                owObj.get("worldName").getAsString(),
                                owObj.get("code").getAsString()
                            );
                        }
                        friends.add(new GuiSocialScreen.SocialPlayerEntry(
                                obj.get("username").getAsString(),
                                obj.get("online").getAsBoolean(),
                                obj.get("status").getAsString(),
                                obj.has("skin") ? obj.get("skin").getAsString() : "",
                                ow
                        ));
                    }
                    
                    pendingRequests.clear();
                    JsonArray requestsArr = json.getAsJsonArray("requests");
                    for (JsonElement e : requestsArr) {
                        if (e.isJsonObject()) {
                            JsonObject reqObj = e.getAsJsonObject();
                            pendingRequests.add(new GuiSocialScreen.SocialPlayerEntry(reqObj.get("username").getAsString(), true, "Pending Request", reqObj.has("skin") ? reqObj.get("skin").getAsString() : ""));
                        } else {
                            pendingRequests.add(new GuiSocialScreen.SocialPlayerEntry(e.getAsString(), true, "Pending Request", ""));
                        }
                    }
                    
                    pendingRequestsOutgoing.clear();
                    if (json.has("pending")) {
                        JsonArray pendingArr = json.getAsJsonArray("pending");
                        for (JsonElement e : pendingArr) {
                            if (e.isJsonObject()) {
                                JsonObject pObj = e.getAsJsonObject();
                                pendingRequestsOutgoing.add(new GuiSocialScreen.SocialPlayerEntry(pObj.get("username").getAsString(), true, "Pending", pObj.has("skin") ? pObj.get("skin").getAsString() : ""));
                            }
                        }
                    }
                    break;
                    
                case "remove_message":
                    if (json.has("target") && json.has("ts")) {
                        String target = json.get("target").getAsString();
                        long ts = json.get("ts").getAsLong();
                        if (conversations.containsKey(target)) {
                            removeIfSafe(conversations.get(target), m -> m.timestamp == ts);
                            saveMessages();
                        }
                    }
                    break;
                    
                case "friend_status":
                    String fName = json.get("username").getAsString();
                    boolean isOnline = json.get("online").getAsBoolean();
                    String fStatus = json.get("status").getAsString();
                    String fSkin = json.has("skin") ? json.get("skin").getAsString() : "";
                    OpenWorldInfo fOpenWorld = null;
                    if (json.has("openWorld") && !json.get("openWorld").isJsonNull()) {
                        JsonObject owObj = json.getAsJsonObject("openWorld");
                        fOpenWorld = new OpenWorldInfo(
                            owObj.get("worldName").getAsString(),
                            owObj.get("code").getAsString()
                        );
                    }
                    for (GuiSocialScreen.SocialPlayerEntry f : friends) {
                        if (f.name.equals(fName)) {
                            f.online = isOnline;
                            f.status = fStatus;
                            if (!fSkin.isEmpty()) f.skinLocation = fSkin;
                            f.openWorld = fOpenWorld;
                            if (isOnline) pushNotification("Friend Online", fName + " is now online.");
                        }
                    }
                    break;
                    
                case "friend_request":
                    if (json.has("from")) {
                        String from = json.get("from").getAsString();
                        String fromSkin = json.has("skin") ? json.get("skin").getAsString() : "";
                        pendingRequests.add(new GuiSocialScreen.SocialPlayerEntry(from, true, "Pending Request", fromSkin));
                        pushNotification("Friend Request", "You have a new friend request from " + from);
                    }
                    break;
                    
                case "friend_request_sent":
                    if (json.has("target")) {
                        String target = json.get("target").getAsString();
                        String tSkin = json.has("skin") ? json.get("skin").getAsString() : "";
                        pendingRequestsOutgoing.add(new GuiSocialScreen.SocialPlayerEntry(target, true, "Pending", tSkin));
                    }
                    break;
                    
                case "friend_request_cancelled":
                    if (json.has("target")) {
                        String target = json.get("target").getAsString();
                        removeIfSafe(pendingRequestsOutgoing, p -> p.name.equals(target));
                    }
                    break;
                    
                case "friend_added": {
                    if (json.has("username")) {
                        String newFriend = json.get("username").getAsString();
                        boolean isFriendOnline = json.has("online") && json.get("online").getAsBoolean();
                        String status = json.has("status") ? json.get("status").getAsString() : "";
                        String skin = json.has("skin") ? json.get("skin").getAsString() : "";
                        OpenWorldInfo ow = null;
                        if (json.has("openWorld") && !json.get("openWorld").isJsonNull()) {
                            JsonObject owObj = json.getAsJsonObject("openWorld");
                            ow = new OpenWorldInfo(owObj.get("worldName").getAsString(), owObj.get("code").getAsString());
                        }
                        friends.add(new GuiSocialScreen.SocialPlayerEntry(newFriend, isFriendOnline, status, skin, ow));
                        removeIfSafe(pendingRequests, p -> p.name.equals(newFriend));
                        removeIfSafe(pendingRequestsOutgoing, p -> p.name.equals(newFriend));
                        pushNotification("Friend Added", newFriend + " is now your friend!");
                    }
                    break;
                }
                    
                case "friend_removed":
                    String removedFriend = json.get("username").getAsString();
                    removeIfSafe(friends, req -> req.name.equals(removedFriend));
                    break;
                    
                case "receive_message":
                    String sender = json.get("sender").getAsString();
                    String text = json.get("text").getAsString();
                    long timestamp = json.get("timestamp").getAsLong();
                    
                    if (!conversations.containsKey(sender)) {
                        conversations.put(sender, new CopyOnWriteArrayList<>());
                    }
                    
                    boolean markRead = false;
                    if (Minecraft.getInstance().currentScreen instanceof GuiSocialScreen) {
                        GuiSocialScreen gss = (GuiSocialScreen)Minecraft.getInstance().currentScreen;
                        if (sender.equals(gss.selectedFriendChat)) {
                            markRead = true;
                        }
                    }
                    
                    conversations.get(sender).add(new GuiSocialScreen.SocialMessage(sender, text, timestamp, markRead));
                    saveMessages();
                    
                    if (!markRead) {
                        pushNotification("New Message", "From " + sender + ": " + text);
                    }
                    break;
                case "search_results":
                    if (json.has("users")) {
                        searchResults.clear();
                        JsonArray usersArr = json.getAsJsonArray("users");
                        for (JsonElement e : usersArr) {
                            if (e.isJsonObject()) {
                                JsonObject obj = e.getAsJsonObject();
                                searchResults.add(new GuiSocialScreen.SocialPlayerEntry(
                                        obj.get("username").getAsString(),
                                        false, "",
                                        obj.has("skin") ? obj.get("skin").getAsString() : ""
                                ));
                            } else {
                                searchResults.add(new GuiSocialScreen.SocialPlayerEntry(e.getAsString(), false, "", ""));
                            }
                        }
                    }
                    break;

                // === World sharing message types ===
                case "friend_open_world":
                    if (json.has("username") && json.has("worldName") && json.has("code")) {
                        String owUname = json.get("username").getAsString();
                        String owName = json.get("worldName").getAsString();
                        String owCode = json.get("code").getAsString();
                        for (GuiSocialScreen.SocialPlayerEntry f : friends) {
                            if (f.name.equals(owUname)) {
                                f.openWorld = new OpenWorldInfo(owName, owCode);
                            }
                        }
                        pushNotification("World Open", owUname + " opened world: " + owName);
                    }
                    break;

                case "friend_close_world":
                    if (json.has("username")) {
                        String cwUname = json.get("username").getAsString();
                        for (GuiSocialScreen.SocialPlayerEntry f : friends) {
                            if (f.name.equals(cwUname)) {
                                f.openWorld = null;
                            }
                        }
                    }
                    break;

                case "join_request":
                    if (json.has("from") && json.has("worldName")) {
                        String reqFrom = json.get("from").getAsString();
                        String reqWorld = json.get("worldName").getAsString();
                        String reqCode = json.has("code") ? json.get("code").getAsString() : "";
                        // Add to pending join requests list
                        pendingJoinRequests.add(new JoinRequestInfo(reqFrom, reqWorld, reqCode));
                        pushNotification("Join Request", reqFrom + " wants to join " + reqWorld + "!");
                        // Also show a friend toast
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) {
                            net.minecraft.client.gui.toasts.FriendToast.showJoinRequestReceived(
                                    mc, reqFrom, reqWorld, getFriendSkin(reqFrom));
                        }
                    }
                    break;

                case "join_request_sent":
                    if (json.has("target") && json.has("worldName")) {
                        String reqTarget = json.get("target").getAsString();
                        String reqTargetWorld = json.get("worldName").getAsString();
                        pushNotification("Request Sent", "Join request sent to " + reqTarget);
                    }
                    break;

                case "join_accepted":
                    if (json.has("host") && json.has("code") && json.has("worldName")) {
                        String hostName = json.get("host").getAsString();
                        String joinCode = json.get("code").getAsString();
                        String joinWorld = json.get("worldName").getAsString();
                        pushNotification("Join Accepted!", hostName + " accepted your request! Code: " + joinCode);
                        // Auto-open the LAN connecting screen so they join immediately
                        Minecraft mc2 = Minecraft.getInstance();
                        if (mc2 != null) {
                            Screen parentScreen = mc2.currentScreen;
                            if (parentScreen == null) {
                                parentScreen = new net.minecraft.client.gui.screen.MainMenuScreen();
                            }
                            mc2.displayGuiScreen(new net.minecraft.client.gui.screen.LANConnectingScreen(parentScreen, joinCode));
                        }
                    }
                    break;

                case "join_rejected":
                    if (json.has("host")) {
                        String rejectHost = json.get("host").getAsString();
                        pushNotification("Join Rejected", rejectHost + " declined your join request.");
                    }
                    break;

                case "invite_received":
                    if (json.has("from") && json.has("worldName") && json.has("code")) {
                        String invFrom = json.get("from").getAsString();
                        String invWorld = json.get("worldName").getAsString();
                        String invCode = json.get("code").getAsString();
                        pendingInvites.add(new JoinRequestInfo(invFrom, invWorld, invCode));
                        pushNotification("Invite", invFrom + " invited you to " + invWorld + "!");
                        Minecraft mc3 = Minecraft.getInstance();
                        if (mc3 != null) {
                            net.minecraft.client.gui.toasts.FriendToast.showInviteReceived(
                                    mc3, invFrom, getFriendSkin(invFrom));
                        }
                    }
                    break;

                case "invite_sent":
                    if (json.has("target") && json.has("worldName")) {
                        String invTarget = json.get("target").getAsString();
                        String invTargetWorld = json.get("worldName").getAsString();
                        pushNotification("Invite Sent", "Invitation sent to " + invTarget);
                        Minecraft mc4 = Minecraft.getInstance();
                        if (mc4 != null) {
                            net.minecraft.client.gui.toasts.FriendToast.showInviteSent(
                                    mc4, invTarget, getFriendSkin(invTarget));
                        }
                    }
                    break;

                case "open_world_success":
                    if (json.has("code") && json.has("worldName")) {
                        String successCode = json.get("code").getAsString();
                        String successWorld = json.get("worldName").getAsString();
                        ourOpenWorld = new OpenWorldInfo(successWorld, successCode);
                        System.out.println("[World] Opened world '" + successWorld + "' with code: " + successCode);
                    }
                    break;

                case "close_world_success":
                    ourOpenWorld = null;
                    System.out.println("[World] Closed world");
                    break;

                case "version_info":
                    latestVersion = json.get("version").getAsString();
                    latestDownloadUrl = json.has("downloadUrl") ? json.get("downloadUrl").getAsString() : "";
                    versionCheckDone = true;
                    System.out.println("[Update] Server version: " + latestVersion + ", download URL: " + latestDownloadUrl);
                    break;

                default:
                    if ("error".equals(type) && json.has("message")) {
                        System.err.println("[Socials] Server error: " + json.get("message").getAsString());
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[Socials] Failed to parse message: " + jsonStr);
            e.printStackTrace();
        }
    }
    
    public static void pushNotification(String title, String text) {
        activeNotifications.add(new SocialNotification(title, text));
    }

    public static void renderNotifications(int scaledWidth, int scaledHeight, float partialTicks) {
        if (activeNotifications.isEmpty()) return;
        
        long now = System.currentTimeMillis();
        int yOffset = scaledHeight - 40;
        
        net.minecraft.client.gui.FontRenderer font = Minecraft.getInstance().fontRenderer;
        
        for (int i = 0; i < activeNotifications.size(); i++) {
            SocialNotification notif = activeNotifications.get(i);
            long diff = now - notif.timeStarted;
            
            // 4 seconds duration
            if (diff > 4000) {
                activeNotifications.remove(i);
                i--;
                continue;
            }
            
            // idk what i did but les go
            float slide = 1.0F;
            if (diff < 300) { // slide in
                slide = diff / 300.0F;
            } else if (diff > 3700) { // slide out
                slide = (4000 - diff) / 300.0F;
            }
            
            // Ease out
            slide = 1.0F - (float)Math.pow(1.0F - slide, 3);
            
            int boxW = 160;
            int boxH = 30;
            
            int startX = scaledWidth + 10;
            int targetX = scaledWidth - boxW - 10;
            
            int currentX = (int)(startX + (targetX - startX) * slide);
            
            // box
            net.minecraft.client.gui.AbstractGui.fill(currentX, yOffset, currentX + boxW, yOffset + boxH, 0xCC222222);
            net.minecraft.client.gui.AbstractGui.fill(currentX, yOffset, currentX + 2, yOffset + boxH, 0xFF44AAFF);
            
            font.drawString("§l" + notif.title, currentX + 8, yOffset + 5, 0xFFFFFF);
            
            String displayTxt = notif.text;
            if (displayTxt.length() > 22) displayTxt = displayTxt.substring(0, 22) + "...";
            font.drawString(displayTxt, currentX + 8, yOffset + 17, 0xAAAAAA);
            
            yOffset -= (boxH + 5); // up
        }
    }
    
    public static void addFriend(String target) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "add_friend");
            req.addProperty("target", target);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void searchUsers(String query) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "search_users");
            req.addProperty("query", query);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void sendSkinUpdate() {
        if (webSocket != null && webSocket.isOpen()) {
            int presetId = net.lax1dude.eaglercraft.profile.EaglerProfile.presetSkinId;
            if (presetId >= 0 && presetId < net.lax1dude.eaglercraft.profile.DefaultSkins.defaultSkinsMap.length) {
                String loc = net.lax1dude.eaglercraft.profile.DefaultSkins.defaultSkinsMap[presetId].location.toString();
                JsonObject req = new JsonObject();
                req.addProperty("type", "set_skin");
                req.addProperty("skin", loc);
                addToken(req);
                webSocket.send(req.toString());
            }
        }
    }

    public static void acceptFriend(String target) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "accept_friend");
            req.addProperty("target", target);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void refuseFriend(String username) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "refuse_friend");
            req.addProperty("target", username);
            addToken(req);
            webSocket.send(req.toString());

            removeIfSafe(pendingRequests, p -> p.name.equals(username));
        }
    }

    public static void cancelFriendRequest(String username) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "cancel_friend_request");
            req.addProperty("target", username);
            addToken(req);
            webSocket.send(req.toString());

            removeIfSafe(pendingRequestsOutgoing, p -> p.name.equals(username));
        }
    }

    public static void removeFriend(String target) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "remove_friend");
            req.addProperty("target", target);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void deleteMessage(String target, GuiSocialScreen.SocialMessage msg) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "remove_message");
            req.addProperty("target", target);
            req.addProperty("ts", msg.timestamp);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void sendMessage(String target, String text) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "send_message");
            req.addProperty("target", target);
            req.addProperty("text", text);
            addToken(req);
            webSocket.send(req.toString());
            
            if (!conversations.containsKey(target)) {
                conversations.put(target, new CopyOnWriteArrayList<>());
            }
            String me = loggedInUsername != null ? loggedInUsername : EaglerProfile.username;
            conversations.get(target).add(new GuiSocialScreen.SocialMessage(me, text, System.currentTimeMillis(), true));
            saveMessages();
        }
    }
    
    public static void saveMessages() {
        if (currentSaveKey != null) {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, List<GuiSocialScreen.SocialMessage>> entry : conversations.entrySet()) {
                JsonArray arr = new JsonArray();
                for (GuiSocialScreen.SocialMessage m : entry.getValue()) {
                    JsonObject msgObj = new JsonObject();
                    msgObj.addProperty("s", m.sender);
                    msgObj.addProperty("t", m.text);
                    msgObj.addProperty("ts", m.timestamp);
                    msgObj.addProperty("r", m.read);
                    arr.add(msgObj);
                }
                root.add(entry.getKey(), arr);
            }
            String jsonStr = root.toString();
            EagRuntime.setStorage(currentSaveKey, jsonStr.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void loadMessages() {
        if (currentSaveKey != null) {
            byte[] data = EagRuntime.getStorage(currentSaveKey);
            if (data != null) {
                try {
                    String jsonStr = new String(data, StandardCharsets.UTF_8);
                    JsonObject root = new JsonParser().parse(jsonStr).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                        String target = entry.getKey();
                        JsonArray arr = entry.getValue().getAsJsonArray();
                        List<GuiSocialScreen.SocialMessage> list = new CopyOnWriteArrayList<>();
                        for (JsonElement el : arr) {
                            JsonObject mObj = el.getAsJsonObject();
                            String sender = mObj.get("s").getAsString();
                            String text = mObj.get("t").getAsString();
                            long ts = mObj.get("ts").getAsLong();
                            boolean read = mObj.has("r") && mObj.get("r").getAsBoolean();
                            GuiSocialScreen.SocialMessage sm = new GuiSocialScreen.SocialMessage(sender, text, ts, true); // default to true since it's old? wait, we can just set read
                            sm.read = read;
                            list.add(sm);
                        }
                        conversations.put(target, list);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load social messages: " + e.getMessage());
                }
            }
        }
    }
    
    public static List<GuiSocialScreen.SocialMessage> getMessages(String friend) {
        return conversations.getOrDefault(friend, new ArrayList<>());
    }

    // === World sharing methods ===

    public static void openWorld(String code, String worldName) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "open_world");
            req.addProperty("code", code);
            req.addProperty("worldName", worldName);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void closeWorld() {
        ourOpenWorld = null;
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "close_world");
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void requestJoin(String target) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "request_join");
            req.addProperty("target", target);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    public static void acceptJoin(String from) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "accept_join");
            req.addProperty("target", from);
            addToken(req);
            webSocket.send(req.toString());
            // Remove from pending list
            removeIfSafe(pendingJoinRequests, j -> j.from.equals(from));
        }
    }

    public static void rejectJoin(String from) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "reject_join");
            req.addProperty("target", from);
            addToken(req);
            webSocket.send(req.toString());
            // Remove from pending list
            removeIfSafe(pendingJoinRequests, j -> j.from.equals(from));
        }
    }

    // === Invite methods ===

    public static void invitePlayer(String target) {
        if (webSocket != null && webSocket.isOpen()) {
            JsonObject req = new JsonObject();
            req.addProperty("type", "invite_player");
            req.addProperty("target", target);
            addToken(req);
            webSocket.send(req.toString());
        }
    }

    // Look up a friend's skin location by username
    public static String getFriendSkin(String username) {
        for (GuiSocialScreen.SocialPlayerEntry f : friends) {
            if (f.name.equals(username)) {
                return f.skinLocation;
            }
        }
        return null;
    }

    // Helper: CopyOnWriteArrayList.removeIf is not supported in TeaVM
    public static <T> void removeIfSafe(List<T> list, java.util.function.Predicate<T> filter) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (filter.test(list.get(i))) {
                list.remove(i);
            }
        }
    }
}
