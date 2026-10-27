package com.handfont.app

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.*

/** Writes a minimal TrueType font. Each pen stroke becomes overlapping rectangles + octagons. */
object TtfBuilder {
    const val HALF = 32f   // half pen width in font units (em = 1000)

    private class Glyph(
        val contours: List<IntArray>, val advance: Int,
        val xMin: Int, val yMin: Int, val xMax: Int, val yMax: Int
    ) {
        val points get() = contours.sumOf { it.size / 2 }
    }

    private fun empty(adv: Int) = Glyph(emptyList(), adv, 0, 0, 0, 0)

    private fun cw(p: FloatArray): FloatArray {
        val n = p.size / 2
        var a = 0f
        for (i in 0 until n) {
            val j = (i + 1) % n
            a += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1]
        }
        if (a <= 0f) return p
        val r = FloatArray(p.size)
        for (i in 0 until n) {
            r[i * 2] = p[(n - 1 - i) * 2]
            r[i * 2 + 1] = p[(n - 1 - i) * 2 + 1]
        }
        return r
    }

    private fun octagon(cx: Float, cy: Float): FloatArray {
        val r = HALF * 1.04f
        val o = FloatArray(16)
        for (k in 0 until 8) {
            val a = -k * PI / 4
            o[k * 2] = cx + (r * cos(a)).toFloat()
            o[k * 2 + 1] = cy + (r * sin(a)).toFloat()
        }
        return o
    }

    private fun rect(a: FloatArray, b: FloatArray): FloatArray? {
        val dx = b[0] - a[0]; val dy = b[1] - a[1]
        val len = hypot(dx, dy)
        if (len < 0.5f) return null
        val nx = -dy / len * HALF; val ny = dx / len * HALF
        return cw(floatArrayOf(
            a[0] + nx, a[1] + ny, b[0] + nx, b[1] + ny,
            b[0] - nx, b[1] - ny, a[0] - nx, a[1] - ny))
    }

    private fun makeGlyph(strokes: List<List<Pt>>): Glyph {
        val raw = ArrayList<FloatArray>()
        for (st in strokes) {
            val pts = ArrayList<FloatArray>()
            for (p in st) {
                val x = p.x * 1000f
                val y = (0.75f - p.y) * 1000f
                val l = pts.lastOrNull()
                if (l == null || hypot(x - l[0], y - l[1]) >= 14f) pts.add(floatArrayOf(x, y))
            }
            if (pts.isEmpty()) continue
            val last = st.last()
            val lx = last.x * 1000f; val ly = (0.75f - last.y) * 1000f
            val pl = pts.last()
            if (hypot(lx - pl[0], ly - pl[1]) > 1f) pts.add(floatArrayOf(lx, ly))
            for (p in pts) raw.add(cw(octagon(p[0], p[1])))
            for (i in 0 until pts.size - 1) rect(pts[i], pts[i + 1])?.let { raw.add(it) }
        }
        if (raw.isEmpty()) return empty(500)
        var minX = Float.MAX_VALUE
        for (c in raw) for (i in c.indices step 2) minX = min(minX, c[i])
        val dx = 50f - minX
        val contours = raw.map { c ->
            IntArray(c.size) { k -> Math.round(if (k % 2 == 0) c[k] + dx else c[k]) }
        }
        var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = Int.MIN_VALUE; var y1 = Int.MIN_VALUE
        for (c in contours) for (i in c.indices step 2) {
            x0 = min(x0, c[i]); x1 = max(x1, c[i])
            y0 = min(y0, c[i + 1]); y1 = max(y1, c[i + 1])
        }
        return Glyph(contours, x1 + 50, x0, y0, x1, y1)
    }

    private fun glyphBytes(g: Glyph): ByteArray {
        if (g.contours.isEmpty()) return ByteArray(0)
        val o = ByteArrayOutputStream(); val d = DataOutputStream(o)
        d.writeShort(g.contours.size)
        d.writeShort(g.xMin); d.writeShort(g.yMin); d.writeShort(g.xMax); d.writeShort(g.yMax)
        var end = -1
        for (c in g.contours) { end += c.size / 2; d.writeShort(end) }
        d.writeShort(0)
        for (i in 0..end) d.writeByte(1)
        var px = 0
        for (c in g.contours) for (i in 0 until c.size / 2) { d.writeShort(c[i * 2] - px); px = c[i * 2] }
        var py = 0
        for (c in g.contours) for (i in 0 until c.size / 2) { d.writeShort(c[i * 2 + 1] - py); py = c[i * 2 + 1] }
        while (o.size() % 4 != 0) d.writeByte(0)
        return o.toByteArray()
    }

    private fun table(f: DataOutputStream.() -> Unit): ByteArray {
        val o = ByteArrayOutputStream()
        DataOutputStream(o).f()
        return o.toByteArray()
    }

    private fun checksum(b: ByteArray): Long {
        var sum = 0L
        var i = 0
        while (i < b.size) {
            var v = 0L
            for (k in 0 until 4) v = (v shl 8) or (if (i + k < b.size) (b[i + k].toLong() and 0xFF) else 0L)
            sum = (sum + v) and 0xFFFFFFFFL
            i += 4
        }
        return sum
    }

    fun build(family: String, input: Map<Char, List<List<Pt>>>): ByteArray {
        val codes = (input.keys.map { it.code } + 32).distinct().sorted()
        val glyphs = ArrayList<Glyph>()
        glyphs.add(empty(500))                       // .notdef
        for (code in codes) {
            glyphs.add(if (code == 32) empty(300) else makeGlyph(input[code.toChar()]!!))
        }
        val n = glyphs.size
        val gdata = glyphs.map { glyphBytes(it) }
        val glyf = table { gdata.forEach { write(it) } }
        val loca = table {
            var off = 0
            for (g in gdata) { writeInt(off); off += g.size }
            writeInt(off)
        }
        val hmtx = table { glyphs.forEach { writeShort(it.advance); writeShort(it.xMin) } }

        var bx0 = 0; var by0 = -300; var bx1 = 1000; var by1 = 800
        for (g in glyphs) if (g.contours.isNotEmpty()) {
            bx0 = min(bx0, g.xMin); by0 = min(by0, g.yMin); bx1 = max(bx1, g.xMax); by1 = max(by1, g.yMax)
        }
        val maxAdv = glyphs.maxOf { it.advance }
        val maxPts = glyphs.maxOf { it.points }
        val maxCont = glyphs.maxOf { it.contours.size }

        val head = table {
            writeInt(0x00010000); writeInt(0x00010000); writeInt(0); writeInt(0x5F0F3CF5)
            writeShort(3); writeShort(1000); writeLong(0); writeLong(0)
            writeShort(bx0); writeShort(by0); writeShort(bx1); writeShort(by1)
            writeShort(0); writeShort(8); writeShort(2); writeShort(1); writeShort(0)
        }
        val hhea = table {
            writeInt(0x00010000); writeShort(800); writeShort(-250); writeShort(0)
            writeShort(maxAdv); writeShort(0); writeShort(0); writeShort(maxAdv)
            writeShort(1); writeShort(0); writeShort(0)
            repeat(4) { writeShort(0) }
            writeShort(0); writeShort(n)
        }
        val maxp = table {
            writeInt(0x00010000); writeShort(n); writeShort(maxPts); writeShort(maxCont)
            writeShort(0); writeShort(0); writeShort(2)
            repeat(8) { writeShort(0) }
        }
        val post = table {
            writeInt(0x00030000); writeInt(0); writeShort(-100); writeShort(50)
            writeInt(0); repeat(4) { writeInt(0) }
        }
        val os2 = table {
            writeShort(4); writeShort(500); writeShort(400); writeShort(5); writeShort(0)
            writeShort(650); writeShort(600); writeShort(0); writeShort(75)
            writeShort(650); writeShort(600); writeShort(0); writeShort(350)
            writeShort(50); writeShort(300); writeShort(0)
            repeat(10) { writeByte(0) }
            writeInt(1); writeInt(0); writeInt(0); writeInt(0)
            write("HAND".toByteArray(Charsets.US_ASCII))
            writeShort(0x40); writeShort(32); writeShort(min(codes.last(), 0xFFFF))
            writeShort(800); writeShort(-250); writeShort(0); writeShort(900); writeShort(300)
            writeInt(1); writeInt(0)
            writeShort(500); writeShort(700); writeShort(0); writeShort(32); writeShort(0)
        }
        val seg = codes.size + 1
        val lg = 31 - Integer.numberOfLeadingZeros(seg)
        val searchRange = 2 * (1 shl lg)
        val cmap = table {
            writeShort(0); writeShort(1); writeShort(3); writeShort(1); writeInt(12)
            writeShort(4); writeShort(16 + 8 * seg); writeShort(0)
            writeShort(seg * 2); writeShort(searchRange); writeShort(lg); writeShort(seg * 2 - searchRange)
            codes.forEach { writeShort(it) }; writeShort(0xFFFF)
            writeShort(0)
            codes.forEach { writeShort(it) }; writeShort(0xFFFF)
            codes.forEachIndexed { i, c -> writeShort((i + 1 - c) and 0xFFFF) }; writeShort(1)
            repeat(seg) { writeShort(0) }
        }
        val ps = family.filter { it.isLetterOrDigit() }.ifEmpty { "MyHandwriting" }
        val names = listOf(1 to family, 2 to "Regular", 3 to "$ps-1.0", 4 to family,
            5 to "Version 1.0", 6 to ps)
        val strs = names.map { it.second.toByteArray(Charsets.UTF_16BE) }
        val name = table {
            writeShort(0); writeShort(names.size); writeShort(6 + 12 * names.size)
            var off = 0
            names.forEachIndexed { i, nm ->
                writeShort(3); writeShort(1); writeShort(0x409); writeShort(nm.first)
                writeShort(strs[i].size); writeShort(off); off += strs[i].size
            }
            strs.forEach { write(it) }
        }

        val tables = linkedMapOf(
            "OS/2" to os2, "cmap" to cmap, "glyf" to glyf, "head" to head, "hhea" to hhea,
            "hmtx" to hmtx, "loca" to loca, "maxp" to maxp, "name" to name, "post" to post)
        val nt = tables.size
        val sr = 16 * (1 shl (31 - Integer.numberOfLeadingZeros(nt)))
        val out = ByteArrayOutputStream(); val d = DataOutputStream(out)
        d.writeInt(0x00010000); d.writeShort(nt); d.writeShort(sr)
        d.writeShort(31 - Integer.numberOfLeadingZeros(nt)); d.writeShort(nt * 16 - sr)
        var off = 12 + 16 * nt
        var headOff = 0
        for ((tag, b) in tables) {
            if (tag == "head") headOff = off
            d.write(tag.toByteArray(Charsets.US_ASCII))
            d.writeInt(checksum(b).toInt()); d.writeInt(off); d.writeInt(b.size)
            off += (b.size + 3) / 4 * 4
        }
        for ((_, b) in tables) {
            d.write(b)
            repeat((4 - b.size % 4) % 4) { d.writeByte(0) }
        }
        val bytes = out.toByteArray()
        val adj = (0xB1B0AFBAL - checksum(bytes)) and 0xFFFFFFFFL
        for (k in 0 until 4) bytes[headOff + 8 + k] = (adj shr (24 - 8 * k)).toByte()
        return bytes
    }
}
