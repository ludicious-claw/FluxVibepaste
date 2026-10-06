package ru.kirka.fluxclient.feature.impl.render.esp;

import net.minecraft.entity.Entity;

public record EspTarget(Entity entity, EspTargetType type, EspPlayerType playerType, EspItemType itemType) {
}
