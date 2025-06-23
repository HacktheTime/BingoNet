package de.hype.bingonet.fabric.mixins.mixin;

// Credits for this code goes to Nea89o Firmanent. 2024 under MIT License


import com.llamalad7.mixinextras.sugar.Local;
import de.hype.bingonet.fabric.Utils;
import de.hype.bingonet.fabric.objects.WorldRenderLastEvent;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRenderLastEventPatch {
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", shift = At.Shift.AFTER))
    public void BingoNet$onWorldRenderLast(Fog fog, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Matrix4f matrix4f2, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        var imm = this.bufferBuilders.getEntityVertexConsumers();
        var stack = new MatrixStack();
        // TODO: pre-cancel this event if F1 is active
        var event = new WorldRenderLastEvent(
                stack, renderTickCounter,
                camera,
                imm
        );
        Utils.renderWaypoints(event);
    }
}
