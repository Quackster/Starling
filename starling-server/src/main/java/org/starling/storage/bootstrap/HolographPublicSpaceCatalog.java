package org.starling.storage.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HolographPublicSpaceCatalog {

    private static final String RESOURCE_PATH = "bootstrap/holograph-public-spaces.sql";
    private static final Pattern INSERT_PATTERN_TEMPLATE = Pattern.compile(
            "INSERT INTO `%s` .*? VALUES\\s*(.*?);",
            Pattern.DOTALL);
    public static final String ROOT_PUBLIC_CATEGORY_NAME = "Public Spaces";
    public static final String ROOT_PRIVATE_CATEGORY_NAME = "Rooms";
    private static final Map<Integer, String> CATEGORY_NAME_OVERRIDES = Map.ofEntries(
            Map.entry(3, "Official Rooms"),
            Map.entry(4, "All Rooms"),
            Map.entry(5, "Outdoor Spaces"),
            Map.entry(6, "Trading Rooms"),
            Map.entry(8, "Park and Infobus"),
            Map.entry(9, "Clubs and Cafes"),
            Map.entry(10, "Staff Rooms"),
            Map.entry(11, "Battle Ball & SnowStorm"),
            Map.entry(12, "Battle Ball Beginners"),
            Map.entry(13, "Battle Ball Amateurs"),
            Map.entry(14, "Battle Ball Intermediates"),
            Map.entry(15, "Battle Ball Experts"),
            Map.entry(16, "Battle Ball Elites"),
            Map.entry(17, "SnowStorm Beginners"),
            Map.entry(18, "SnowStorm Amateurs"),
            Map.entry(19, "SnowStorm Intermediates"),
            Map.entry(20, "SnowStorm Experts"),
            Map.entry(21, "SnowStorm Elites"),
            Map.entry(22, "Battle Ball"),
            Map.entry(23, "SnowStorm"),
            Map.entry(24, "Moderator Rooms"),
            Map.entry(26, "Help Desk"),
            Map.entry(27, "Donations"),
            Map.entry(28, "Chill Rooms"),
            Map.entry(29, "Theme Rooms"),
            Map.entry(30, "Hobba Rooms"),
            Map.entry(31, "Guide Rooms"),
            Map.entry(32, "Relaxing Rooms"),
            Map.entry(33, "Game Rooms"),
            Map.entry(34, "Casino")
    );
    private static final Map<Integer, PublicRoomText> PUBLIC_ROOM_TEXT_OVERRIDES = Map.ofEntries(
            Map.entry(101, new PublicRoomText("Welcome Lounge", "New? Lost? Get a warm welcome here.")),
            Map.entry(102, new PublicRoomText("Habbo Lido", "Dive right in!")),
            Map.entry(103, new PublicRoomText("Theatredrome Habboween", "Warm welcome to Bullet For My Valentine!")),
            Map.entry(104, new PublicRoomText("Habbo Library", "Time to catch up on some studying")),
            Map.entry(105, new PublicRoomText("Sunset Cafe", "Let yourself be lulled by the gentle pixel waves...")),
            Map.entry(106, new PublicRoomText("The Dirty Duck Pub", "The perfect place to chill!")),
            Map.entry(107, new PublicRoomText("Habbo Lido II", "Dive right in!")),
            Map.entry(108, new PublicRoomText("Floating Garden", "Peace, tranquility and still waters")),
            Map.entry(109, new PublicRoomText("Rooftop Rumble", "Are you ready?")),
            Map.entry(110, new PublicRoomText("Picnic Garden", "Don't forget to grab a carrot or two!")),
            Map.entry(111, new PublicRoomText("Habbo Gardens", "Go for a stroll outdoors")),
            Map.entry(112, new PublicRoomText("FRANK Infobus", "Welcome to the FRANK Infobus!")),
            Map.entry(113, new PublicRoomText("Battle Ball Beginners", "Beginner battle ball")),
            Map.entry(114, new PublicRoomText("Battle Ball Amateurs", "Amateur battle ball!")),
            Map.entry(115, new PublicRoomText("Battle Ball Intermediates", "Intermediate battle ball!")),
            Map.entry(116, new PublicRoomText("Battle Ball Experts", "Expert battle ball!")),
            Map.entry(117, new PublicRoomText("Battle Ball Elites", "Expert battle ball!")),
            Map.entry(118, new PublicRoomText("SnowStorm Beginners",
                    "Yes, take a load of snowballs and hit the enemy Teams. Easy, isn't it?")),
            Map.entry(119, new PublicRoomText("SnowStorm Amateurs",
                    "Practice improves a Snow Stormer's aim... Ops, missed!")),
            Map.entry(120, new PublicRoomText("SnowStorm Intermediates", "For the accomplished Snow Stormers.")),
            Map.entry(121, new PublicRoomText("SnowStorm Experts",
                    "For the William Tells and Robin Hoods of Snow Storming.")),
            Map.entry(122, new PublicRoomText("SnowStorm Elites",
                    "For the William Tells and Robin Hoods of Snow Storming."))
    );
    private static final HolographPublicSpaceCatalog INSTANCE = loadCatalog();

    private final List<NavigatorCategorySeed> navigatorCategories;
    private final List<PublicRoomSeed> publicRooms;
    private final List<RoomModelSeed> roomModels;

    private HolographPublicSpaceCatalog(
            List<NavigatorCategorySeed> navigatorCategories,
            List<PublicRoomSeed> publicRooms,
            List<RoomModelSeed> roomModels
    ) {
        this.navigatorCategories = List.copyOf(navigatorCategories);
        this.publicRooms = List.copyOf(publicRooms);
        this.roomModels = List.copyOf(roomModels);
    }

    public static HolographPublicSpaceCatalog load() {
        return INSTANCE;
    }

    public List<NavigatorCategorySeed> navigatorCategories() {
        return navigatorCategories;
    }

    public List<PublicRoomSeed> publicRooms() {
        return publicRooms;
    }

    public List<RoomModelSeed> roomModels() {
        return roomModels;
    }

    private static HolographPublicSpaceCatalog loadCatalog() {
        String sql = readBundledSql();

        List<SourceCategory> sourceCategories = parseCategories(sql);
        Map<Integer, Integer> categoryIdMap = buildCategoryIdMap(sourceCategories);
        List<NavigatorCategorySeed> navigatorCategories = buildNavigatorCategories(sourceCategories, categoryIdMap);

        List<PublicRoomSeed> publicRooms = buildPublicRooms(parsePublicRooms(sql), categoryIdMap);
        Set<String> publicModelNames = new HashSet<>();
        for (PublicRoomSeed room : publicRooms) {
            publicModelNames.add(normalize(room.unitStrId()));
        }

        List<RoomModelSeed> roomModels = buildRoomModels(parseRoomModels(sql), publicModelNames);
        return new HolographPublicSpaceCatalog(navigatorCategories, publicRooms, roomModels);
    }

    private static String readBundledSql() {
        try (InputStream input = HolographPublicSpaceCatalog.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Bundled Holograph bootstrap resource is missing: " + RESOURCE_PATH);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bundled Holograph bootstrap resource", e);
        }
    }

    private static List<SourceCategory> parseCategories(String sql) {
        List<List<String>> rows = parseInsertRows(sql, "room_categories");
        List<SourceCategory> categories = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            categories.add(new SourceCategory(
                    parseInt(row, 0),
                    parseInt(row, 1),
                    parseInt(row, 2),
                    parseString(row, 3),
                    parseInt(row, 4),
                    parseInt(row, 6)
            ));
        }
        return categories;
    }

    private static List<SourcePublicRoom> parsePublicRooms(String sql) {
        List<List<String>> rows = parseInsertRows(sql, "rooms");
        List<SourcePublicRoom> publicRooms = new ArrayList<>();
        for (List<String> row : rows) {
            SourcePublicRoom room = new SourcePublicRoom(
                    parseInt(row, 0),
                    parseString(row, 1),
                    parseString(row, 2),
                    parseNullableString(row, 3),
                    parseInt(row, 4),
                    parseNullableString(row, 5),
                    parseNullableString(row, 6),
                    parseInt(row, 11),
                    parseInt(row, 13),
                    parseInt(row, 14)
            );

            if (!room.isPublicRoom()) {
                continue;
            }

            publicRooms.add(room);
        }
        return publicRooms;
    }

    private static List<SourceRoomModel> parseRoomModels(String sql) {
        List<List<String>> rows = parseInsertRows(sql, "room_modeldata");
        List<SourceRoomModel> roomModels = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            roomModels.add(new SourceRoomModel(
                    normalize(parseString(row, 0)),
                    parseInt(row, 2),
                    parseInt(row, 3),
                    parseDouble(row, 4),
                    parseInt(row, 5),
                    parseString(row, 6),
                    parseString(row, 7)
            ));
        }
        return roomModels;
    }

    private static Map<Integer, Integer> buildCategoryIdMap(List<SourceCategory> sourceCategories) {
        int maxSourceCategoryId = 0;
        for (SourceCategory category : sourceCategories) {
            maxSourceCategoryId = Math.max(maxSourceCategoryId, category.id());
        }

        int remappedNoCategoryId = maxSourceCategoryId + 1;
        Map<Integer, Integer> categoryIdMap = new HashMap<>();
        for (SourceCategory category : sourceCategories) {
            categoryIdMap.put(category.id(), category.id() == 0 ? remappedNoCategoryId : category.id());
        }
        return categoryIdMap;
    }

    private static List<NavigatorCategorySeed> buildNavigatorCategories(
            List<SourceCategory> sourceCategories,
            Map<Integer, Integer> categoryIdMap
    ) {
        Set<Integer> sourceNodes = new HashSet<>();
        for (SourceCategory category : sourceCategories) {
            if (category.parentId() > 0) {
                sourceNodes.add(category.parentId());
            }
        }

        List<NavigatorCategorySeed> categories = new ArrayList<>(sourceCategories.size() + 2);
        categories.add(new NavigatorCategorySeed(1, 1, 0, 1, ROOT_PUBLIC_CATEGORY_NAME, 1, 0, 1, 1, 0, 1));
        categories.add(new NavigatorCategorySeed(2, 2, 0, 1, ROOT_PRIVATE_CATEGORY_NAME, 0, 1, 1, 1, 0, 1));

        int orderId = 3;
        for (SourceCategory category : sourceCategories) {
            int mappedId = categoryIdMap.get(category.id());
            int mappedParentId = switch (category.id()) {
                case 0, 4 -> 2;
                case 3 -> 1;
                default -> categoryIdMap.getOrDefault(category.parentId(), category.type() == 0 ? 1 : 2);
            };

            categories.add(new NavigatorCategorySeed(
                    mappedId,
                    orderId++,
                    mappedParentId,
                    sourceNodes.contains(category.id()) ? 1 : 0,
                    resolveCategoryName(category.id(), category.name()),
                    category.type() == 0 ? 1 : 0,
                    category.allowTrading(),
                    Math.max(category.minRoleAccess(), 0),
                    Math.max(category.minRoleAccess(), 0),
                    0,
                    0
            ));
        }

        return categories;
    }

    private static List<PublicRoomSeed> buildPublicRooms(
            List<SourcePublicRoom> sourceRooms,
            Map<Integer, Integer> categoryIdMap
    ) {
        List<PublicRoomSeed> publicRooms = new ArrayList<>(sourceRooms.size());
        for (SourcePublicRoom room : sourceRooms) {
            String modelName = resolvePublicRoomModel(room.modelName(), room.description(), room.casts());
            Integer mappedCategoryId = categoryIdMap.get(room.categoryId());
            if (mappedCategoryId == null) {
                throw new IllegalStateException("Missing category mapping for public room " + room.id());
            }
            PublicRoomText roomText = resolvePublicRoomText(room);

            publicRooms.add(new PublicRoomSeed(
                    room.id(),
                    mappedCategoryId,
                    roomText.name(),
                    modelName,
                    "",
                    room.id(),
                    0,
                    defaultString(room.casts()),
                    room.currentUsers(),
                    room.maxUsers(),
                    0,
                    room.showName() != 0 ? 1 : 0,
                    "",
                    roomText.description()
            ));
        }

        return publicRooms;
    }

    private static String resolveCategoryName(int sourceCategoryId, String fallbackName) {
        return CATEGORY_NAME_OVERRIDES.getOrDefault(sourceCategoryId, fallbackName);
    }

    private static PublicRoomText resolvePublicRoomText(SourcePublicRoom room) {
        PublicRoomText override = PUBLIC_ROOM_TEXT_OVERRIDES.get(room.id());
        if (override != null) {
            return override;
        }

        String description = room.description();
        if (description == null || description.isBlank() || looksLikeTextKey(description)) {
            description = "";
        }
        return new PublicRoomText(room.name(), description);
    }

    private static String resolvePublicRoomModel(String rawModelName, String description, String casts) {
        String normalizedModel = normalize(rawModelName);
        if (!normalizedModel.isBlank()) {
            return normalizedModel;
        }

        String normalizedDescription = normalize(description);
        String normalizedCasts = normalize(casts);
        if (normalizedDescription.startsWith("bb_lobby_") || normalizedCasts.contains("hh_game_bb")) {
            return "bb_lobby_1";
        }
        if (normalizedDescription.startsWith("sw_lobby_") || normalizedCasts.contains("hh_game_snowwar")) {
            return "snowwar_lobby_1";
        }

        throw new IllegalStateException("Unable to infer public room model for description '" + description + "'");
    }

    private static List<RoomModelSeed> buildRoomModels(List<SourceRoomModel> sourceModels, Set<String> publicModelNames) {
        List<RoomModelSeed> roomModels = new ArrayList<>(sourceModels.size());
        for (SourceRoomModel model : sourceModels) {
            roomModels.add(new RoomModelSeed(
                    model.modelName(),
                    publicModelNames.contains(model.modelName()) ? 1 : 0,
                    model.doorX(),
                    model.doorY(),
                    model.doorZ(),
                    model.doorDir(),
                    model.heightmap(),
                    model.publicRoomItems(),
                    "",
                    "",
                    ""
            ));
        }
        return roomModels;
    }

    private static List<List<String>> parseInsertRows(String sql, String tableName) {
        Matcher matcher = Pattern.compile(
                String.format(INSERT_PATTERN_TEMPLATE.pattern(), Pattern.quote(tableName)),
                INSERT_PATTERN_TEMPLATE.flags())
                .matcher(sql);

        if (!matcher.find()) {
            return Collections.emptyList();
        }

        return parseTuples(matcher.group(1));
    }

    private static List<List<String>> parseTuples(String valuesBlock) {
        List<List<String>> tuples = new ArrayList<>();
        List<String> fields = null;
        StringBuilder field = new StringBuilder();
        boolean inString = false;
        int depth = 0;

        for (int index = 0; index < valuesBlock.length(); index++) {
            char current = valuesBlock.charAt(index);
            if (inString) {
                if (current == '\\' && index + 1 < valuesBlock.length()) {
                    appendDecodedEscapedCharacter(field, valuesBlock.charAt(index + 1));
                    index++;
                    continue;
                }

                if (current == '\'') {
                    if (index + 1 < valuesBlock.length() && valuesBlock.charAt(index + 1) == '\'') {
                        field.append('\'');
                        index++;
                    } else {
                        inString = false;
                    }
                    continue;
                }

                field.append(current);
                continue;
            }

            if (current == '\'') {
                inString = true;
                continue;
            }

            if (current == '(') {
                depth++;
                if (depth == 1) {
                    fields = new ArrayList<>();
                    field.setLength(0);
                    continue;
                }
            }

            if (current == ')') {
                depth--;
                if (depth == 0) {
                    if (fields == null) {
                        throw new IllegalStateException("Malformed SQL tuple data in " + RESOURCE_PATH);
                    }
                    fields.add(field.toString().trim());
                    tuples.add(fields);
                    field.setLength(0);
                    continue;
                }
            }

            if (current == ',' && depth == 1) {
                if (fields == null) {
                    throw new IllegalStateException("Malformed SQL tuple data in " + RESOURCE_PATH);
                }
                fields.add(field.toString().trim());
                field.setLength(0);
                continue;
            }

            if (depth >= 1) {
                field.append(current);
            }
        }

        return tuples;
    }

    private static void appendDecodedEscapedCharacter(StringBuilder builder, char next) {
        switch (next) {
            case 'r' -> builder.append('\r');
            case 'n' -> builder.append('\n');
            case 't' -> builder.append('\t');
            case '\\' -> builder.append('\\');
            case '\'' -> builder.append('\'');
            default -> {
                builder.append('\\');
                builder.append(next);
            }
        }
    }

    private static int parseInt(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NULL")) {
            return 0;
        }
        return (int) Math.round(Double.parseDouble(value));
    }

    private static double parseDouble(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NULL")) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    private static String parseString(List<String> row, int index) {
        String value = parseNullableString(row, index);
        return value == null ? "" : value;
    }

    private static String parseNullableString(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.equalsIgnoreCase("NULL")) {
            return null;
        }
        return value;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static boolean looksLikeTextKey(String value) {
        return value.indexOf(' ') < 0 && value.contains("_");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record NavigatorCategorySeed(
            int id,
            int orderId,
            int parentId,
            int isNode,
            String name,
            int publicSpaces,
            int allowTrading,
            int minRoleAccess,
            int minRoleSetFlatCat,
            int clubOnly,
            int isTopPriority
    ) {}

    public record PublicRoomSeed(
            int id,
            int categoryId,
            String name,
            String unitStrId,
            String heightmap,
            int port,
            int door,
            String casts,
            int currentUsers,
            int maxUsers,
            int usersInQueue,
            int isVisible,
            String navigatorFilter,
            String description
    ) {}

    public record RoomModelSeed(
            String modelName,
            int isPublic,
            int doorX,
            int doorY,
            double doorZ,
            int doorDir,
            String heightmap,
            String publicRoomItems,
            String wallpaper,
            String floorPattern,
            String landscape
    ) {}

    private record SourceCategory(
            int id,
            int parentId,
            int type,
            String name,
            int minRoleAccess,
            int allowTrading
    ) {}

    private record SourcePublicRoom(
            int id,
            String name,
            String description,
            String ownerName,
            int categoryId,
            String modelName,
            String casts,
            int showName,
            int currentUsers,
            int maxUsers
    ) {
        private boolean isPublicRoom() {
            return categoryId > 0 && (ownerName == null || ownerName.isBlank());
        }
    }

    private record SourceRoomModel(
            String modelName,
            int doorX,
            int doorY,
            double doorZ,
            int doorDir,
            String heightmap,
            String publicRoomItems
    ) {}

    private record PublicRoomText(
            String name,
            String description
    ) {}
}
