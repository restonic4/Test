package com.chaotic_loom.game.world.components;

import com.chaotic_loom.game.core.Loggers;
import com.chaotic_loom.game.registries.built_in.Blocks;

import java.util.Objects;

public class BlockInstance {
    private final Block block;
    private final Block.Direction direction;

    public BlockInstance(Block block, Block.Direction direction) {
        // Basic validation: Ensure the chosen direction is allowed for this block type
        if (block != null && direction != null && !block.getSettings().isDirectionAllowed(direction)) {
            // Default to the first allowed direction or North if validation fails
            this.direction = block.getSettings().getAllowedDirections().isEmpty() ?
                    Block.Direction.NORTH :
                    block.getSettings().getAllowedDirections().iterator().next();

            Loggers.OTHER.error("Warning: Tried to set block {} with disallowed direction {}. Defaulting to {}", block.getIdentifier(), direction, this.direction);
        } else {
            this.direction = direction != null ? direction : Block.Direction.NORTH; // Default null directions
        }

        this.block = block != null ? block : Blocks.AIR; // Default null blocks to Air
    }

    public Block getBlock() {
        return block;
    }

    public Block.Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockInstance that = (BlockInstance) o;
        return Objects.equals(block, that.block) && direction == that.direction;
    }

    @Override
    public String toString() {
        return "BlockInstance{" +
                "block=" + (block != null ? block.getIdentifier() : "null") +
                ", direction=" + direction +
                '}';
    }
}
