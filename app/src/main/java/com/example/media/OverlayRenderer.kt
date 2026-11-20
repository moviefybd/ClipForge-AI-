package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.SubtitleAlign
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyle
import com.example.model.WatermarkConfig
import com.example.model.WatermarkType
import com.example.model.WatermarkWeight
import kotlin.math.roundToInt

object OverlayRenderer {

    fun parseColorSafe(hex: String, fallback: Int = Color.WHITE): Int {
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }

    fun renderWatermarkBitmap(
        context: Context,
        config: WatermarkConfig,
        outputWidth: Int = 1080
    ): Bitmap? {
        if (!config.enabled) return null
        val scaleFactor = outputWidth / 360f

        return when (config.type) {
            WatermarkType.TEXT -> {
                val text = config.text.trim()
                if (text.isEmpty()) return null

                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = config.fontSizeSp * scaleFactor
                    color = parseColorSafe(config.textColorHex, Color.WHITE)
                    alpha = (config.opacity.coerceIn(0.05f, 1f) * 255).roundToInt()
                    val baseTf = ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT
                    typeface = when (config.fontWeight) {
                        WatermarkWeight.NORMAL -> Typeface.create(baseTf, Typeface.NORMAL)
                        WatermarkWeight.MEDIUM -> Typeface.create(baseTf, Typeface.NORMAL)
                        WatermarkWeight.BOLD -> Typeface.create(baseTf, Typeface.BOLD)
                    }
                    setShadowLayer(4f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(140, 0, 0, 0))
                }

                val padH = (14f * scaleFactor).roundToInt()
                val padV = (8f * scaleFactor).roundToInt()
                val textWidth = paint.measureText(text).roundToInt().coerceAtLeast(1)
                val fm = paint.fontMetrics
                val textHeight = (fm.descent - fm.ascent).roundToInt().coerceAtLeast(1)

                val bmp = Bitmap.createBitmap(
                    textWidth + padH * 2,
                    textHeight + padV * 2,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bmp)
                canvas.drawText(text, padH.toFloat(), padV - fm.ascent, paint)
                bmp
            }

            WatermarkType.IMAGE -> {
                val uriStr = config.imageUriString ?: return null
                return runCatching {
                    val raw = context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    } ?: return@runCatching null

                    val targetPx = (config.imageSizeDp * scaleFactor).roundToInt().coerceIn(48, 520)
                    val ratio = raw.height.toFloat() / raw.width.toFloat().coerceAtLeast(1f)
                    val w = targetPx
                    val h = (targetPx * ratio).roundToInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                    if (scaled !== raw) raw.recycle()

                    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(output)
                    val alphaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (config.opacity.coerceIn(0.05f, 1f) * 255).roundToInt()
                    }
                    canvas.drawBitmap(scaled, 0f, 0f, alphaPaint)
                    scaled.recycle()
                    output
                }.getOrNull()
            }
        }
    }

    /**
     * Renders a full 1080x1920 transparent subtitle frame using StaticLayout,
     * supporting native HarfBuzz shaping for Bengali (বাংলা) and English scripts.
     */
    fun renderSubtitleOverlayBitmap(
        context: Context,
        text: String,
        style: SubtitleStyle,
        frameWidth: Int = 1080,
        frameHeight: Int = 1920
    ): Bitmap {
        val safeW = frameWidth.coerceAtLeast(16)
        val safeH = frameHeight.coerceAtLeast(16)
        val bmp = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        if (text.isBlank()) return bmp

        val canvas = Canvas(bmp)
        val scaleFactor = safeW / 360f
        val maxTextWidth = (safeW * 0.84f).roundToInt().coerceAtLeast(1)

        val typeface = when (style.fontFamily) {
            SubtitleFontFamily.SANS -> ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
            SubtitleFontFamily.DISPLAY -> Typeface.create(
                ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.SANS_SERIF,
                Typeface.BOLD
            )
            SubtitleFontFamily.MONO -> ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
            SubtitleFontFamily.SERIF -> Typeface.SERIF
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.fontSizeSp * scaleFactor
            color = parseColorSafe(style.textColorHex, Color.WHITE)
            this.typeface = typeface
            if (style.shadowEnabled) {
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(180, 0, 0, 0))
            }
        }

        val alignment = when (style.alignment) {
            SubtitleAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
            SubtitleAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            SubtitleAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxTextWidth)
            .setAlignment(alignment)
            .setLineSpacing(4f * scaleFactor, 1.1f)
            .setIncludePad(true)
            .build()

        val textLeft = (safeW - maxTextWidth) / 2f
        val textTop = when (style.position) {
            SubtitlePosition.TOP -> safeH * 0.14f
            SubtitlePosition.CENTER -> (safeH - layout.height) / 2f
            SubtitlePosition.BOTTOM -> safeH * 0.78f - layout.height
        }.coerceIn(48f, (safeH - layout.height - 48f).coerceAtLeast(48f))

        // Background pill
        if (layout.lineCount > 0 && style.backgroundAlpha > 0.02f) {
            var minLineLeft = Float.MAX_VALUE
            var maxLineRight = 0f
            for (i in 0 until layout.lineCount) {
                minLineLeft = minOf(minLineLeft, layout.getLineLeft(i))
                maxLineRight = maxOf(maxLineRight, layout.getLineRight(i))
            }
            val padX = 16f * scaleFactor
            val padY = 8f * scaleFactor
            val bgColor = parseColorSafe(style.backgroundColorHex, Color.parseColor("#10121A"))
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(
                    (style.backgroundAlpha.coerceIn(0f, 1f) * 255).roundToInt(),
                    Color.red(bgColor),
                    Color.green(bgColor),
                    Color.blue(bgColor)
                )
            }
            val rect = RectF(
                (textLeft + minLineLeft - padX).coerceAtLeast(24f),
                (textTop - padY).coerceAtLeast(24f),
                (textLeft + maxLineRight + padX).coerceAtMost(safeW - 24f),
                (textTop + layout.height + padY).coerceAtMost(safeH - 24f)
            )
            if (rect.right > rect.left && rect.bottom > rect.top) {
                canvas.drawRoundRect(rect, 14f * scaleFactor, 14f * scaleFactor, bgPaint)
            }
        }

        canvas.save()
        canvas.translate(textLeft, textTop)

        if (style.outlineEnabled) {
            val strokePaint = TextPaint(textPaint).apply {
                setStyle(Paint.Style.STROKE)
                strokeWidth = 3.5f * scaleFactor
                strokeJoin = Paint.Join.ROUND
                color = parseColorSafe(style.outlineColorHex, Color.BLACK)
                clearShadowLayer()
            }
            val strokeLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, strokePaint, maxTextWidth)
                .setAlignment(alignment)
                .setLineSpacing(4f * scaleFactor, 1.1f)
                .setIncludePad(true)
                .build()
            strokeLayout.draw(canvas)
        }

        layout.draw(canvas)
        canvas.restore()
        return bmp
    }

    /**
     * Generates a 1080x1920 blurred backdrop bitmap from a sample frame of the video
     * for Blur Background 9:16 composition mode.
     */
    fun createBlurredBackdropBitmap(
        sourceFrame: Bitmap?,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): Bitmap {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        if (sourceFrame != null && sourceFrame.width > 0 && sourceFrame.height > 0) {
            // Downsample sharply then box-blur for smooth fast backdrop without RenderScript deprecation
            val smallW = 72
            val smallH = 128
            val tiny = Bitmap.createScaledBitmap(sourceFrame, smallW, smallH, true)
            val blurredSmall = fastBoxBlur(tiny, radius = 6)
            tiny.recycle()

            val stretched = Bitmap.createScaledBitmap(blurredSmall, targetWidth, targetHeight, true)
            blurredSmall.recycle()
            canvas.drawBitmap(stretched, 0f, 0f, null)
            stretched.recycle()

            // Subtle dark studio scrim so foreground video pops
            val scrimPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, targetHeight.toFloat(),
                    Color.argb(125, 16, 18, 26),
                    Color.argb(150, 16, 18, 26),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), scrimPaint)
        } else {
            val fallbackPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(),
                    Color.parseColor("#191D29"),
                    Color.parseColor("#10121A"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), fallbackPaint)
        }
        return output
    }

    private fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
        val w = source.width
        val h = source.height
        val pix = IntArray(w * h)
        source.getPixels(pix, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val c = pix[ny * w + nx]
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                        count++
                    }
                }
                out[y * w + x] = Color.rgb(r / count, g / count, b / count)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }
}
