package com.handfont.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val chars = ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
            ".,!?-:;'\"()@&").toList()
    private val store = HashMap<Char, ArrayList<ArrayList<Pt>>>()
    private var index = 0
    private var pending: ByteArray? = null
    private lateinit var drawView: DrawView
    private lateinit var label: TextView
    private lateinit var nameBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }
        label = TextView(this).apply {
            textSize = 20f; gravity = Gravity.CENTER; setTextColor(Color.BLACK)
        }
        drawView = DrawView(this)
        nameBox = EditText(this).apply { hint = "Font name"; setText("MyHandwriting") }

        val row1 = row(
            btn("Prev") { go(-1) },
            btn("Undo") { undo() },
            btn("Clear") { clear() },
            btn("Next") { go(1) }
        )
        val export = Button(this).apply {
            text = "Export .ttf"
            setOnClickListener { export() }
        }
        root.addView(label)
        root.addView(drawView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(row1)
        root.addView(nameBox)
        root.addView(export)
        setContentView(ScrollView(this).apply { addView(root) })
        show()
    }

    private fun btn(t: String, f: () -> Unit) = Button(this).apply {
        text = t
        setOnClickListener { f() }
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun row(vararg v: android.view.View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        v.forEach { addView(it) }
    }

    private fun show() {
        val c = chars[index]
        label.text = "Likho: $c   (${index + 1}/${chars.size}, done: ${store.size})"
        drawView.strokes = store[c] ?: ArrayList()
        drawView.invalidate()
    }

    private fun saveCurrent() {
        val c = chars[index]
        if (drawView.strokes.isEmpty()) store.remove(c) else store[c] = drawView.strokes
    }

    private fun go(d: Int) {
        saveCurrent()
        index = (index + d + chars.size) % chars.size
        show()
    }

    private fun undo() {
        if (drawView.strokes.isNotEmpty()) drawView.strokes.removeAt(drawView.strokes.size - 1)
        drawView.invalidate()
    }

    private fun clear() {
        drawView.strokes = ArrayList()
        drawView.invalidate()
    }

    private fun export() {
        saveCurrent()
        if (store.isEmpty()) {
            Toast.makeText(this, "Pehle kuch letters likho", Toast.LENGTH_SHORT).show()
            return
        }
        val name = nameBox.text.toString().trim().ifEmpty { "MyHandwriting" }
        pending = TtfBuilder.build(name, store)
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "$name.ttf")
        }
        startActivityForResult(i, 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data
        if (req == 1 && res == RESULT_OK && uri != null) {
            contentResolver.openOutputStream(uri)?.use { it.write(pending!!) }
            Toast.makeText(this, "Font save ho gaya", Toast.LENGTH_LONG).show()
        }
    }
}
