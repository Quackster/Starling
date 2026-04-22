package org.oldskooler.vibe.storage.bootstrap.seed.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomModelSeedCatalogTest {

    @Test
    void roomModelsIncludeBattleBallAndSnowStormLobbySeeds() {
        assertTrue(RoomModelSeedCatalog.roomModels().stream()
                .anyMatch(model -> "bb_lobby_1".equals(model.modelName()) && model.isPublic() == 1));
        assertTrue(RoomModelSeedCatalog.roomModels().stream()
                .anyMatch(model -> "snowwar_lobby_1".equals(model.modelName()) && model.isPublic() == 1));
    }
}
