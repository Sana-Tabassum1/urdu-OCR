package com.urduocr.scanner.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Layout
import android.text.Spannable
import android.text.style.AlignmentSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.urduocr.scanner.R
import com.urduocr.scanner.RetrofitHelper
import com.urduocr.scanner.adapters.ImagePagerAdapter
import com.urduocr.scanner.bottomsheet.ModelSelectorBottomSheet
import com.urduocr.scanner.databinding.FragmentBatchExtractBinding
import com.urduocr.scanner.datastore.PreferenceDataStoreAPI
import com.urduocr.scanner.datastore.PreferenceDataStoreKeysConstants
import com.urduocr.scanner.datastore.PreferencesDataStoreHelper
import com.urduocr.scanner.models.Content
import com.urduocr.scanner.models.GPTRequest
import com.urduocr.scanner.models.ImageUrl
import com.urduocr.scanner.models.Message
import com.urduocr.scanner.viewmodels.BatchScanningViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class BatchExtractFragment : Fragment() {
    private lateinit var binding: FragmentBatchExtractBinding
    private val viewModel: BatchScanningViewModel by activityViewModels()

    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private var currentPage = 0

    private lateinit var dataStoreAPI: PreferenceDataStoreAPI

    private lateinit var tts: TextToSpeech
    private var isSpeaking = false
    private var isTtsReady = false
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrike = false
    private var selectedAlignment: View? = null
    private var alreadyProcessed = false

    private val apiKey = "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"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBatchExtractBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataStoreAPI = PreferencesDataStoreHelper(requireActivity())

        viewModel.extractedText.value?.let { saved ->
            if (saved.isNotEmpty()) {
                binding.etExtractedText.text = saved
                binding.progressStepLayout.progressBar.visibility = View.VISIBLE
                alreadyProcessed = true
            }
        }

        // Initialize TTS
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("ur", "PK")) // For Urdu
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(requireContext(), "Urdu TTS not supported", Toast.LENGTH_SHORT).show()
                } else {
                    // Set default speech rate and pitch
                    tts.setSpeechRate(1.0f) // Normal speed
                    tts.setPitch(1.0f) // Normal pitch
                    isTtsReady = true

                    // Set utterance listener for auto-stop
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            requireActivity().runOnUiThread {
                                isSpeaking = true
                                updatePlayButtonState()
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            requireActivity().runOnUiThread {
                                isSpeaking = false
                                updatePlayButtonState()
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            requireActivity().runOnUiThread {
                                isSpeaking = false
                                updatePlayButtonState()
                            }
                        }
                    })
                }
            } else {
                Toast.makeText(requireContext(), "TTS Initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up play/stop button
        binding.btnPlay.setOnClickListener {
            if (!isTtsReady) {
                Toast.makeText(requireContext(), "TTS not ready yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val text = binding.etExtractedText.text.toString()
            if (text.isBlank()) {
                Toast.makeText(requireContext(), "No text to read", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSpeaking) {
                // Manual stop
                tts.stop()
                isSpeaking = false
                updatePlayButtonState()
            } else {
                // Start speaking
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "unique_id")
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "unique_id")
            }
        }


        // Set utterance progress listener
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                requireActivity().runOnUiThread {
                    isSpeaking = false
                    binding.btnPlay.text = "Listen"
                    binding.btnPlay.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(requireContext(), R.drawable.volumeicon),
                        null, null, null
                    )
                }
            }

            override fun onError(utteranceId: String?) {
                requireActivity().runOnUiThread {
                    isSpeaking = false
                    binding.btnPlay.text = "Listen"
                    binding.btnPlay.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(requireContext(), R.drawable.volumeicon),
                        null, null, null
                    )
                }
            }
        })

        viewModel.bitmapImages.observe(viewLifecycleOwner) { bitmapList ->
            imagePagerAdapter = ImagePagerAdapter(bitmapList)
            binding.viewPager.adapter = imagePagerAdapter
            binding.viewPager.setCurrentItem(0, false)
            currentPage = 0

            if (bitmapList.size <= 1) {
                binding.leftarrow.visibility = View.GONE
                binding.rightarrow.visibility = View.GONE
            } else {
                binding.leftarrow.visibility = View.VISIBLE
                binding.rightarrow.visibility = View.VISIBLE
            }

            binding.leftarrow.setOnClickListener {
                if (currentPage > 0) {
                    currentPage--
                    binding.viewPager.setCurrentItem(currentPage, true)
                }
            }

            binding.rightarrow.setOnClickListener {
                if (currentPage < bitmapList.size - 1) {
                    currentPage++
                    binding.viewPager.setCurrentItem(currentPage, true)
                }
            }

            binding.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPage = position
                }
            })
        }

        binding.retakeSpinner.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val bottomSheet = ModelSelectorBottomSheet { selectedModel ->
                    Toast.makeText(
                        requireContext(),
                        "Selected: $selectedModel",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                bottomSheet.show(parentFragmentManager, "ModelSheet")
            }
            true
        }

        binding.ivBack.setOnClickListener {
            viewModel.clearAll()
            viewModel.extractedText.value = null
            findNavController().navigateUp()
        }

        binding.btnExpand.setOnClickListener {
            val pos = binding.viewPager.currentItem
            findNavController().navigate(
                R.id.action_batchExtractFragment_to_fullScreenImageFragment,
                Bundle().apply { putInt("position", pos) }
            )
        }

        if (!alreadyProcessed) {
            alreadyProcessed = true

            val launchExtraction: (String) -> Unit = { prompt ->
                Log.d(TAG, "onViewCreated123: $prompt")
                val progressDialog = showProcessingDialog(requireContext())
                updateProgressInDialog(progressDialog, 1)

                lifecycleScope.launch {
                    val extractedTexts = mutableListOf<String>()
                    viewModel.bitmapImages.value?.forEachIndexed { index, bitmap ->
                        val txt = callImagesApi(bitmap.toBase64Uri(), prompt)
                        extractedTexts.add(txt)
                        if (index == 0) updateProgressInDialog(progressDialog, 2)
                    }
                    updateProgressInDialog(progressDialog, 3)
                    val finalText = extractedTexts.joinToString("\n\n")
                    binding.etExtractedText.setText(finalText)
                    binding.progressStepLayout.progressBar.visibility = View.VISIBLE

                    viewModel.extractedText.value = finalText
                }
            }

            lifecycleScope.launch {
                val selectedModel =
                    dataStoreAPI.getPreference(
                        PreferenceDataStoreKeysConstants.OCR_MODEL,
                        "OCR-Basic"
                    )
                        .first().takeIf { it.isNotEmpty() }
                launchExtraction.invoke(getPromptText(selectedModel!!))
            }
        }

        binding.btnEdit.setOnClickListener {
            binding.editlayout.visibility = View.VISIBLE
            val text = binding.etExtractedText.text.toString()
            binding.editableExtractedText.setText(text)
            binding.etExtractedText.visibility = View.GONE
            binding.editableExtractedText.visibility = View.VISIBLE
        }

        setEvents()
        setupFormattingButtons()
        setupAlignmentButtons()

        binding.btnCopy.setOnClickListener {
            copyTextTOClipboard()
        }
        binding.btnShare.setOnClickListener {
            shareText()
        }
        binding.gerenrate.setOnClickListener {
            val suggested = "urdu_text_${System.currentTimeMillis()}"
            showSaveDialogWithFileName(suggested)
        }
    }

    private fun Bitmap.toBase64Uri(): String {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes = stream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private suspend fun callImagesApi(imageData: String, prompt: String): String =
        withContext(Dispatchers.IO) {
            try {
                Log.d("TAG", "callImagesApiPrompt: $prompt")
                val request = GPTRequest(
                    model = "gpt-4o",
                    max_tokens = 1000,
                    messages = listOf(
                        Message(
                            role = "user",
                            content = listOf(
                                Content(type = "text", text = prompt),
                                Content(type = "image_url", image_url = ImageUrl(imageData))
                            )
                        )
                    )
                )

                val call = RetrofitHelper.instance.extractTextFromImage("Bearer $apiKey", request)
                val response = call.execute()

                if (response.isSuccessful && response.body() != null) {
                    return@withContext response.body()!!.choices.firstOrNull()?.message?.content
                        ?: ""
                } else {
                    return@withContext "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                return@withContext "Exception: ${e.localizedMessage}"
            }
        }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun copyTextTOClipboard() {
        val text = binding.etExtractedText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Extracted Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Text copied to clipboad", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No text to copy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareText() {
        val text = binding.etExtractedText.text.toString()
        if (text.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(intent, "Share text via"))
        } else {
            Toast.makeText(requireContext(), "No text to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTextAsFile(fileName: String) {
        val text = binding.etExtractedText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "No text to save", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val file = File(requireContext().filesDir, "$fileName.txt")
            file.writeText(text)
            viewModel.clearAll()
            viewModel.extractedText.value = null
            Toast.makeText(requireContext(), "Saved as: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving file: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveTextAsPdf(fileName: String) {
        val context = requireContext()
        val text = binding.etExtractedText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(context, "No text to save", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val paint = Paint().apply { textSize = 14f; isAntiAlias = true }

        var y = 50f
        text.split("\n").forEach { line ->
            page.canvas.drawText(line, 40f, y, paint)
            y += 25f
        }
        pdfDoc.finishPage(page)

        val file = File(context.filesDir, "$fileName.pdf")
        try {
            FileOutputStream(file).use { pdfDoc.writeTo(it) }
            pdfDoc.close()

            viewModel.clearAll()
            viewModel.extractedText.value = null

            Toast.makeText(context, "PDF saved as: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFontMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.font_list_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            binding.fontSpinner.text = item.title
            when (item.itemId) {
                R.id.font_alvi -> {
                    binding.editableExtractedText.typeface =
                        ResourcesCompat.getFont(requireContext(), R.font.alvi_nastaleeq_regular)
                    true
                }

                R.id.font_jamil -> {
                    binding.editableExtractedText.typeface =
                        ResourcesCompat.getFont(requireContext(), R.font.jameel_noori_nastaleeq)
                    true
                }

                else -> {
                    binding.editableExtractedText.typeface = Typeface.DEFAULT
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
                    binding.editableExtractedText.setTextColor(0xFF000000.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFF000000.toInt())
                    true
                }

                R.id.font_red -> {
                    binding.editableExtractedText.setTextColor(0xFFFF0000.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFFFF0000.toInt())
                    true
                }

                R.id.font_blue -> {
                    binding.editableExtractedText.setTextColor(0xFF0000FF.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFF0000FF.toInt())
                    true
                }

                R.id.font_green -> {
                    binding.editableExtractedText.setTextColor(0xFF00FF00.toInt())
                    binding.colorLine.imageTintList =
                        ColorStateList.valueOf(0xFF00FF00.toInt())
                    true
                }

                else -> {
                    binding.editableExtractedText.setTextColor(0xFF000000.toInt())
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
                    binding.editableExtractedText.setBackgroundColor(0xFF000000.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF000000.toInt())
                    true
                }

                R.id.font_red -> {
                    binding.editableExtractedText.setBackgroundColor(0xFFFF0000.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFFFF0000.toInt())
                    true
                }

                R.id.font_blue -> {
                    binding.editableExtractedText.setBackgroundColor(0xFF0000FF.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF0000FF.toInt())
                    true
                }

                R.id.font_green -> {
                    binding.editableExtractedText.setBackgroundColor(0xFF00FF00.toInt())
                    binding.canvasColorLine.imageTintList =
                        ColorStateList.valueOf(0xFF00FF00.toInt())
                    true
                }

                else -> {
                    binding.editableExtractedText.setBackgroundColor(0xFF000000.toInt())
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
            binding.editableExtractedText.textSize = selectedFontSize
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
        val start = binding.editableExtractedText.selectionStart
        val end = binding.editableExtractedText.selectionEnd
        if (start >= 0 && end > start) {
            val spannable = binding.editableExtractedText.text as Spannable
            val spans = spannable.getSpans(start, end, span::class.java)

            var spanAlreadyExists = false
            for (existingSpan in spans) {
                spannable.removeSpan(existingSpan)
                spanAlreadyExists = true
            }

            if (!spanAlreadyExists) {
                spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
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
                selectedAlignment?.clearColorFilter()
                button.setColorFilter(green)
                selectedAlignment = button
                applyAlignment(alignment)
            }
        }
    }

    private fun applyAlignment(alignment: Layout.Alignment) {
        val start = binding.editableExtractedText.selectionStart
        val end = binding.editableExtractedText.selectionEnd
        if (start in 0..<end) {
            val spannable = binding.editableExtractedText.text as Spannable
            spannable.setSpan(
                AlignmentSpan.Standard(alignment),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun getPromptText(model: String): String {
        return when (model) {
            "OCR-Basic" -> "You are an OCR engine for Urdu images. Quickly scan the image and locate any Urdu text. Transcribe exactly what you see, preserving spacing and line breaks. Output only the raw Urdu text."
            "OCR-Refine" -> "You are an OCR engine for Urdu. First pass: identify all text regions in the image. Second pass: transcribe each region, preserving diacritics. Third pass: re-read your transcription against the image and fix any obvious mistakes. Output only the cleaned Urdu text."
            "OCR-Context" -> "You are an OCR engine specialized in Urdu. Visually inspect the image to find text areas. Transcribe each block, preserving formatting. For each block, note anywhere the image quality or handwriting might introduce uncertainty. In your final output, include the transcription but flag uncertain words in brackets (e.g. [ش‌بہ؟]). Output a plain-text Urdu transcript with bracketed notes."
            "OCR-Visual" -> "You are an advanced OCR engine for Urdu images. Analyze the image's lighting, contrast, and background to understand text clarity. Locate and transcribe every Urdu text region, preserving punctuation. Adjust your transcription to account for any skewed or stylized text. At the end, provide the final Urdu text and a one-sentence note on any challenging areas (e.g. 'bottom-right corner was low contrast'). Output only the transcript and that single note."
            "OCR-Semantic" -> "You are a semantic Urdu OCR expert. Examine the entire image, noting text layout, styles, and visual context. Transcribe all Urdu text, automatically correcting any OCR errors. Reflect on the content's meaning and, in a brief parenthesis after each paragraph, summarize its gist in Urdu. For any named entities (people, places), highlight them in bold in your transcription. Output a clean, human-readable Urdu transcript with these inline annotations."
            else -> "Extract all Urdu text from this image and return plain text only."
        }
    }

    private fun showSaveDialogWithFileName(suggestedName: String) {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_save_options, null)

        val etName = view.findViewById<EditText>(R.id.tvDescription)
        val btnFile = view.findViewById<TextView>(R.id.btnSaveAsFile)
        val btnPdf = view.findViewById<TextView>(R.id.btnSaveAsPdf)

        etName.setText(suggestedName)
        etName.setSelection(etName.text.length)

        fun paint(btn: TextView, selected: Boolean) {
            btn.setBackgroundResource(
                if (selected) R.drawable.button_selector
                else R.drawable.unselected_button
            )
            btn.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) android.R.color.white else R.color.green1
                )
            )
        }

        var saveAsPdf = false
        paint(btnFile, true)
        paint(btnPdf, false)

        val dlg = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(true)
            .create()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun currentName(): String {
            val raw = etName.text.toString().trim()
            return if (raw.isEmpty()) "urdu_text_${System.currentTimeMillis()}" else raw
        }

        fun finishDialog() {
            Handler(Looper.getMainLooper()).postDelayed({
                dlg.dismiss()
                findNavController().navigate(R.id.action_batchExtractFragment_to_nav_home)
            }, 120)
        }

        btnFile.setOnClickListener {
            if (saveAsPdf) {
                saveAsPdf = false
                paint(btnFile, true); paint(btnPdf, false)
            }
            saveTextAsFile(currentName())
            finishDialog()
        }

        btnPdf.setOnClickListener {
            if (!saveAsPdf) {
                saveAsPdf = true
                paint(btnPdf, true); paint(btnFile, false)
            }
            saveTextAsPdf(currentName())
            finishDialog()
        }

        dlg.show()
    }

    private fun updateProgressInDialog(dialog: Dialog, stage: Int) {
        val title = dialog.findViewById<TextView>(R.id.tvProgressTitle)
        val subtitle = dialog.findViewById<TextView>(R.id.tvProgressSubtitle)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)

        subtitle.startBlinkingAlpha(500L)
        when (stage) {
            1 -> {
                title?.text = "Processing Image!"
                subtitle?.text = "Detecting text..."
                animateProgressBarInDialog(progressBar, 20)
            }

            2 -> {
                subtitle?.text = "Parsing text..."
                animateProgressBarInDialog(progressBar, 60)
            }

            3 -> {
                subtitle?.text = "Finishing up..."
                animateProgressBarInDialog(progressBar, 100)

                Handler(Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                }, 800)
            }
        }
    }

    private fun TextView.startBlinkingAlpha(duration: Long = 800L) {
        ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun animateProgressBarInDialog(progressBar: ProgressBar?, target: Int) {
        progressBar?.let {
            ObjectAnimator.ofInt(it, "progress", target).apply {
                duration = 600
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    fun showProcessingDialog(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.progress_layout)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = dialog.window?.attributes
        layoutParams?.dimAmount = 0.5f
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        return dialog
    }
    private fun updatePlayButtonState() {
        if (isSpeaking) {
            binding.btnPlay.text = "Stop"
            binding.btnPlay.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.volumeicon), // Use a stop icon
                null, null, null
            )
        } else {
            binding.btnPlay.text = "Listen"
            binding.btnPlay.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.volumeicon), // Normal volume icon
                null, null, null
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (isSpeaking) {
            tts.stop()
            isSpeaking = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }

    fun View.clearColorFilter() {
        if (this is ImageView) {
            this.colorFilter = null
        }
    }
}