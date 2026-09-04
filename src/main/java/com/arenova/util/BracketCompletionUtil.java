package com.arenova.util;

import com.arenova.entities.Event;
import com.arenova.entities.EventRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses bracket JSON saved from the frontend and resolves tournament completion.
 */
public final class BracketCompletionUtil {

    private static final Pattern FIELD_STRING = Pattern.compile(
            "\"%s\"\\s*:\\s*\"([^\"]*)\""
    );
    private static final Pattern FIELD_INT = Pattern.compile(
            "\"%s\"\\s*:\\s*(\\d+)"
    );

    private BracketCompletionUtil() {
    }

    /** Bracket entries marked {@code Live} (ongoing matches). */
    public static int countLiveMatches(String bracketJson) {
        if (bracketJson == null || bracketJson.isBlank()) {
            return 0;
        }
        return (int) parseMatches(bracketJson).stream()
                .filter(match -> "Live".equalsIgnoreCase(match.status))
                .count();
    }

    public static boolean isTournamentComplete(String bracketJson) {
        return findChampion(bracketJson) != null;
    }

    private static boolean isGrandFinalRound(String roundLabel) {
        if (roundLabel == null || roundLabel.isBlank()) {
            return false;
        }
        String n = roundLabel.trim().toLowerCase();
        if (n.contains("semi") || n.contains("quarter")) {
            return false;
        }
        return n.equals("final")
                || n.equals("grand final")
                || n.endsWith(" final")
                || n.endsWith("· final");
    }

    public static String findChampion(String bracketJson) {
        if (bracketJson == null || bracketJson.isBlank()) {
            return null;
        }

        List<BracketMatchSlice> matches = parseMatches(bracketJson);
        if (matches.isEmpty()) {
            return null;
        }

        for (BracketMatchSlice match : matches) {
            if (isGrandFinalRound(match.roundLabel) && match.isCompletedWithWinner()) {
                return match.winner;
            }
        }

        int maxRound = matches.stream()
                .mapToInt(m -> m.round)
                .max()
                .orElse(-1);
        if (maxRound < 0) {
            return null;
        }

        String champion = null;
        for (BracketMatchSlice match : matches) {
            if (match.round != maxRound) {
                continue;
            }
            if (!match.isCompletedWithWinner()) {
                return null;
            }
            champion = match.winner;
        }
        return champion;
    }

    public static boolean registrationIsChampion(
            EventRegistration registration,
            Event event,
            String champion
    ) {
        if (champion == null || champion.isBlank() || registration == null) {
            return false;
        }
        String seed = formatSeedName(registration, event);
        return namesMatch(seed, champion);
    }

    public static String formatSeedName(EventRegistration registration, Event event) {
        boolean team = event != null
                && "team".equalsIgnoreCase(event.getParticipantType());
        if (team) {
            String teamName = registration.getTeamName() != null
                    ? registration.getTeamName().trim()
                    : "";
            String teamTag = registration.getTeamTag();
            if (teamTag != null && !teamTag.isBlank()) {
                return teamName + " [" + teamTag.trim() + "]";
            }
            return teamName;
        }
        if (registration.getCaptainUsername() != null
                && !registration.getCaptainUsername().isBlank()) {
            return registration.getCaptainUsername().trim();
        }
        return registration.getTeamName() != null
                ? registration.getTeamName().trim()
                : "";
    }

    private static List<BracketMatchSlice> parseMatches(String bracketJson) {
        List<BracketMatchSlice> matches = new ArrayList<>();
        if (bracketJson == null || bracketJson.isBlank()) {
            return matches;
        }
        String trimmed = bracketJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return matches;
        }

        String body = trimmed.substring(1, trimmed.length() - 1);
        for (String chunk : body.split("\\},\\s*\\{")) {
            String normalized = chunk;
            if (!normalized.startsWith("{")) {
                normalized = "{" + normalized;
            }
            if (!normalized.endsWith("}")) {
                normalized = normalized + "}";
            }

            BracketMatchSlice slice = new BracketMatchSlice();
            slice.round = readInt(normalized, "round");
            slice.roundLabel = readString(normalized, "roundLabel");
            slice.status = readString(normalized, "status");
            slice.winner = readString(normalized, "winner");
            slice.slotA = readString(normalized, "slotA");
            slice.slotB = readString(normalized, "slotB");
            if (slice.status != null || slice.winner != null) {
                matches.add(slice);
            }
        }
        return matches;
    }

    /** Runner-up = other finalist in the completed final (or last completed max-round match). */
    public static String findRunnerUp(String bracketJson) {
        if (bracketJson == null || bracketJson.isBlank()) {
            return null;
        }

        String champion = findChampion(bracketJson);
        if (champion == null) {
            return null;
        }

        List<BracketMatchSlice> matches = parseMatches(bracketJson);
        BracketMatchSlice finalMatch = null;

        for (BracketMatchSlice match : matches) {
            if (isGrandFinalRound(match.roundLabel)
                    && match.isCompletedWithWinner()
                    && namesMatch(match.winner, champion)) {
                finalMatch = match;
                break;
            }
        }

        if (finalMatch == null) {
            int maxRound = matches.stream().mapToInt(m -> m.round).max().orElse(-1);
            for (BracketMatchSlice match : matches) {
                if (match.round == maxRound
                        && match.isCompletedWithWinner()
                        && namesMatch(match.winner, champion)) {
                    finalMatch = match;
                }
            }
        }

        if (finalMatch == null) {
            return null;
        }

        if (finalMatch.slotA != null
                && !finalMatch.slotA.isBlank()
                && !namesMatch(finalMatch.slotA, champion)
                && isRealParticipant(finalMatch.slotA)) {
            return finalMatch.slotA.trim();
        }
        if (finalMatch.slotB != null
                && !finalMatch.slotB.isBlank()
                && !namesMatch(finalMatch.slotB, champion)
                && isRealParticipant(finalMatch.slotB)) {
            return finalMatch.slotB.trim();
        }
        return null;
    }

    public static boolean registrationIsRunnerUp(
            EventRegistration registration,
            Event event,
            String runnerUp
    ) {
        if (runnerUp == null || runnerUp.isBlank() || registration == null) {
            return false;
        }
        String seed = formatSeedName(registration, event);
        return namesMatch(seed, runnerUp);
    }

    private static boolean isRealParticipant(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String n = name.trim();
        return !n.equalsIgnoreCase("TBD")
                && !n.equalsIgnoreCase("BYE")
                && !n.equalsIgnoreCase("—")
                && !n.toLowerCase().startsWith("winner of")
                && !n.toLowerCase().startsWith("loser of");
    }

    private static String readString(String json, String field) {
        Matcher matcher = Pattern.compile(
                "\"%s\"\\s*:\\s*\"([^\"]*)\"".formatted(field)
        ).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static int readInt(String json, String field) {
        Matcher matcher = Pattern.compile(
                "\"%s\"\\s*:\\s*(\\d+)".formatted(field)
        ).matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private static boolean namesMatch(String seed, String champion) {
        if (seed == null || seed.isBlank()) {
            return false;
        }
        return seed.trim().equalsIgnoreCase(champion.trim());
    }

    private static final class BracketMatchSlice {
        private int round = -1;
        private String roundLabel;
        private String status;
        private String winner;
        private String slotA;
        private String slotB;

        private boolean isCompletedWithWinner() {
            return "Completed".equals(status)
                    && winner != null
                    && !winner.isBlank();
        }
    }
}
