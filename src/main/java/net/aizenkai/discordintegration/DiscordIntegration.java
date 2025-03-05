package net.aizenkai.discordintegration;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import okhttp3.*;
import com.google.gson.Gson;

@Mod(DiscordIntegration.MOD_ID)
public class DiscordIntegration {

    public static final String MOD_ID = "discordintegration";
    public static final Logger LOGGER = LogUtils.getLogger();
    private MinecraftServer minecraftServer;

    private static String webhookUrl;

    private static DiscordIntegration instance;

    public DiscordIntegration() {
        instance = this;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    public static DiscordIntegration getInstance() {
        return instance;
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Load bot token and channel ID from config
        // webhookUrl = FMLLoader.getConfigDir().resolve("discordintegration-webhook.txt").toUri().toString(); //replace this
        Path configPath = FMLPaths.CONFIGDIR.get();
        webhookUrl = "YOUR WEBHOOOK"; // Load the webhook url here
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        minecraftServer = event.getServer();
        LOGGER.info("Minecraft server is starting!");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Minecraft server is stopping!");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getMessage().getString(); // Get the chat message
        String playerName = player.getName().getString(); // Get player name

        // Send the formatted message to the Discord bot
        sendWebhookMessage(playerName, message);
    }

    private void sendWebhookMessage(String playerName, String message) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        // Create a JSON payload
        Gson gson = new Gson();
        WebhookPayload payload = new WebhookPayload(playerName, message);
        String jsonPayload = gson.toJson(payload);

        RequestBody body = RequestBody.create(jsonPayload, mediaType);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LOGGER.error("Failed to send message to Discord webhook: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    LOGGER.info("Message sent to Discord webhook successfully!");
                } else {
                    LOGGER.warn("Failed to send message to Discord webhook. Response code: " + response.code());
                    try {
                        LOGGER.warn("Response body: " + response.body().string()); // Log the response body for debugging
                    } catch (IOException ex) {
                        LOGGER.error("Failed to log response body: " + ex.getMessage());
                    }
                }
                //The only reason we had this was that, it would not allow us to "ignore" the problem
                //But since it is un-needed we can get rid of it
                response.close();  // Close the response to prevent resource leaks
            }
        });
    }

    // Internal class to package output.
    static class WebhookPayload {
        String player_name;
        String message;
        public WebhookPayload(String player_name, String message) {
            this.player_name = player_name;
            this.message = message;
        }
    }
}