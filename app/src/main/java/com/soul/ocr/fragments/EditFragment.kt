package com.soul.ocr.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.style.AlignmentSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.soul.ocr.ImageSizeType
import com.soul.ocr.R
import com.soul.ocr.databinding.FragmentEditBinding
import java.io.File
import java.io.FileOutputStream

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private var selectedTypeface: Typeface? = null
    private var selectedTextColor: Int = 0xFF000000.toInt()
    private var selectedBgColor: Int = 0xFFFFFFFF.toInt()
    private var selectedFontSize = 18f
    private var selectedSize: ImageSizeType = ImageSizeType.SIZE_1_1

    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrike = false
    private var selectedAlignment: View? = null
    private lateinit var selectedSizeButton: AppCompatButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
        setupFormattingButtons()
        setupAlignmentButtons()
        setupSizeButtons()


        binding.btnSize11.setOnClickListener {
            updateCardSize(ImageSizeType.SIZE_1_1)
            updateSelectedSizeButton(binding.btnSize11)
        }

        binding.btnSize23.setOnClickListener {
            updateCardSize(ImageSizeType.SIZE_2_3)
            updateSelectedSizeButton(binding.btnSize23)
        }

        binding.btnSize32.setOnClickListener {
            updateCardSize(ImageSizeType.SIZE_3_2)
            updateSelectedSizeButton(binding.btnSize32)
        }

        binding.btnSizeA4.setOnClickListener {
            updateCardSize(ImageSizeType.SIZE_A4)
            updateSelectedSizeButton(binding.btnSizeA4)
        }
        binding.btnGenerateFile.setOnClickListener {
            val text = binding.etEditText.text.toString()

            if (text.isNotEmpty()) {
                generateImageFromText(text, selectedSize)
            } else {
                Toast.makeText(requireContext(), "Text is empty!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }


//        binding.btnGenerateFile.setOnClickListener {
//            generateImageFromText()
//        }
//        binding.sizeGroup.setOnCheckedChangeListener { _, checkedId ->
//            val layoutParams = binding.editcard.layoutParams
//
//            layoutParams.height = when (checkedId) {
//                R.id.radioButton_1_1 -> 300.dpToPx(requireContext())  // 1:1 ratio
//                R.id.radioButton_3_2 -> 200.dpToPx(requireContext())  // 3:2 ratio
//                R.id.radioButton_2_3 -> 400.dpToPx(requireContext())  // 2:3 ratio
//                R.id.radioButton_A4 -> 500.dpToPx(requireContext())   // A4 size
//                else -> layoutParams.height
//            }
//
//            binding.editcard.layoutParams = layoutParams
//        }
//        binding.settingsIcon.setOnClickListener {
//            findNavController().navigate(R.id.action_editFragment_to_settingFragment)
//        }

    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun showFontMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.font_list_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            binding.fontSpinner.text = item.title
            // Perform action based on selected item
            when (item.itemId) {
                R.id.font_alvi -> {
                    binding.etEditText.typeface =
                        ResourcesCompat.getFont(requireContext(), R.font.alvi_nastaleeq_regular)
                    true
                }

                R.id.font_jamil -> {
                    binding.etEditText.typeface =
                        ResourcesCompat.getFont(requireContext(), R.font.jameel_noori_nastaleeq)
                    true
                }

                else -> {
                    binding.etEditText.typeface = Typeface.DEFAULT
                    true
                }
            }

        }
        popup.show()
    }

    private fun showFontColorMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.font_color_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.font_black -> {
                    binding.etEditText.setTextColor(0xFF000000.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFF000000.toInt())
                    true
                }

                R.id.font_red -> {
                    binding.etEditText.setTextColor(0xFFFF0000.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFFFF0000.toInt())
                    true
                }

                R.id.font_blue -> {
                    binding.etEditText.setTextColor(0xFF0000FF.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFF0000FF.toInt())
                    true
                }

                R.id.font_green -> {
                    binding.etEditText.setTextColor(0xFF00FF00.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFF00FF00.toInt())
                    true
                }

                else -> {
                    binding.etEditText.setTextColor(0xFF000000.toInt())
                    binding.colorLine.backgroundTintList =
                        ColorStateList.valueOf(0xFF000000.toInt())
                    true
                }
            }

        }
        popup.show()
    }

    private fun showBackgroundMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.font_color_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->

            when (item.itemId) {
                R.id.font_black -> {
                    binding.etEditText.setBackgroundColor(0xFF000000.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF000000.toInt())
                    true
                }

                R.id.font_red -> {
                    binding.etEditText.setBackgroundColor(0xFFFF0000.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFFFF0000.toInt())
                    true
                }

                R.id.font_blue -> {
                    binding.etEditText.setBackgroundColor(0xFF0000FF.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF0000FF.toInt())
                    true
                }

                R.id.font_green -> {
                    binding.etEditText.setBackgroundColor(0xFF00FF00.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF00FF00.toInt())
                    true
                }

                else -> {
                    binding.etEditText.setBackgroundColor(0xFF000000.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF000000.toInt())
                    true
                }
            }

        }
        popup.show()
    }

    private fun setEvents() {

        binding.fontSpinner.setOnClickListener {
            showFontMenu(it)
        }

        binding.textColor.setOnClickListener {
            showFontColorMenu(it)
        }

        binding.fontSizeSpinner.setOnClickListener {
            showFontSizeMenu(it)
        }

        binding.canvasColor.setOnClickListener {
            showBackgroundMenu(it)
        }
    }

    private fun showFontSizeMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.font_size_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            val selectedText = item.title.toString()
            val selectedFontSize = selectedText.toFloatOrNull() ?: 14f
            binding.etEditText.textSize = selectedFontSize
            binding.fontSizeSpinner.text = "$selectedFontSize px"
            true
        }

        popup.show()
    }

    private fun setupFormattingButtons() {
        binding.Blod.setOnClickListener {
            isBold = !isBold
            binding.Blod.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (isBold) R.color.green1 else R.color.lightgreen
                )
            )
            applySpan(StyleSpan(Typeface.BOLD))
        }

        binding.italic.setOnClickListener {
            isItalic = !isItalic
            binding.italic.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (isItalic) R.color.green1 else R.color.lightgreen
                )
            )
            applySpan(StyleSpan(Typeface.ITALIC))
        }

        binding.underlin.setOnClickListener {
            isUnderline = !isUnderline
            binding.underlin.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (isUnderline) R.color.green1 else R.color.lightgreen
                )
            )
            applySpan(UnderlineSpan())
        }

        binding.StrikrThrough.setOnClickListener {
            isStrike = !isStrike
            binding.StrikrThrough.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (isStrike) R.color.green1 else R.color.lightgreen
                )
            )
            applySpan(StrikethroughSpan())
        }
    }

    private fun applySpan(span: Any) {
        val start = binding.etEditText.selectionStart
        val end = binding.etEditText.selectionEnd
        if (start >= 0 && end > start) {
            val spannable = binding.etEditText.text as Spannable
            val spans = spannable.getSpans(start, end, span::class.java)

            var spanAlreadyExists = false
            for (existingSpan in spans) {
                // Remove if span already applied
                spannable.removeSpan(existingSpan)
                spanAlreadyExists = true
            }

            if (!spanAlreadyExists) {
                spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun applyAlignment(alignment: Layout.Alignment) {
        val start = binding.etEditText.selectionStart
        val end = binding.etEditText.selectionEnd
        if (start >= 0 && end > start) {
            val spannable = binding.etEditText.text as Spannable
            spannable.setSpan(
                AlignmentSpan.Standard(alignment),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun setupAlignmentButtons() {
        val green = ContextCompat.getColor(requireContext(), R.color.green1)
        val lightGreen = ContextCompat.getColor(requireContext(), R.color.lightgreen)

        val alignmentButtons = mapOf(
            binding.leftwriting to Layout.Alignment.ALIGN_NORMAL,
            binding.centerwriting to Layout.Alignment.ALIGN_CENTER,
            binding.rightwriting to Layout.Alignment.ALIGN_OPPOSITE,
            binding.equalwriting to Layout.Alignment.ALIGN_NORMAL
        )

        for ((button, alignment) in alignmentButtons) {
            button.setOnClickListener {
                selectedAlignment?.clearColorFilter()  // previous button unselected
                button.setColorFilter(green)           // current selected
                selectedAlignment = button
                applyAlignment(alignment)
            }
        }
    }


    private fun generateImageFromText(text: String, sizeType: ImageSizeType) {
        val width: Int
        val height: Int

        when (sizeType) {
            ImageSizeType.SIZE_1_1 -> {
                width = 800
                height = 800
            }

            ImageSizeType.SIZE_2_3 -> {
                width = 800
                height = 1200
            }

            ImageSizeType.SIZE_3_2 -> {
                width = 1200
                height = 800
            }

            ImageSizeType.SIZE_A4 -> {
                width = 1240
                height = 1754
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 40f
        paint.textAlign = Paint.Align.RIGHT
        val x = width - 40f
        var y = 100f

        val lines = text.split("\n")
        for (line in lines) {
            canvas.drawText(line, x, y, paint)
            y += paint.textSize + 20
        }

        // ✅ Save to internal storage
        val filename = "ExtractedUrdu_text.png"
        val file = File(requireContext().filesDir, filename)

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        Toast.makeText(
            requireContext(),
            "Saved to Internal: ${file.absolutePath}",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun updateCardSize(sizeType: ImageSizeType) {
        val layoutParams = binding.SizecardView.layoutParams

        when (sizeType) {
            ImageSizeType.SIZE_1_1 -> {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = 400  // square look
            }

            ImageSizeType.SIZE_2_3 -> {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = 600
            }

            ImageSizeType.SIZE_3_2 -> {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = 300
            }

            ImageSizeType.SIZE_A4 -> {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = 842  // A4 ratio approx in dp
            }
        }

        binding.SizecardView.layoutParams = layoutParams
    }

    private fun setupSizeButtons() {
        val sizeButtons = listOf(
            binding.btnSize11 to ImageSizeType.SIZE_1_1,
            binding.btnSize23 to ImageSizeType.SIZE_2_3,
            binding.btnSize32 to ImageSizeType.SIZE_3_2,
            binding.btnSizeA4 to ImageSizeType.SIZE_A4
        )

        // Default selection (optional)
        selectedSizeButton = binding.btnSize11
        selectedSizeButton.isSelected = true
        selectedSize = ImageSizeType.SIZE_1_1
        updateCardSize(selectedSize)

        sizeButtons.forEach { (button, sizeType) ->
            button.setOnClickListener {
                // Remove selection from previous button
                selectedSizeButton.isSelected = false

                // Highlight new one
                button.isSelected = true
                selectedSizeButton = button

                // Update size
                selectedSize = sizeType
                updateCardSize(sizeType)
            }
        }
    }
    private fun updateSelectedSizeButton(selectedButton: Button) {
        val allButtons = listOf(
            binding.btnSize11,
            binding.btnSize23,
            binding.btnSize32,
            binding.btnSizeA4
        )

        allButtons.forEach { it.isSelected = false }
        selectedButton.isSelected = true
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun View.clearColorFilter() {
        if (this is android.widget.ImageView) {
            this.colorFilter = null
        }
    }
}
