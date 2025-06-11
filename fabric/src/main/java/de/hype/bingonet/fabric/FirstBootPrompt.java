package de.hype.bingonet.fabric;

import de.hype.bingonet.client.common.SystemUtils;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.shared.constants.VanillaItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.awt.*;

public class FirstBootPrompt extends Screen {
    public final Screen parent;
    boolean wait = true;
    MinecraftClient client = MinecraftClient.getInstance();
    TextRenderer textRenderer = client.textRenderer;

    protected FirstBootPrompt() {
        super(Text.literal("§6§lBingo Net Notice"));
        parent = client.currentScreen;
        init();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.init(client, width, height);
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        try {
            String notice = """
                    §7We experienced players being surprised by the things we collect. Due too this this Screen was created. We try to be very open about the things we collect to avoid surprises. We think the Skyblock Data we collect is beneficial for the community over all.
                    
                    We also collect IP Addresses etc for moderation and analysis purposes. We understand wishes for privacy. We do not receive any Data until you connect to the Network.
                    
                    Before you can use Bingo Net you need to do some things. Due too technical Reasons this can't be skipped. If you don't want to do this now you will have to remove the mod again for now. You can click the §aMod Self Remove§7 to remove the mod. You will have to restart the game afterwards manually though. The Process will cost you roughly §630 Minutes§7 if you actually read the Stuff.
                    
                    Bingo Net is a Network Mod. Due too this we require you to link (Your/a new) §bDiscord Account§7 to your Minecraft Account.
                    
                    This is the case because we
                    ◉ Use Discord for Announcements regarding the Mod AND its Usage
                    ◉ We can use this to inform you about Changes and Updates
                    ◉ We have our explanations there
                    and more.
                    
                    The Data we get sent is processed by us and a lot of it also stored to Database. We also log a lot of things.
                    
                    §f§lThis mod has DRM. As the mod Self Remove already indicates we have the option to trigger a mod Self Removal option.§r§7
                    - We spent thousands of hours into the Network and we do not want some individuals to be able to use the things we spent a lot of effort in.
                    We also have a couple Trolls at our disposal but those are made in a way to avoid any type of permanent damage.
                    - For example to force a Lobby Swap in case of lobby sniping etc.
                    We collect Data about Bingo which also include some personal Data of yours such as Profiles, Contribution Numbers and more.
                    
                    We do §cNOT§7 sell your Data but use them to analyze Bingos and improve our Data sets to provide it to those we know how to understand it. For example to allow looking up old Profile Data to view Contribution counts and more!
                    
                    §4§lWhen you click the connect to Network Button you agree to the Privacy Policy!
                    
                    The Mod is an entirely provided on an as is basis and we are not responsible for any damage caused by the Mod.
                    
                    §7We knowingly Color Coded like this! We want to make it so you actually read through the Stuff!
                    """;

            int padding = 10;
            int buttonHeight = 20;
            int buttonWidth = 200;
            int textWidgetWidth = (int) (width * 0.5); // 50% of the screen width
            int textWidgetHeight = height / 2;

            TextWidget titleWidget = new TextWidget(Text.literal("Bingo Net Notice"), textRenderer);
            titleWidget.setX((width - textWidgetWidth) / 2);
            titleWidget.setY(padding);

            ScrollableTextWidget textWidget = new ScrollableTextWidget((width - textWidgetWidth) / 2, padding * 2 + titleWidget.getHeight(), textWidgetWidth, textWidgetHeight, Text.literal(notice), textRenderer);
            //Color Coding against using it knowingly.
            ButtonWidget openDiscord = ButtonWidget.builder(Text.literal("§cOpen Discord in Browser"), (b) -> this.openDiscord())
                    .dimensions((width - buttonWidth) / 2, padding * 3 + titleWidget.getHeight() + textWidgetHeight, buttonWidth, buttonHeight)
                    .build();
            ButtonWidget openPrivacyPolicyInBrowser = ButtonWidget.builder(Text.literal("§aOpen Privacy Policy in Browser"), (b) -> this.openPrivacyBrowser())
                    .dimensions((width - buttonWidth) / 2, padding * 4 + titleWidget.getHeight() + textWidgetHeight + buttonHeight, buttonWidth, buttonHeight)
                    .build();
            ButtonWidget connectToNetwork = ButtonWidget.builder(Text.literal("§cConnect to Network (Requires to be registered already)"), (b) -> this.connectToNetwork())
                    .dimensions((width - buttonWidth) / 2, padding * 5 + titleWidget.getHeight() + textWidgetHeight + buttonHeight * 2, buttonWidth, buttonHeight)
                    .build();
            ButtonWidget modSelfRemove = ButtonWidget.builder(Text.literal("§aMod Self Remove"), (b) -> this.selfRemove())
                    .dimensions((width - buttonWidth) / 2, padding * 6 + titleWidget.getHeight() + textWidgetHeight + buttonHeight * 3, buttonWidth, buttonHeight)
                    .build();
            ButtonWidget gitHubButton = ButtonWidget.builder(Text.literal("§aOpen Github"), (b) -> this.openGithub())
                    .dimensions((width - buttonWidth) / 2, padding * 7 + titleWidget.getHeight() + textWidgetHeight + buttonHeight * 4, buttonWidth, buttonHeight)
                    .build();
            CheckboxWidget useBBIntegration = CheckboxWidget.builder(Text.literal("Do you want to use our Bingo Brewers Integration?"), MinecraftClient.getInstance().textRenderer).checked(false).tooltip(
                    Tooltip.of(Text.literal("The Bingo Brewers Integration makes Bingo Net connect to indigo_polecat's Bingo Brewer Mod Server. (bingobrewers.com). It is not subject to our own Privacy. We are not responsible for possible Network Bans."))
            ).callback((checkbox, checked) -> {
                BingoNet.generalConfig.useBingoBrewersIntegration = checked;
                BingoNet.generalConfig.save();
            }).build();
            useBBIntegration.onPress();
            useBBIntegration.setDimensionsAndPosition((width - textWidgetWidth) / 2, padding * 8 + titleWidget.getHeight() + buttonHeight * 5, textWidgetWidth, buttonHeight);

            addDrawableChild(titleWidget);
            addDrawableChild(textWidget);
            addDrawableChild(openDiscord);
            addDrawableChild(gitHubButton);
            addDrawableChild(openPrivacyPolicyInBrowser);
            addDrawableChild(connectToNetwork);
            addDrawableChild(modSelfRemove);
            addDrawableChild(useBBIntegration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openGithub() {
        String link = "https://github.com/HacktheTime/BingoNet";
        SystemUtils.openInBrowser(link);
        SystemUtils.setClipboardContent(link);
    }

    private void openPrivacyBrowser() {
        String link = "https://hackthetime.de/privacy";
        SystemUtils.openInBrowser(link);
        SystemUtils.setClipboardContent(link);
    }

    private void selfRemove() {
        if (BBsentialConnection.selfDestruct()) {
            throw new RuntimeException("Mod was removed successfully. You can Relaunch (cause we cant for you)");
        } else {
            client.execute(() -> client.setScreen(new NoticeScreen(() -> {
                throw new RuntimeException("Closing MC");
            }, Text.literal("BingoNet"), Text.literal("Sorry we weren't able to remove the mod for you. You will need to remove it yourself"))));
        }
    }

    public void waitFor() {
        while (wait) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void openDiscord() {
        String discord = "ArtWfVMwxm";
        SystemUtils.openInBrowser("https://discord.gg/%s".formatted(discord));
        SystemUtils.setClipboardContent(discord);
    }

    public void connectToNetwork() {
        ((Utils) EnvironmentCore.utils).displayToast(new Utils.BBToast("Connecting to Network", "Connecting", null, null, Color.CYAN));
        BingoNet.connectToBBserver();
        while (BingoNet.connection == null) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        while (BingoNet.connection.getAuthenticated() == null) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (BingoNet.connection.getAuthenticated() == Boolean.TRUE) {
            ((Utils) EnvironmentCore.utils).displayToast(new Utils.BBToast("Connected", "Authentication Successful. Continuing", SoundEvents.ENTITY_ARROW_HIT, VanillaItems.EMERALD_BLOCK, Color.GREEN));
            close();
        } else {
            ((Utils) EnvironmentCore.utils).displayToast(new Utils.BBToast("Connecting Failed", "You are not registered. Please register! (If you are registered try again. If still not working you probably have an ongoing Punishment.)", null, VanillaItems.REDSTONE_BLOCK, Color.RED));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void close() {
        client.execute(() -> client.setScreen(parent));
        wait = false;
    }
}