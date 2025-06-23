package de.hype.bingonet.fabric

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import net.minecraft.util.Util
import java.util.function.Function

object CustomRenderPipelines {
    val GUI_TEXTURED_NO_DEPTH_TRIS =
        RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withLocation(identifier("gui_textured_overlay_tris"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withDepthWrite(false)
            .build()
    val OMNIPRESENT_LINES = RenderPipeline
        .builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
        .withLocation(identifier("lines"))
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()
    val COLORED_OMNIPRESENT_QUADS =
        RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)// TODO: split this up to support better transparent ordering.
            .withLocation(identifier("colored_omnipresent_quads"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withDepthWrite(false)
            .build()

    val CIRCLE_FILTER_TRANSLUCENT_GUI_TRIS =
        RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withLocation(("gui_textured_overlay_tris_circle"))
            .withUniform("InnerCutoutRadius", UniformType.FLOAT)
            .withFragmentShader(identifier("circle_discard_color"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .build()

    fun identifier(name: String): Identifier {
        return Identifier.of("bingonet", name)
    }
}

object CustomRenderLayers {
    inline fun memoizeTextured(crossinline func: (Identifier) -> RenderLayer) = memoize(func)
    inline fun <T, R> memoize(crossinline func: (T) -> R): Function<T, R> {
        return Util.memoize { it: T -> func(it) }
    }

    val GUI_TEXTURED_NO_DEPTH_TRIS = memoizeTextured { texture ->
        RenderLayer.of(
            "firmament_gui_textured_overlay_tris",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            CustomRenderPipelines.GUI_TEXTURED_NO_DEPTH_TRIS,
            RenderLayer.MultiPhaseParameters.builder().texture(
                RenderPhase.Texture(texture, TriState.DEFAULT, false)
            )
                .build(false)
        )
    }
    val LINES = RenderLayer.of(
        "bingonet_firmament_lines",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        CustomRenderPipelines.OMNIPRESENT_LINES,
        RenderLayer.MultiPhaseParameters.builder() // TODO: accept linewidth here
            .build(false)
    )
    val COLORED_QUADS = RenderLayer.of(
        "bingonet_firmament_quads",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        CustomRenderPipelines.COLORED_OMNIPRESENT_QUADS,
        RenderLayer.MultiPhaseParameters.builder()
            .lightmap(RenderPhase.DISABLE_LIGHTMAP)
            .build(false)
    )

    val TRANSLUCENT_CIRCLE_GUI =
        RenderLayer.of(
            "bingonet_firmament_circle_gui",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            CustomRenderPipelines.CIRCLE_FILTER_TRANSLUCENT_GUI_TRIS,
            RenderLayer.MultiPhaseParameters.builder()
                .build(false)
        )
}
