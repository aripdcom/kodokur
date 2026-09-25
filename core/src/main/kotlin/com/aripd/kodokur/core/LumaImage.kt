package com.aripd.kodokur.core

/**
 * Gri tonlu (Y düzlemi) görüntü; satırlar arasında boşluk yok. Kamera karesi de,
 * galeriden seçilen görsel de çözücüye bu biçimde gelir.
 */
class LumaImage(val pixels: ByteArray, val width: Int, val height: Int) {

    init {
        require(width > 0 && height > 0 && pixels.size >= width * height) {
            "Geçersiz görüntü: ${width}x$height, ${pixels.size} bayt"
        }
    }

    /** Saat yönünde [degrees] (0, 90, 180, 270) döndürülmüş kopya; 0 ise kendisi. */
    fun rotated(degrees: Int): LumaImage {
        val d = ((degrees % 360) + 360) % 360
        if (d == 0) return this
        val out = ByteArray(width * height)
        when (d) {
            90 -> for (y in 0 until height) {
                val row = y * width
                for (x in 0 until width) out[x * height + (height - 1 - y)] = pixels[row + x]
            }
            180 -> for (i in 0 until width * height) out[width * height - 1 - i] = pixels[i]
            270 -> for (y in 0 until height) {
                val row = y * width
                for (x in 0 until width) out[(width - 1 - x) * height + y] = pixels[row + x]
            }
            else -> throw IllegalArgumentException("Dik açı değil: $degrees")
        }
        return if (d == 180) LumaImage(out, width, height) else LumaImage(out, height, width)
    }

    companion object {
        /** Satır adımı genişlikten büyük olabilen bir düzlemden sıkışık kopya. */
        fun fromPlane(plane: ByteArray, width: Int, height: Int, rowStride: Int): LumaImage {
            if (rowStride == width && plane.size >= width * height) return LumaImage(plane, width, height)
            val out = ByteArray(width * height)
            for (y in 0 until height) System.arraycopy(plane, y * rowStride, out, y * width, width)
            return LumaImage(out, width, height)
        }

        /** 0xAARRGGBB piksellerden parlaklık (BT.601 yaklaşık, tamsayı). */
        fun fromArgb(argb: IntArray, width: Int, height: Int): LumaImage {
            val out = ByteArray(width * height)
            for (i in out.indices) {
                val p = argb[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                out[i] = ((r * 77 + g * 150 + b * 29) shr 8).toByte()
            }
            return LumaImage(out, width, height)
        }
    }
}
