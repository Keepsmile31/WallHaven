package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

internal val LocalLiquidGlassMode = compositionLocalOf { "ultimate" }

/**
 * AGSL shaders and RenderEffect builders for experimental Liquid Glass refraction
 * and chromatic dispersion on Android 14+ (minSdk 34).
 */
internal object LiquidGlassShaders {

    /**
     * Rounded-capsule convex glass refraction + optional RGB chromatic dispersion.
     * Samples surrounding backdrop along the outward SDF normal near curved rims
     * to recreate physical liquid-glass edge bending and prismatic dispersion.
     */
    private const val LIQUID_GLASS_CAPSULE_AGSL = """
        uniform shader content;
        uniform float2 uResolution;
        uniform float uCornerRadius;
        uniform float uRefraction;
        uniform float uDispersion;
        uniform float uZoom;
        uniform float uEdgeBand;
        uniform float uSpecular;
        uniform half3 uFallbackColor;

        float sdRoundedRect(float2 p, float2 halfSize, float r) {
            float2 q = abs(p) - halfSize + float2(r);
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half3 samplePremulOverFallback(float2 coord) {
            float2 safe = clamp(coord, float2(1.0), max(uResolution - float2(1.0), float2(1.0)));
            half4 c = content.eval(safe);
            return clamp(c.rgb + uFallbackColor * (1.0 - c.a), half3(0.0), half3(1.0));
        }

        half4 main(float2 fragCoord) {
            float2 center = uResolution * 0.5;
            float2 halfSize = max(center - float2(1.0), float2(2.0));
            float r = min(uCornerRadius, min(halfSize.x, halfSize.y));
            float2 p = fragCoord - center;

            float d = sdRoundedRect(p, halfSize, r);
            float alphaMask = 1.0 - smoothstep(-1.2, 0.6, d);
            if (alphaMask <= 0.001) {
                return half4(0.0);
            }

            // Outward surface normal from SDF gradient
            float dx = sdRoundedRect(p + float2(1.5, 0.0), halfSize, r) - d;
            float dy = sdRoundedRect(p + float2(0.0, 1.5), halfSize, r) - d;
            float2 normal = normalize(float2(dx, dy) + float2(0.0001));

            float band = max(uEdgeBand, 2.0);
            float depth = clamp(-d / band, 0.0, 1.0);
            float rimFactor = pow(1.0 - depth, 2.15);

            // Subtle convex magnification near the center + outward Snell rim pull
            float2 zoomedCoord = center + p * (1.0 - uZoom * (1.0 - rimFactor * 0.35));
            float2 refractOffset = normal * (uRefraction * rimFactor);

            half3 baseG = samplePremulOverFallback(zoomedCoord + refractOffset);
            half3 rgb;
            if (uDispersion > 0.001) {
                float disp = uDispersion * rimFactor;
                half3 colR = samplePremulOverFallback(zoomedCoord + refractOffset * (1.0 + disp));
                half3 colB = samplePremulOverFallback(zoomedCoord + refractOffset * (1.0 - disp));
                rgb = half3(colR.r, baseG.g, colB.b);
            } else {
                rgb = baseG;
            }

            // Specular Fresnel highlight along top-left & bottom-right curved glass rim
            float fresnel = pow(1.0 - depth, 4.0);
            float specTop = max(dot(-normal, normalize(float2(-0.45, -0.89))), 0.0);
            float specBottom = max(dot(-normal, normalize(float2(0.45, 0.89))), 0.0);
            float spec = fresnel * uSpecular * (0.65 * specTop + 0.35 * specBottom);
            rgb = clamp(rgb + half3(spec), half3(0.0), half3(1.0));

            return half4(rgb * alphaMask, alphaMask);
        }
    """

    fun createBarShader(): RuntimeShader = RuntimeShader(LIQUID_GLASS_CAPSULE_AGSL)

    fun createLensShader(): RuntimeShader = RuntimeShader(LIQUID_GLASS_CAPSULE_AGSL)

    fun buildStandardBlurEffect(radiusPx: Float): ComposeRenderEffect? {
        if (radiusPx <= 0.5f) return null
        return runCatching {
            RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }.getOrNull()
    }

    fun buildUltimateCapsuleEffect(
        shader: RuntimeShader,
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        blurRadiusPx: Float,
        refractionPx: Float,
        dispersion: Float,
        zoom: Float,
        edgeBandPx: Float,
        specular: Float,
        fallbackColorR: Float = 1f,
        fallbackColorG: Float = 1f,
        fallbackColorB: Float = 1f
    ): ComposeRenderEffect? {
        if (widthPx < 4f || heightPx < 4f) return null
        return runCatching {
            shader.setFloatUniform("uResolution", widthPx, heightPx)
            shader.setFloatUniform("uCornerRadius", cornerRadiusPx)
            shader.setFloatUniform("uRefraction", refractionPx)
            shader.setFloatUniform("uDispersion", dispersion)
            shader.setFloatUniform("uZoom", zoom)
            shader.setFloatUniform("uEdgeBand", edgeBandPx)
            shader.setFloatUniform("uSpecular", specular)
            shader.setFloatUniform("uFallbackColor", fallbackColorR, fallbackColorG, fallbackColorB)

            val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
            val finalEffect = if (blurRadiusPx > 0.5f) {
                val blurEffect = RenderEffect.createBlurEffect(
                    blurRadiusPx,
                    blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
                RenderEffect.createChainEffect(shaderEffect, blurEffect)
            } else {
                shaderEffect
            }
            finalEffect.asComposeRenderEffect()
        }.getOrNull()
    }
}
