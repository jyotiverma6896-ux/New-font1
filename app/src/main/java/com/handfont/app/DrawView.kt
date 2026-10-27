package com.handfont.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

data class Pt(val x: Float, val y: Float)

class DrawView(ctx: Context) : View(ctx) {
    var strokes: ArrayList<ArrayList<Pt>> = ArrayList()
    private var current: ArrayList<Pt>? = null
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val grey = Paint().apply { color = 0xFFCCCCCC.toInt(); strokeWidth = 2f }
    private val red = Paint().apply { color = 0xFFE53935.toInt(); strokeWidth = 3f }

    override fun onMeasure(w: Int, h: Int) {
        val s = MeasureSpec.getSize(w)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val s = width.toFloat()
        c.drawColor(Color.WHITE)
        c.drawLine(0f, s * 0.05f, s, s * 0.05f, grey)   // cap height
        c.drawLine(0f, s * 0.25f, s, s * 0.25f, grey)   // x-height
        c.drawLine(0f, s * 0.75f, s, s * 0.75f, red)    // baseline
        pen.strokeWidth = s * TtfBuilder.HALF * 2f / 1000f
        for (st in strokes) {
            if (st.size == 1) {
                c.drawPoint(st[0].x * s, st[0].y * s, pen)
            } else {
                val p = Path()
                p.moveTo(st[0].x * s, st[0].y * s)
                for (i in 1 until st.size) p.lineTo(st[i].x * s, st[i].y * s)
                c.drawPath(p, pen)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = (e.x / width).coerceIn(0f, 1f)
        val y = (e.y / height).coerceIn(0f, 1f)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val st = arrayListOf(Pt(x, y))
                strokes.add(st); current = st
            }
            MotionEvent.ACTION_MOVE -> current?.add(Pt(x, y))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> current = null
        }
        invalidate()
        return true
    }
}
