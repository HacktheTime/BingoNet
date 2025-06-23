package de.hype.bingonet.fabric
// Credits go to nea89 for this (Firmanent)! Just slightly adapted by me

import com.mojang.blaze3d.systems.RenderSystem
import de.hype.bingonet.fabric.objects.WorldRenderLastEvent
import de.hype.bingonet.shared.objects.RenderInformation
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.*
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector3f
import java.awt.Color
import java.lang.Math.pow

/**
 * @author nea89o in Firmanent
 */
class RenderInWorldContext(
    val matrixStack: MatrixStack,
    private val camera: Camera,
    private val tickCounter: RenderTickCounter,
    val vertexConsumers: VertexConsumerProvider.Immediate,
) {

    fun block(blockPos: BlockPos, color: Int) {
        matrixStack.push()
        matrixStack.translate(blockPos.x.toFloat(), blockPos.y.toFloat(), blockPos.z.toFloat())
        buildCube(matrixStack.peek().positionMatrix, vertexConsumers.getBuffer(CustomRenderLayers.COLORED_QUADS), color)
        matrixStack.pop()
    }

    fun block(blockPos: BlockPos, color: Color) {
        block(blockPos, color.rgb)
    }

    enum class VerticalAlign {
        TOP, BOTTOM, CENTER;

        fun align(index: Int, count: Int): Float {
            return when (this) {
                CENTER -> (index - count / 2F) * (1 + getFontHeight())
                BOTTOM -> (index - count) * (1 + getFontHeight())
                TOP -> (index) * (1 + getFontHeight())
            }
        }
    }

    fun waypoint(position: BlockPos, vararg label: Text) {
        text(
            position.toCenterPos(),
            *label,
            Text.literal("§e${formatDistance(MinecraftClient.getInstance().player?.pos?.distanceTo(position.toCenterPos()) ?: 42069.0)}"),
            background = 0xAA202020.toInt()
        )
    }

    fun waypoint(position: BlockPos, color: Color, vararg label: Text) {
        text(
            position.toCenterPos(),
            *label,
            Text.literal("§e${formatDistance(MinecraftClient.getInstance().player?.pos?.distanceTo(position.toCenterPos()) ?: 42069.0)}"),
            background = color.rgb
        )
    }


    fun doWaypointIcon(position: Vec3d, textures: List<RenderInformation>, width: Int, height: Int) {
        renderTextures(
            position,
            textures.map { it.texturePath?.let { path -> Identifier.of(path) } },
            width,
            height,
            0.1f
        )
    }

    fun renderTextures(
        position: Vec3d,
        textures: List<Identifier?>,
        width: Int,
        height: Int,
        padding: Float
    ) {
        if (textures.isEmpty()) return
        val count = textures.size
        // padding applies only between textures, so only add it when there are > 1 texture
        val totalWidth = width * count + if (count > 1) padding * (count - 1) else 0f
        // center by subtracting half the total width, then add half the texture width so that each texture is centered
        val left = -totalWidth / 2 + width / 2.0
        for ((index, texture) in textures.withIndex()) {
            if (texture == null || texture.path.isEmpty()) continue
            // For each texture, apply the left offset and move the y coordinate up by 1
            val xOffset = left + index * (width + if (count > 1) padding else 0f)
            texture(
                position.add(xOffset.toDouble(), 0.0, 0.0),
                texture, width, height
            )
        }
    }

    fun formatDistance(distance: Double): String {
        if (distance < 10) return "%.1fm".format(distance)
        return "%dm".format(distance.toInt())
    }

    fun withFacingThePlayer(position: Vec3d, block: FacingThePlayerContext.() -> Unit) {
        matrixStack.push()
        matrixStack.translate(position.x, position.y, position.z)
        val actualCameraDistance = position.distanceTo(camera.pos)
        val distanceToMoveTowardsCamera = if (actualCameraDistance < 10) 0.0 else -(actualCameraDistance - 10.0)
        val vec = position.subtract(camera.pos).multiply(distanceToMoveTowardsCamera / actualCameraDistance)
        matrixStack.translate(vec.x, vec.y, vec.z)
        matrixStack.multiply(camera.rotation)
        matrixStack.scale(0.025F, -0.025F, 1F)

        FacingThePlayerContext(this).run(block)

        matrixStack.pop()
        vertexConsumers.drawCurrentLayer()
    }

    fun sprite(position: Vec3d, sprite: Sprite, width: Int, height: Int) {
        texture(
            position, sprite.atlasId, width, height, sprite.minU, sprite.minV, sprite.maxU, sprite.maxV
        )
    }

    fun texture(position: Vec3d, texture: Identifier, width: Int, height: Int) {
        texture(position, texture, width, height, 0.0f, 0.0f, 1.0f, 1.0f)
    }

    fun texture(
        position: Vec3d, texture: Identifier, width: Int, height: Int,
        u1: Float, v1: Float,
        u2: Float, v2: Float,
    ) {
        withFacingThePlayer(position) {
            texture(texture, width, height, u1, v1, u2, v2)
        }
    }


    fun text(
        position: Vec3d,
        vararg texts: Text,
        verticalAlign: VerticalAlign = VerticalAlign.CENTER,
        background: Int = 0x70808080
    ) {
        withFacingThePlayer(position) {
            text(*texts, verticalAlign = verticalAlign, background = background)
        }
    }

    fun tinyBlock(vec3d: Vec3d, size: Float, color: Int) {
        matrixStack.push()
        matrixStack.translate(vec3d.x, vec3d.y, vec3d.z)
        matrixStack.scale(size, size, size)
        matrixStack.translate(-.5, -.5, -.5)
        buildCube(matrixStack.peek().positionMatrix, vertexConsumers.getBuffer(CustomRenderLayers.COLORED_QUADS), color)
        matrixStack.pop()
        vertexConsumers.draw()
    }

    fun wireframeCube(blockPos: BlockPos, lineWidth: Float = 10F) {
        val buf = vertexConsumers.getBuffer(RenderLayer.LINES)
        matrixStack.push()
        // TODO: this does not render through blocks (or water layers) anymore
        RenderSystem.lineWidth(lineWidth / pow(camera.pos.squaredDistanceTo(blockPos.toCenterPos()), 0.25).toFloat())
        matrixStack.translate(blockPos.x.toFloat(), blockPos.y.toFloat(), blockPos.z.toFloat())
        buildWireFrameCube(matrixStack.peek(), buf)
        matrixStack.pop()
        vertexConsumers.draw()
    }

    fun line(vararg points: Vec3d, lineWidth: Float = 10F, color: Color = Color.WHITE) {
        line(points.toList(), lineWidth, color)
    }

    fun tracer(toWhere: Vec3d, lineWidth: Float = 3f, color: Color = Color.WHITE) {
        val cameraForward = Vector3f(0f, 0f, -1f).rotate(camera.rotation)
        line(camera.pos.add(Vec3d(cameraForward)), toWhere, lineWidth = lineWidth, color = color)
    }

    fun line(points: List<Vec3d>, lineWidth: Float = 10F, color: Color = Color.WHITE) {
        RenderSystem.lineWidth(lineWidth)
        val buffer = vertexConsumers.getBuffer(CustomRenderLayers.LINES)

        val matrix = matrixStack.peek()
        var lastNormal: Vector3f? = null
        points.zipWithNext().forEach { (a, b) ->
            val normal =
                Vector3f(b.x.toFloat(), b.y.toFloat(), b.z.toFloat()).sub(a.x.toFloat(), a.y.toFloat(), a.z.toFloat())
                    .normalize()
            val lastNormal0 = lastNormal ?: normal
            lastNormal = normal
            buffer.vertex(matrix.positionMatrix, a.x.toFloat(), a.y.toFloat(), a.z.toFloat())
                .normal(matrix, lastNormal0.x, lastNormal0.y, lastNormal0.z).color(color.rgb).next()
            buffer.vertex(matrix.positionMatrix, b.x.toFloat(), b.y.toFloat(), b.z.toFloat())
                .normal(matrix, normal.x, normal.y, normal.z).color(color.rgb).next()
        }
    }
    // TODO: put the favourite icons in front of items again

    companion object {
        private fun doLine(
            matrix: MatrixStack.Entry,
            buf: VertexConsumer,
            i: Float,
            j: Float,
            k: Float,
            x: Float,
            y: Float,
            z: Float
        ) {
            val normal = Vector3f(x, y, z).sub(i, j, k).normalize()
            buf.vertex(matrix.positionMatrix, i, j, k).normal(matrix, normal.x, normal.y, normal.z).color(-1).next()
            buf.vertex(matrix.positionMatrix, x, y, z).normal(matrix, normal.x, normal.y, normal.z).color(-1).next()
        }


        private fun buildWireFrameCube(matrix: MatrixStack.Entry, buf: VertexConsumer) {
            for (i in 0..1) {
                for (j in 0..1) {
                    val i = i.toFloat()
                    val j = j.toFloat()
                    doLine(matrix, buf, 0F, i, j, 1F, i, j)
                    doLine(matrix, buf, i, 0F, j, i, 1F, j)
                    doLine(matrix, buf, i, j, 0F, i, j, 1F)
                }
            }
        }

        private fun buildCube(matrix: Matrix4f, buf: VertexConsumer, color: Int) {
            // Y-
            buf.vertex(matrix, 0F, 0F, 0F).color(color)
            buf.vertex(matrix, 0F, 0F, 1F).color(color)
            buf.vertex(matrix, 1F, 0F, 1F).color(color)
            buf.vertex(matrix, 1F, 0F, 0F).color(color)
            // Y+
            buf.vertex(matrix, 0F, 1F, 0F).color(color)
            buf.vertex(matrix, 1F, 1F, 0F).color(color)
            buf.vertex(matrix, 1F, 1F, 1F).color(color)
            buf.vertex(matrix, 0F, 1F, 1F).color(color)
            // X-
            buf.vertex(matrix, 0F, 0F, 0F).color(color)
            buf.vertex(matrix, 0F, 0F, 1F).color(color)
            buf.vertex(matrix, 0F, 1F, 1F).color(color)
            buf.vertex(matrix, 0F, 1F, 0F).color(color)
            // X+
            buf.vertex(matrix, 1F, 0F, 0F).color(color)
            buf.vertex(matrix, 1F, 1F, 0F).color(color)
            buf.vertex(matrix, 1F, 1F, 1F).color(color)
            buf.vertex(matrix, 1F, 0F, 1F).color(color)
            // Z-
            buf.vertex(matrix, 0F, 0F, 0F).color(color)
            buf.vertex(matrix, 1F, 0F, 0F).color(color)
            buf.vertex(matrix, 1F, 1F, 0F).color(color)
            buf.vertex(matrix, 0F, 1F, 0F).color(color)
            // Z+
            buf.vertex(matrix, 0F, 0F, 1F).color(color)
            buf.vertex(matrix, 0F, 1F, 1F).color(color)
            buf.vertex(matrix, 1F, 1F, 1F).color(color)
            buf.vertex(matrix, 1F, 0F, 1F).color(color)
        }


        fun renderInWorld(event: WorldRenderLastEvent, block: RenderInWorldContext. () -> Unit) {
            // TODO: there should be *no more global state*. the only thing we should be doing is render layers. that includes settings like culling, blending, shader color, and depth testing
            // For now i will let these functions remain, but this needs to go before i do a full (non-beta) release
            event.matrices.push()
            event.matrices.translate(-event.camera.pos.x, -event.camera.pos.y, -event.camera.pos.z)

            val ctx = RenderInWorldContext(
                event.matrices,
                event.camera,
                event.tickCounter,
                event.vertexConsumers
            )

            block(ctx)

            event.matrices.pop()
            event.vertexConsumers.draw()
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F)
        }
    }

}

/**
 * @author nea89o in Firmanent
 */
class FacingThePlayerContext(val worldContext: RenderInWorldContext) {
    val matrixStack by worldContext::matrixStack


    fun formatDistance(distance: Double): String {
        if (distance < 10) return "%.1fm".format(distance)
        return "%dm".format(distance.toInt())
    }

    fun text(
        vararg texts: Text,
        verticalAlign: RenderInWorldContext.VerticalAlign = RenderInWorldContext.VerticalAlign.CENTER,
        background: Int = 0x70808080,
    ) {
        if (!texts.isNotEmpty()) {
            return@text
        }
        for ((index, text) in texts.withIndex()) {
            worldContext.matrixStack.push()
            val width = MinecraftClient.getInstance().textRenderer.getWidth(text)
            worldContext.matrixStack.translate(-width / 2F, verticalAlign.align(index, texts.size), 0F)
            val vertexConsumer: VertexConsumer =
                worldContext.vertexConsumers.getBuffer(RenderLayer.getTextBackgroundSeeThrough())
            val matrix4f = worldContext.matrixStack.peek().positionMatrix
            vertexConsumer.vertex(matrix4f, -1.0f, -1.0f, 0.0f).color(background)
                .light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).next()
            vertexConsumer.vertex(matrix4f, -1.0f, getFontHeight(), 0.0f).color(background)
                .light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).next()
            vertexConsumer.vertex(matrix4f, width.toFloat(), getFontHeight(), 0.0f).color(background)
                .light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).next()
            vertexConsumer.vertex(matrix4f, width.toFloat(), -1.0f, 0.0f).color(background)
                .light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE).next()
            worldContext.matrixStack.translate(0F, 0F, 0.01F)

            MinecraftClient.getInstance().textRenderer.draw(
                text,
                0F,
                0F,
                -1,
                false,
                worldContext.matrixStack.peek().positionMatrix,
                worldContext.vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            )
            worldContext.matrixStack.pop()
        }
    }

    fun texture(
        texture: Identifier, width: Int, height: Int,
        u1: Float, v1: Float,
        u2: Float, v2: Float,
    ) {
        val buf = worldContext.vertexConsumers.getBuffer(RenderLayer.getGuiTexturedOverlay(texture))
        val hw = width / 2F
        val hh = height / 2F
        val matrix4f: Matrix4f = worldContext.matrixStack.peek().positionMatrix
        buf.vertex(matrix4f, -hw, -hh, 0F)
            .color(-1)
            .texture(u1, v1).next()
        buf.vertex(matrix4f, -hw, +hh, 0F)
            .color(-1)
            .texture(u1, v2).next()
        buf.vertex(matrix4f, +hw, +hh, 0F)
            .color(-1)
            .texture(u2, v2).next()
        buf.vertex(matrix4f, +hw, -hh, 0F)
            .color(-1)
            .texture(u2, v1).next()
        worldContext.vertexConsumers.draw()
    }

}

fun VertexConsumer.next() = this


fun getFontHeight(): Float {
    return MinecraftClient.getInstance().textRenderer.fontHeight.toFloat()
}