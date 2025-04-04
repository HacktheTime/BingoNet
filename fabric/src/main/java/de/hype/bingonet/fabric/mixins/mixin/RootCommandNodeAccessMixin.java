package de.hype.bingonet.fabric.mixins.mixin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import de.hype.bingonet.fabric.mixins.mixinaccessinterfaces.IBingoNetCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(RootCommandNode.class)
public abstract class RootCommandNodeAccessMixin<S> extends CommandNodeAccessMixin<S> implements de.hype.bingonet.fabric.mixins.mixinaccessinterfaces.IRootCommandNodeMixinAccess<S> {

    @Unique
    @Override
    public void BingoNet$replaceNodes(List<LiteralArgumentBuilder<IBingoNetCommandSource>> newNodes) {
        for (LiteralArgumentBuilder<IBingoNetCommandSource> newNode : newNodes) {
            if (BingoNet$removeNode(newNode.getLiteral()) != null)
                addChild((CommandNode<S>) (Object) newNode.build());
        }
    }
}

