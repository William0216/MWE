package fr.alexdoru.megawallsenhancementsmod.scoreboard;

import fr.alexdoru.megawallsenhancementsmod.chat.ChatUtil;
import fr.alexdoru.megawallsenhancementsmod.gui.huds.LastWitherHPHUD;
import fr.alexdoru.megawallsenhancementsmod.utils.SoundUtil;
import fr.alexdoru.megawallsenhancementsmod.utils.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardParser {

    public static final Pattern GAME_ID_PATTERN = Pattern.compile("\\d+/\\d+/\\d+\\s+([\\d\\w]+)");
    private static final Pattern GATES_OPEN_PATTERN = Pattern.compile("Gates Open: \\d+:\\d+");
    private static final Pattern WALLS_FALL_PATTERN = Pattern.compile("Walls Fall: (\\d+):(\\d+)");
    private static final Pattern MW_INGAME_PATTERN = Pattern.compile("[0-9]+\\sF\\.\\sKills?\\s[0-9]+\\sF\\.\\sAssists?");
    private static final Pattern PREGAME_LOBBY_PATTERN = Pattern.compile("Players:\\s*[0-9]+/[0-9]+");
    private static final Pattern WITHER_ALIVE_PATTERN = Pattern.compile("\\[[BGRY]\\] Wither HP: ([,\\d]+)");
    private static final Pattern REPLAY_MAP_PATTERN = Pattern.compile("Map: ([a-zA-Z0-9_ ]+)");

    private static boolean triggeredWallsFallAlert = false;
    private static boolean triggeredGameEndAlert = false;

    private final List<String> aliveWithers = new ArrayList<>(4);
    private String gameId = null;
    private boolean isInMwGame = false;
    private boolean isMWEnvironement = false;
    private boolean isReplayMode = false;
    private boolean isMWReplay = false;
    private String replayMap = null;
    private boolean isInSkyblock = false;
    private boolean isPreGameLobby = false;
    private boolean isPrepPhase = false;
    private boolean hasGameEnded = false;

    public static void onGameStart() {
        triggeredWallsFallAlert = false;
        triggeredGameEndAlert = false;
    }

    /* This runs on every tick to parse the scoreboard data */
    public ScoreboardParser() {

        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        final Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        final String title = ScoreboardUtils.getUnformattedSidebarTitle(scoreboard);
        if (title.contains("MEGA WALLS")) {
            isMWEnvironement = true;
            this.parseMegaWallsScoreboard(scoreboard);
        } else if (title.contains("REPLAY")) {
            isReplayMode = true;
            this.parseReplayScoreboard(scoreboard);
        } else if (title.contains("SKYBLOCK")) {
            isInSkyblock = true;
        }

    }

    private void parseMegaWallsScoreboard(Scoreboard scoreboard) {
        final List<String> formattedLines = ScoreboardUtils.getFormattedSidebarText(scoreboard);
        final List<String> cleanLines = ScoreboardUtils.stripControlCodes(formattedLines);

        if (cleanLines.size() < 10) {
            return;
        }

        if (MW_INGAME_PATTERN.matcher(cleanLines.get(9)).find()) {
            isInMwGame = true;
            final Matcher gameIDMatcher = GAME_ID_PATTERN.matcher(cleanLines.get(0));
            if (gameIDMatcher.find()) {
                gameId = gameIDMatcher.group(1);
            }
        } else {
            for (final String line : cleanLines) {
                if (PREGAME_LOBBY_PATTERN.matcher(line).find()) {
                    isPreGameLobby = true;
                    return;
                }
            }
        }

        final String gameTimeLine = cleanLines.get(1);
        final Matcher wallsFallMatcher = WALLS_FALL_PATTERN.matcher(gameTimeLine);
        if (wallsFallMatcher.find()) {
            isPrepPhase = true;
            if (!triggeredWallsFallAlert && wallsFallMatcher.group(1).equals("00") && wallsFallMatcher.group(2).equals("10") && !Display.isActive()) {
                SoundUtil.playGameStartSound();
                triggeredWallsFallAlert = true;
            }
        } else if (GATES_OPEN_PATTERN.matcher(gameTimeLine).find()) {
            isPrepPhase = true;
        } else if (!triggeredGameEndAlert && "Game End: 05:00".equals(gameTimeLine)) {
            SoundUtil.playNotePling();
            ChatUtil.addChatMessage(EnumChatFormatting.YELLOW + "Game ends in 5 minutes!");
            triggeredGameEndAlert = true;
        } else if (gameTimeLine.contains("Game End: 00:00")) {
            hasGameEnded = true;
        }

        int eliminatedTeams = 0;
        int witherHP = 1000;

        for (int i = 3; i < 7; i++) {

            final String line = cleanLines.get(i);
            /*Wither alive detection*/
            final Matcher witherAliveMatcher = WITHER_ALIVE_PATTERN.matcher(line);
            final String colorCode;
            if (witherAliveMatcher.find()) {
                colorCode = StringUtil.getLastColorCodeBefore(formattedLines.get(i), "\\[");
                aliveWithers.add(colorCode);
                witherHP = Integer.parseInt(witherAliveMatcher.group(1).replace(",", ""));
            }

            if (line.contains("eliminated!")) {
                eliminatedTeams++;
            }

        }

        if (eliminatedTeams == 3) {
            hasGameEnded = true;
        }

        if (isOnlyOneWitherAlive()) {
            LastWitherHPHUD.instance.updateWitherHP(witherHP);
        }
    }

    private void parseReplayScoreboard(Scoreboard scoreboard) {
        final List<String> formattedLines = ScoreboardUtils.getFormattedSidebarText(scoreboard);
        final List<String> cleanLines = ScoreboardUtils.stripControlCodes(formattedLines);
        if (cleanLines.isEmpty()) {
            return;
        }
        for (final String line : cleanLines) {
            final Matcher mapMatcher = REPLAY_MAP_PATTERN.matcher(line);
            if (mapMatcher.find()) {
                replayMap = mapMatcher.group(1);
            } else if (line.contains("Game: Mega Walls")) {
                isMWReplay = true;
            }
        }
    }

    public boolean isWitherAlive(String colorCode) {
        return aliveWithers.contains(colorCode);
    }

    public boolean areAllWithersAlive() {
        return aliveWithers.size() == 4;
    }

    public String getGameId() {
        return gameId;
    }

    public List<String> getAliveWithers() {
        return aliveWithers;
    }

    public boolean hasGameEnded() {
        return hasGameEnded;
    }

    public boolean isPrepPhase() {
        return isPrepPhase;
    }

    public boolean isOnlyOneWitherAlive() {
        return aliveWithers.size() == 1;
    }

    public boolean isMWEnvironement() {
        return isMWEnvironement;
    }

    public boolean isReplayMode() {
        return isReplayMode;
    }

    public boolean isMWReplay() {
        return isMWReplay;
    }

    public String getReplayMap() {
        return replayMap;
    }

    public boolean isInSkyblock() {
        return isInSkyblock;
    }

    public boolean isInMwGame() {
        return isInMwGame;
    }

    public boolean isPreGameLobby() {
        return isPreGameLobby;
    }

}
