package org.oldskooler.vibe.storage.bootstrap;

import org.oldskooler.vibe.game.room.layout.RoomLayoutRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.defaultString;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.normalize;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.parseDouble;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.parseInsertRows;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.parseInt;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.parseNullableString;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.parseString;
import static org.oldskooler.vibe.storage.bootstrap.BootstrapSqlSupport.readBundledSql;

public final class HolographPublicSpaceCatalog {

    private static final String RESOURCE_PATH = "bootstrap/holograph-public-spaces.sql";
    public static final String ROOT_PUBLIC_CATEGORY_NAME = "Public Spaces";
    public static final String ROOT_PRIVATE_CATEGORY_NAME = "Rooms";
    private static final String GENERIC_HALLWAY_DESCRIPTION = "Roam more of the hotel's corridors.";
    private static final List<String> ROMAN_NUMERALS = List.of(
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"
    );
    private static final List<String> FLOOR_LOBBY_NAMES = List.of(
            "First Floor Lobby",
            "Second Floor Lobby",
            "Third Floor Lobby"
    );
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
    private static final List<SupplementalPublicRoomSeed> SUPPLEMENTAL_PUBLIC_ROOMS = buildSupplementalPublicRooms();
    private static final HolographPublicSpaceCatalog INSTANCE = loadCatalog();

    private final List<NavigatorCategorySeed> navigatorCategories;
    private final List<PublicRoomSeed> publicRooms;
    private final List<RoomModelSeed> roomModels;

    /**
     * Creates a new HolographPublicSpaceCatalog.
     * @param navigatorCategories the navigator categories value
     * @param publicRooms the public rooms value
     * @param roomModels the room models value
     */
    private HolographPublicSpaceCatalog(
            List<NavigatorCategorySeed> navigatorCategories,
            List<PublicRoomSeed> publicRooms,
            List<RoomModelSeed> roomModels
    ) {
        this.navigatorCategories = List.copyOf(navigatorCategories);
        this.publicRooms = List.copyOf(publicRooms);
        this.roomModels = List.copyOf(roomModels);
    }

    /**
     * Loads.
     * @return the resulting load
     */
    public static HolographPublicSpaceCatalog load() {
        return INSTANCE;
    }

    /**
     * Navigators categories.
     * @return the result of this operation
     */
    public List<NavigatorCategorySeed> navigatorCategories() {
        return navigatorCategories;
    }

    /**
     * Publics rooms.
     * @return the result of this operation
     */
    public List<PublicRoomSeed> publicRooms() {
        return publicRooms;
    }

    /**
     * Rooms models.
     * @return the resulting room models
     */
    public List<RoomModelSeed> roomModels() {
        return roomModels;
    }

    /**
     * Loads catalog.
     * @return the resulting load catalog
     */
    private static HolographPublicSpaceCatalog loadCatalog() {
        String sql = readBundledSql(HolographPublicSpaceCatalog.class, RESOURCE_PATH);

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

    /**
     * Parses categories.
     * @param sql the sql value
     * @return the resulting parse categories
     */
    private static List<SourceCategory> parseCategories(String sql) {
        List<List<String>> rows = parseInsertRows(sql, "room_categories", RESOURCE_PATH);
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

    /**
     * Parses public rooms.
     * @param sql the sql value
     * @return the resulting parse public rooms
     */
    private static List<SourcePublicRoom> parsePublicRooms(String sql) {
        List<List<String>> rows = parseInsertRows(sql, "rooms", RESOURCE_PATH);
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

    /**
     * Parses room models.
     * @param sql the sql value
     * @return the resulting parse room models
     */
    private static List<SourceRoomModel> parseRoomModels(String sql) {
        List<List<String>> rows = parseInsertRows(sql, "room_modeldata", RESOURCE_PATH);
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

    /**
     * Builds category id map.
     * @param sourceCategories the source categories value
     * @return the resulting build category id map
     */
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

    /**
     * Builds navigator categories.
     * @param sourceCategories the source categories value
     * @param categoryIdMap the category id map value
     * @return the resulting build navigator categories
     */
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

    /**
     * Builds public rooms.
     * @param sourceRooms the source rooms value
     * @param categoryIdMap the category id map value
     * @return the resulting build public rooms
     */
    private static List<PublicRoomSeed> buildPublicRooms(
            List<SourcePublicRoom> sourceRooms,
            Map<Integer, Integer> categoryIdMap
    ) {
        List<PublicRoomSeed> publicRooms = new ArrayList<>(sourceRooms.size() + SUPPLEMENTAL_PUBLIC_ROOMS.size());
        Set<Integer> usedRoomIds = new HashSet<>();
        for (SourcePublicRoom room : sourceRooms) {
            String modelName = resolvePublicRoomModel(room.modelName(), room.description(), room.casts());
            Integer mappedCategoryId = categoryIdMap.get(room.categoryId());
            if (mappedCategoryId == null) {
                throw new IllegalStateException("Missing category mapping for public room " + room.id());
            }
            if (!usedRoomIds.add(room.id())) {
                throw new IllegalStateException("Duplicate public room id " + room.id());
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

        for (SupplementalPublicRoomSeed room : SUPPLEMENTAL_PUBLIC_ROOMS) {
            Integer mappedCategoryId = categoryIdMap.get(room.categoryId());
            if (mappedCategoryId == null) {
                throw new IllegalStateException("Missing category mapping for supplemental public room " + room.id());
            }
            if (!usedRoomIds.add(room.id())) {
                throw new IllegalStateException("Duplicate public room id " + room.id());
            }

            publicRooms.add(new PublicRoomSeed(
                    room.id(),
                    mappedCategoryId,
                    room.name(),
                    room.unitStrId(),
                    "",
                    room.id(),
                    0,
                    room.casts(),
                    0,
                    room.maxUsers(),
                    0,
                    1,
                    "",
                    room.description()
            ));
        }

        return publicRooms;
    }

    /**
     * Resolves category name.
     * @param sourceCategoryId the source category id value
     * @param fallbackName the fallback name value
     * @return the resulting resolve category name
     */
    private static String resolveCategoryName(int sourceCategoryId, String fallbackName) {
        return CATEGORY_NAME_OVERRIDES.getOrDefault(sourceCategoryId, fallbackName);
    }

    /**
     * Resolves public room text.
     * @param room the room value
     * @return the resulting resolve public room text
     */
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

    /**
     * Resolves public room model.
     * @param rawModelName the raw model name value
     * @param description the description value
     * @param casts the casts value
     * @return the resulting resolve public room model
     */
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

    /**
     * Builds room models.
     * @param sourceModels the source models value
     * @param publicModelNames the public model names value
     * @return the resulting build room models
     */
    private static List<RoomModelSeed> buildRoomModels(List<SourceRoomModel> sourceModels, Set<String> publicModelNames) {
        List<RoomModelSeed> roomModels = new ArrayList<>(sourceModels.size() + publicModelNames.size());
        Set<String> seededModels = new HashSet<>();
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
            seededModels.add(model.modelName());
        }

        List<String> missingPublicModels = publicModelNames.stream()
                .filter(modelName -> !seededModels.contains(modelName))
                .sorted()
                .toList();

        for (String modelName : missingPublicModels) {
            RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.defaultPublicRoom(modelName);
            roomModels.add(new RoomModelSeed(
                    modelName,
                    1,
                    visuals.doorX(),
                    visuals.doorY(),
                    visuals.doorZ(),
                    visuals.doorDir(),
                    visuals.heightmap(),
                    "",
                    "",
                    "",
                    ""
            ));
        }

        return roomModels;
    }

    /**
     * Builds supplemental public rooms.
     * @return the resulting build supplemental public rooms
     */
    private static List<SupplementalPublicRoomSeed> buildSupplementalPublicRooms() {
        List<SupplementalPublicRoomSeed> rooms = new ArrayList<>();

        rooms.add(supplementalRoom(201, 7, "Ballroom", "ballroom", "hh_room_ballroom", 30, "Come play ball!"));
        rooms.add(supplementalRoom(202, 9, "The Chromide Club", "bar_a", "hh_room_bar", 30, "Ghetto Fabulous"));
        rooms.add(supplementalRoom(203, 9, "Club Massiva", "bar_b", "hh_room_bar", 30, "Strut your funky stuff!"));
        rooms.add(supplementalRoom(204, 9, "Chill-out Room", "malja_bar_a", "hh_room_disco", 25, "Rest your dancing feet!"));
        rooms.add(supplementalRoom(205, 9, "Dancefloor", "malja_bar_b", "hh_room_disco", 30, "Make all the right moves."));
        rooms.add(supplementalRoom(206, 9, "Beauty Salon", "beauty_salon0", "hh_room_beauty_salon_general", 20,
                "The Penelope movie is in cinemas February 1st."));
        rooms.add(supplementalRoom(207, 9, "Beauty Salon L'Oreal", "beauty_salon1", "hh_room_beauty_salon_loreal", 20,
                "No Pixel Surgery, only natural make-ups!"));
        rooms.add(supplementalRoom(208, 9, "Cafe Ole", "cafe_ole", "hh_room_cafe", 25,
                "Relax with friends over one of Maria's specialty coffees."));
        rooms.add(supplementalRoom(209, 9, "The Oasis", "cafe_gold0", "hh_room_gold", 25, "Just an illusion."));
        rooms.add(supplementalRoom(210, 5, "Zen Garden", "chill", "hh_room_chill", 20, "Get ready to meditate!"));
        rooms.add(supplementalRoom(211, 7, "Habbo Cinema", "cinema_a", "hh_room_cinema", 25,
                "Now showing: Kick Warz II - The revenge of Donnie Santini!"));
        rooms.add(supplementalRoom(212, 9, "Club Mammoth", "club_mammoth", "hh_room_clubmammoth", 30,
                "Monumental and magnificent. For Habbo Club members only."));
        rooms.add(supplementalRoom(213, 9, "Eric's Eaterie", "cr_cafe", "hh_room_erics", 25, "Join Eric for a bite to eat."));
        rooms.add(supplementalRoom(214, 7, "The Den", "cr_staff", "hh_room_den", 20, "Has anyone seen my map?"));
        rooms.add(supplementalRoom(215, 9, "Dusty Lounge", "dusty_lounge", "hh_room_dustylounge", 25,
                "A dignified lounge for sitting back and enjoying a licorice pipe."));
        rooms.add(supplementalRoom(216, 9, "Emperor's Hall", "emperors", "hh_room_emperors", 30,
                "Even the smallest of light... shines in the darkness."));
        rooms.add(supplementalRoom(217, 3, "Median Lobby", "lobby_a", "hh_room_lobby", 40, "A Mean place to hang."));
        rooms.add(supplementalRoom(218, 7, "Gamehall Lobby", "entryhall", "hh_room_gamehall", 40,
                "Pit your wits on the battlefield, the board or the baize - choose wisely!"));
        rooms.add(supplementalRoom(219, 7, "Noughts & Crosses", "halla", "hh_room_gamehall", 20,
                "It's one-on-one for five in a row."));
        rooms.add(supplementalRoom(220, 7, "Battleships", "hallb", "hh_room_gamehall", 20,
                "Call your shots and sink the enemy fleet."));
        rooms.add(supplementalRoom(221, 7, "Chess", "hallc", "hh_room_gamehall", 20, "Are you the new Deep Blue?"));
        rooms.add(supplementalRoom(222, 7, "Poker", "halld", "hh_room_gamehall", 20,
                "Get a hand like a foot? Keep a straight face and bluff it out."));
        rooms.add(supplementalRoom(223, 8, "Imperial Park", "gate_park", "hh_room_gate_park", 35, "Follow your path..."));
        rooms.add(supplementalRoom(224, 8, "Imperial Park II", "gate_park_2", "hh_room_gate_park", 35,
                "Follow your path..."));
        rooms.add(supplementalRoom(225, 9, "Habburgers", "habburger", "hh_room_habburger", 20, "Get food here!"));
        rooms.add(supplementalRoom(226, 9, "Ice Cafe", "ice_cafe", "hh_room_icecafe", 20, "Come here. And chill out."));
        rooms.add(supplementalRoom(227, 9, "My Habbo Home Netcafe", "netcafe", "hh_room_netcafe", 20,
                "Learn a foreign language and win Habbo Credits in our quests!"));
        rooms.add(supplementalRoom(228, 7, "Old Skool Habbo", "old_skool0", "hh_room_old_skool", 30,
                "A set of rooms inspired by the original and legendary Mobiles Disco, the progenitor of Habbo."));
        rooms.add(supplementalRoom(229, 7, "Old Skool Habbo II", "old_skool1", "hh_room_old_skool", 30,
                "A set of rooms inspired by the original and legendary Mobiles Disco, the progenitor of Habbo."));
        rooms.add(supplementalRoom(230, 9, "Club Golden Dragon", "orient", "hh_room_orient", 25,
                "Tres chic with an Eastern twist. For Habbo Club members only."));
        rooms.add(supplementalRoom(231, 9, "Rooftop Cafe", "rooftop", "hh_room_rooftop", 25,
                "Hang out on the very rooftop of Habbo Hotel!"));
        rooms.add(supplementalRoom(232, 5, "Rooftop Rumble II", "rooftop_2", "hh_room_rooftop", 25, "Are you ready?"));
        rooms.add(supplementalRoom(233, 9, "Ten Forward", "space_cafe", "hh_room_space_cafe", 25,
                "In this space noone can see you ask for a soda!"));
        rooms.add(supplementalRoom(234, 7, "The Power Gym", "sport", "hh_room_sport", 25, "Get a solid work out!"));
        rooms.add(supplementalRoom(235, 9, "Star Lounge", "star_lounge", "hh_room_starlounge", 25,
                "Chat with Sean Kingston here!"));
        rooms.add(supplementalRoom(236, 5, "Sun Terrace", "sun_terrace", "hh_room_sun_terrace", 25,
                "For lazy afternoons and super tan treatment!"));
        rooms.add(supplementalRoom(237, 9, "Chinese Tea Room", "tearoom", "hh_room_tearoom", 20,
                "A soothing atmosphere and amazing tea brews."));
        rooms.add(supplementalRoom(238, 7, "MuchMusic HQ", "tv_studio", "hh_room_tv_studio", 30, "Sponsored by Bobbanet.com."));

        addTvStudioVariants(rooms, 239);
        addTheatredromeVariants(rooms, 242);
        addFloorLobbies(rooms, 251);
        addHallways(rooms, 254);

        return List.copyOf(rooms);
    }

    /**
     * Adds tv studio variants.
     * @param rooms the rooms value
     * @param startingId the starting id value
     */
    private static void addTvStudioVariants(List<SupplementalPublicRoomSeed> rooms, int startingId) {
        rooms.add(supplementalRoom(startingId, 7, "TV Studio", "tv_studio", "hh_room_tv_studio_general", 30,
                "Lights, camera, action!"));
        rooms.add(supplementalRoom(startingId + 1, 7, "The Box Studio", "tv_studio", "hh_room_tv_studio_thebox", 30,
                "Live music and backstage buzz around the clock."));
        rooms.add(supplementalRoom(startingId + 2, 7, "VIVA Studio", "tv_studio", "hh_room_tv_studio_viva", 30,
                "Hit the studio floor and hang out with music fans."));
    }

    /**
     * Adds theatredrome variants.
     * @param rooms the rooms value
     * @param startingId the starting id value
     */
    private static void addTheatredromeVariants(List<SupplementalPublicRoomSeed> rooms, int startingId) {
        rooms.add(supplementalRoom(startingId, 7, "Theatredrome", "theater", "hh_room_theater", 40,
                "I still miss Ralph, I really really do."));
        rooms.add(supplementalRoom(startingId + 1, 7, "Theatredrome Easter", "theater", "hh_room_theater_easter", 40,
                "Springtime takes over the stage."));
        rooms.add(supplementalRoom(startingId + 2, 7, "Theatredrome Habboween", "theater",
                "hh_room_theater_halloween", 40, "Warm welcome to Bullet For My Valentine!"));
        rooms.add(supplementalRoom(startingId + 3, 7, "Theatredrome Habbowood", "theater",
                "hh_room_theater_hbowood", 40, "Home to the Habbowood Gala and HAFTA Awards!"));
        rooms.add(supplementalRoom(startingId + 4, 7, "Theatredrome Valentine", "theater",
                "hh_room_theater_valentine", 40, "Hug a friend, it's Valentine's!"));
        rooms.add(supplementalRoom(startingId + 5, 7, "Theatredrome Xmas", "theater", "hh_room_theater_xmas", 40,
                "Seasonal shows and snowy stage lights."));
        rooms.add(supplementalRoom(startingId + 6, 7, "Theatredrome Deli", "theater", "hh_room_theater_deli", 40,
                "A fresh stage set for food and fun."));
        rooms.add(supplementalRoom(startingId + 7, 7, "Theatredrome Samsung", "theater",
                "hh_room_theater_samsung", 40, "A sleek sponsor take on the classic stage."));
        rooms.add(supplementalRoom(startingId + 8, 7, "Theatredrome AXE", "theater", "hh_room_theater_axe", 40,
                "A special AXE takeover at the Theatredrome."));
    }

    /**
     * Adds floor lobbies.
     * @param rooms the rooms value
     * @param startingId the starting id value
     */
    private static void addFloorLobbies(List<SupplementalPublicRoomSeed> rooms, int startingId) {
        String[] models = {"floorlobby_a", "floorlobby_b", "floorlobby_c"};
        for (int index = 0; index < models.length; index++) {
            rooms.add(supplementalRoom(
                    startingId + index,
                    3,
                    FLOOR_LOBBY_NAMES.get(index),
                    models[index],
                    "hh_room_floorlobbies",
                    35,
                    "A quiet stop between the public floors."
            ));
        }
    }

    /**
     * Adds hallways.
     * @param rooms the rooms value
     * @param startingId the starting id value
     */
    private static void addHallways(List<SupplementalPublicRoomSeed> rooms, int startingId) {
        for (int index = 0; index < ROMAN_NUMERALS.size(); index++) {
            String description = index == 1 ? "Beware witches and warlocks." : GENERIC_HALLWAY_DESCRIPTION;
            rooms.add(supplementalRoom(
                    startingId + index,
                    3,
                    "Hallway " + ROMAN_NUMERALS.get(index),
                    "hallway" + index,
                    "hh_room_hallway",
                    35,
                    description
            ));
        }
    }

    /**
     * Supplementals room.
     * @param id the id value
     * @param categoryId the category id value
     * @param name the name value
     * @param unitStrId the unit str id value
     * @param casts the casts value
     * @param maxUsers the max users value
     * @param description the description value
     * @return the result of this operation
     */
    private static SupplementalPublicRoomSeed supplementalRoom(
            int id,
            int categoryId,
            String name,
            String unitStrId,
            String casts,
            int maxUsers,
            String description
    ) {
        return new SupplementalPublicRoomSeed(
                id,
                categoryId,
                name,
                normalize(unitStrId),
                casts,
                maxUsers,
                description
        );
    }

    /**
     * Lookses like text key.
     * @param value the value value
     * @return the result of this operation
     */
    private static boolean looksLikeTextKey(String value) {
        return value.indexOf(' ') < 0 && value.contains("_");
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
        /**
         * Returns whether public room.
         * @return whether public room
         */
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

    private record SupplementalPublicRoomSeed(
            int id,
            int categoryId,
            String name,
            String unitStrId,
            String casts,
            int maxUsers,
            String description
    ) {}
}
