package com.urduocr.scanner.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AlignmentSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.urduocr.scanner.ImageSizeType
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.FragmentEditBinding
import java.io.File
import java.io.FileOutputStream

class EditFragment : Fragment() {

    /* ======================================================= binding & state */
    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrike = false
    private var selectedAlignment: View? = null

    /** user‑picked canvas background (default white) */
    private var canvasBgColor: Int = Color.WHITE

    /* ======================================================= lifecycle */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEvents()
        setupFormattingButtons()
        setupAlignmentButtons()
        binding.btnGenerateFile.setOnClickListener { showSizePickerDialog() }
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    /* ======================================================= Size dialog */
    private fun showSizePickerDialog() {
        val v      = layoutInflater.inflate(R.layout.text_to_image_dailoug, null)
        val btn1   = v.findViewById<AppCompatButton>(R.id.btnSize1_1)
        val btn2   = v.findViewById<AppCompatButton>(R.id.btnSize2_3)
        val btn3   = v.findViewById<AppCompatButton>(R.id.btnSize3_2)
        val btnA4  = v.findViewById<AppCompatButton>(R.id.btnSizeA4)
        val etName = v.findViewById<EditText>(R.id.etFileName)
        val btnSave   = v.findViewById<TextView>(R.id.btnSave)
        val btnCancel = v.findViewById<TextView>(R.id.btnCancle)

        /* default file name */
        val defName = "image_${System.currentTimeMillis()}"
        etName.setText(defName); etName.setSelection(defName.length)

        /* size‑button highlight */
        var picked = ImageSizeType.SIZE_1_1
        val sizePairs = listOf(
            btn1 to ImageSizeType.SIZE_1_1,
            btn2 to ImageSizeType.SIZE_2_3,
            btn3 to ImageSizeType.SIZE_3_2,
            btnA4 to ImageSizeType.SIZE_A4
        )
        fun highlightSize(selectedBtn: AppCompatButton) {
            sizePairs.forEach { (btn, _) ->
                val selected = btn == selectedBtn
                btn.setBackgroundResource(
                    if (selected) R.drawable.bg_size_selected
                    else          R.drawable.bg_size_unselected
                )
                btn.setTextColor(
                    ContextCompat.getColor(requireContext(),
                        if (selected) android.R.color.black else android.R.color.black)
                )
            }
        }

        sizePairs.forEach { (b, s) -> b.setOnClickListener { picked = s; highlightSize(b) } }
        highlightSize(btn1)

        /* Save / Cancel swap helper */
        val actionBtns = listOf(btnSave, btnCancel)
        fun swapActions(sel: TextView) {
            actionBtns.forEach { btn ->
                val filled = btn == sel
                btn.setBackgroundResource(if (filled) R.drawable.button_selector else R.drawable.unselected_button)
                btn.setTextColor(ContextCompat.getColor(requireContext(),
                    if (filled) android.R.color.white else android.R.color.black))
            }
        }
        swapActions(btnSave)   // default

        val dlg = AlertDialog.Builder(requireContext()).setView(v).setCancelable(false).create()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        /* ——— listeners with 120 ms visual delay ——— */
        val delayMs = 120L

        btnSave.setOnClickListener {
            swapActions(btnSave)                 // show filled style
            btnSave.postDelayed({
                val raw = etName.text.toString().trim()
                if (raw.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter file name", Toast.LENGTH_SHORT).show()
                    return@postDelayed
                }
                val fileName = if (raw.lowercase().endsWith(".png")) raw else "$raw.png"
                val txt = binding.etEditText.text
                if (txt.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Text is empty!", Toast.LENGTH_SHORT).show()
                    return@postDelayed
                }
                generateImageFromText(txt, picked, fileName)
                dlg.dismiss()
            }, delayMs)
        }

        btnCancel.setOnClickListener {
            swapActions(btnCancel)               // show filled style on Cancel
            btnCancel.postDelayed({ dlg.dismiss() }, delayMs)
        }

        dlg.show()
    }



    /* ======================================================= Render PNG */
    private fun generateImageFromText(text: CharSequence, size: ImageSizeType, fileName: String) {
        val (w,h)= when(size){
            ImageSizeType.SIZE_1_1->800 to 800
            ImageSizeType.SIZE_2_3->800 to 1200
            ImageSizeType.SIZE_3_2->1200 to 800
            ImageSizeType.SIZE_A4 ->1240 to 1754
        }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply{ color=binding.etEditText.currentTextColor; textSize=binding.etEditText.textSize; typeface =binding.etEditText.typeface }
        val layout = StaticLayout.Builder.obtain(text,0,text.length,paint,w).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).build()
        val bmp = Bitmap.createBitmap(w, maxOf(h, layout.height), Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply{ drawColor(canvasBgColor); layout.draw(this) }
        val file = File(requireContext().filesDir, fileName)
        FileOutputStream(file).use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)}
        Toast.makeText(requireContext(),"Saved ➜ ${file.absolutePath}",Toast.LENGTH_LONG).show()
    }

    /* ======================================================= Formatting */
    private fun setupFormattingButtons(){
        val g=ContextCompat.getColor(requireContext(),R.color.green1); val gr=ContextCompat.getColor(requireContext(),R.color.lightgreen)
        fun View.toggle(on:Boolean)=(this as? ImageView)?.setColorFilter(if(on)g else gr)
        binding.Blod.setOnClickListener{isBold=!isBold;binding.Blod.toggle(isBold);toggleSpan(StyleSpan(Typeface.BOLD),isBold)}
        binding.italic.setOnClickListener{isItalic=!isItalic;binding.italic.toggle(isItalic);toggleSpan(StyleSpan(Typeface.ITALIC),isItalic)}
        binding.underlin.setOnClickListener{isUnderline=!isUnderline;binding.underlin.toggle(isUnderline);toggleSpan(UnderlineSpan(),isUnderline)}
        binding.StrikrThrough.setOnClickListener{isStrike=!isStrike;binding.StrikrThrough.toggle(isStrike);toggleSpan(StrikethroughSpan(),isStrike)}
    }
    private fun toggleSpan(span:Any,add:Boolean){ val s=binding.etEditText.selectionStart; val e=binding.etEditText.selectionEnd; if(s>=0&&e>s){ val ed=binding.etEditText.text as Spannable; if(add)ed.setSpan(span,s,e,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) else ed.removeSpan(span)}}

    /* ======================================================= Alignment */
    private fun setupAlignmentButtons(){ val g=ContextCompat.getColor(requireContext(),R.color.green1); val map= mapOf(binding.leftwriting to Layout.Alignment.ALIGN_NORMAL,binding.centerwriting to Layout.Alignment.ALIGN_CENTER,binding.rightwriting to Layout.Alignment.ALIGN_OPPOSITE,binding.equalwriting to Layout.Alignment.ALIGN_NORMAL)
        map.forEach{(b,a)-> b.setOnClickListener{ selectedAlignment?.clearColorFilter(); (b as ImageView).setColorFilter(g); selectedAlignment=b; val s=binding.etEditText.selectionStart; val e=binding.etEditText.selectionEnd; if(s>=0&&e>s)(binding.etEditText.text as Spannable).setSpan(AlignmentSpan.Standard(a),s,e,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)}} }

    /* ======================================================= Misc events */
    private fun setEvents(){ binding.fontSpinner.setOnClickListener{ showFontMenu(it)}; binding.textColor.setOnClickListener{showFontColorMenu(it)}; binding.fontSizeSpinner.setOnClickListener{showFontSizeMenu(it)}; binding.canvasColor.setOnClickListener{showBackgroundMenu(it)} }

    /* ===== Popup menus (only background one changed to capture colour) */
    private fun showFontMenu(anchor:View){ val pop=PopupMenu(requireContext(),anchor).apply{menuInflater.inflate(R.menu.font_list_menu,menu)}; pop.setOnMenuItemClickListener{item-> binding.fontSpinner.text=item.title; binding.etEditText.typeface=when(item.itemId){ R.id.font_alvi->ResourcesCompat.getFont(requireContext(),R.font.alvi_nastaleeq_regular); R.id.font_jamil->ResourcesCompat.getFont(requireContext(),R.font.jameel_noori_nastaleeq); else->Typeface.DEFAULT}; true}; pop.show() }
    private fun showFontSizeMenu(anchor:View){ val pop=PopupMenu(requireContext(),anchor).apply{menuInflater.inflate(R.menu.font_size_menu,menu)}; pop.setOnMenuItemClickListener{item-> val fs=item.title.toString().toFloatOrNull()?:14f; binding.etEditText.textSize=fs; binding.fontSizeSpinner.text="$fs px"; true}; pop.show() }
    private fun showFontColorMenu(anchor:View){ val pop=PopupMenu(requireContext(),anchor).apply{menuInflater.inflate(R.menu.font_color_menu,menu)}; pop.setOnMenuItemClickListener{item-> val c=when(item.itemId){ R.id.font_black->0xFF000000.toInt(); R.id.font_red->0xFFFF0000.toInt(); R.id.font_blue->0xFF0000FF.toInt(); R.id.font_green->0xFF00FF00.toInt(); else->0xFF000000.toInt()}; binding.etEditText.setTextColor(c); binding.colorLine.imageTintList=ColorStateList.valueOf(c); true}; pop.show() }
    private fun showBackgroundMenu(anchor:View){ val pop=PopupMenu(requireContext(),anchor).apply{menuInflater.inflate(R.menu.font_color_menu,menu)}; pop.setOnMenuItemClickListener{item-> val c=when(item.itemId){ R.id.font_black->0xFF000000.toInt(); R.id.font_red->0xFFFF0000.toInt(); R.id.font_blue->0xFF0000FF.toInt(); R.id.font_green->0xFF00FF00.toInt(); else->Color.WHITE}; binding.etEditText.setBackgroundColor(c); binding.canvasColorLine.imageTintList=ColorStateList.valueOf(c); canvasBgColor=c; true}; pop.show() }

    /* ======================================================= util */
    private fun View.clearColorFilter(){ if(this is ImageView) colorFilter=null }
    private fun Int.dpToPx(ctx:Context)=(this*ctx.resources.displayMetrics.density).toInt()
}
