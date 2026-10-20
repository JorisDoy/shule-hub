package ke.shulehub.academics;

import java.util.UUID;

import ke.shulehub.shared.school.LevelBand;

public record LearningAreaInfo(UUID id, String code, String name, LevelBand levelBand, int sortOrder) {
}
