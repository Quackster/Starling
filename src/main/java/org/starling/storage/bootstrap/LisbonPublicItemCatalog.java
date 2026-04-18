package org.starling.storage.bootstrap;

import java.util.ArrayList;
import java.util.List;

public final class LisbonPublicItemCatalog {

    private static final String RESOURCE_PATH = "bootstrap/lisbon-public-items.sql";
    private static final LisbonPublicItemCatalog INSTANCE = loadCatalog();

    private final List<PublicRoomItemSeed> publicRoomItems;

    /**
     * Creates a new LisbonPublicItemCatalog.
     * @param publicRoomItems the public room items value
     */
    private LisbonPublicItemCatalog(List<PublicRoomItemSeed> publicRoomItems) {
        this.publicRoomItems = List.copyOf(publicRoomItems);
    }

    /**
     * Loads.
     * @return the resulting load
     */
    public static LisbonPublicItemCatalog load() {
        return INSTANCE;
    }

    /**
     * Publics room items.
     * @return the result of this operation
     */
    public List<PublicRoomItemSeed> publicRoomItems() {
        return publicRoomItems;
    }

    /**
     * Loads catalog.
     * @return the resulting load catalog
     */
    private static LisbonPublicItemCatalog loadCatalog() {
        String sql = BootstrapSqlSupport.readBundledSql(LisbonPublicItemCatalog.class, RESOURCE_PATH);
        List<List<String>> rows = BootstrapSqlSupport.parseInsertRows(sql, "public_items", RESOURCE_PATH);
        List<PublicRoomItemSeed> publicRoomItems = new ArrayList<>(rows.size());

        for (List<String> row : rows) {
            publicRoomItems.add(new PublicRoomItemSeed(
                    BootstrapSqlSupport.parseInt(row, 0),
                    BootstrapSqlSupport.normalize(BootstrapSqlSupport.parseString(row, 1)),
                    BootstrapSqlSupport.parseString(row, 2),
                    BootstrapSqlSupport.parseInt(row, 3),
                    BootstrapSqlSupport.parseInt(row, 4),
                    BootstrapSqlSupport.parseDouble(row, 5),
                    BootstrapSqlSupport.parseInt(row, 6),
                    BootstrapSqlSupport.parseDouble(row, 7),
                    BootstrapSqlSupport.parseInt(row, 8),
                    BootstrapSqlSupport.parseInt(row, 9),
                    BootstrapSqlSupport.parseString(row, 10),
                    BootstrapSqlSupport.defaultString(BootstrapSqlSupport.parseNullableString(row, 11)),
                    BootstrapSqlSupport.parseNullableString(row, 12),
                    BootstrapSqlSupport.parseNullableString(row, 13)
            ));
        }

        return new LisbonPublicItemCatalog(publicRoomItems);
    }

    public record PublicRoomItemSeed(
            int id,
            String roomModel,
            String sprite,
            int x,
            int y,
            double z,
            int rotation,
            double topHeight,
            int length,
            int width,
            String behaviour,
            String currentProgram,
            String teleportTo,
            String swimTo
    ) {}
}
