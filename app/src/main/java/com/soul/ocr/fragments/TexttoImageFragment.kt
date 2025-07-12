package com.soul.ocr.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.soul.ocr.ModelClass.Content
import com.soul.ocr.ModelClass.GPTRequest
import com.soul.ocr.ModelClass.GPTResponse
import com.soul.ocr.ModelClass.ImageUrl
import com.soul.ocr.ModelClass.Message
import com.soul.ocr.databinding.FragmentPreviewBinding
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.soul.ocr.R
import com.soul.ocr.RetrofitHelper
import com.soul.ocr.interfce.GPTApi
import java.io.File
import java.io.FileOutputStream
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale


class TexttoImageFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var tts: TextToSpeech
    private var isSpeaking = false
    private var isTtsReady = false

    private val apiKey = "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"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etExtractedText.movementMethod = ScrollingMovementMethod()

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("ur", "PK"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(requireContext(), "Urdu TTS not supported", Toast.LENGTH_SHORT).show()
                } else {
                    tts.setSpeechRate(1.0f)
                    tts.setPitch(1.2f)

                    for (voice in tts.voices) {
                        if (
                            voice.locale == Locale("ur", "PK") &&
                            voice.name.contains("male", ignoreCase = true) &&
                            voice.quality == Voice.QUALITY_NORMAL &&
                            voice.latency <= Voice.LATENCY_NORMAL // Prefer fast voices
                        ) {
                            tts.voice = voice
                            break
                        }
                    }


                    // Warm-up TTS to reduce delay
                    tts.speak("", TextToSpeech.QUEUE_FLUSH, null, "warmup")
                    isTtsReady = true
                }
            } else {
                Toast.makeText(requireContext(), "TTS Initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                requireActivity().runOnUiThread {
                    binding.btnplay.setImageResource(R.drawable.playbutton)
                }
            }

            override fun onError(utteranceId: String?) {}
        })

        val croppedImageUriString = arguments?.getString("croppedImageUri")
        val uri = croppedImageUriString?.toUri()

        uri?.let {
            val bitmap = getBitmapFromUri(it)
            binding.finalImageView.setImageBitmap(bitmap)

            val base64Image = bitmapToBase64(bitmap)
            extractTextFromImage(base64Image)
        }

        binding.backArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRetake.setOnClickListener {
            findNavController().navigate(R.id.action_texttoImageFragment_to_cameraFragment)
        }

        binding.btnExpand.setOnClickListener {
            val drawable = binding.finalImageView.drawable ?: return@setOnClickListener
            val bitmap = (drawable as BitmapDrawable).bitmap

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val bundle = Bundle().apply {
                putByteArray("image_data", byteArray)
            }

            findNavController().navigate(
                R.id.action_texttoImageFragment_to_fullScreenImageFragment,
                bundle
            )
        }

        binding.btnCopy.setOnClickListener { copyTextTOClipboard() }
        binding.btnShare.setOnClickListener { shareText() }
        binding.btnSaveFile.setOnClickListener { saveTextAsFile() }
        binding.btnSavePdf.setOnClickListener { saveTextAsPdf() }

        binding.btnplay.setOnClickListener {
            val textToSpeak = binding.etExtractedText.text.toString()

            if (textToSpeak.isNotBlank() && isTtsReady) {
                if (!isSpeaking) {
                    tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
                    isSpeaking = true
                    binding.btnplay.setImageResource(R.drawable.pausebutton)
                } else {
                    tts.stop()
                    isSpeaking = false
                    binding.btnplay.setImageResource(R.drawable.playbutton)
                }
            } else {
                Toast.makeText(requireContext(), "No text to speak or TTS not ready", Toast.LENGTH_SHORT).show()
            }
        }

//        binding.settingsIcon.setOnClickListener {
//            findNavController().navigate(R.id.action_texttoImageFragment_to_settingFragment)
//        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun extractTextFromImage(base64Image: String) {
        val service = RetrofitHelper.instance

        val imageContent = Content(
            type = "image_url",
            image_url = ImageUrl("data:image/png;base64,$base64Image")
        )

        val prompt = Content(
            type = "text",
            text = "Extract all Urdu text from this image and return plain text only."
        )

        val message = Message(
            role = "user",
            content = listOf(prompt, imageContent)
        )

        val request = GPTRequest(
            model = "gpt-4o",
            messages = listOf(message),
            max_tokens = 1000
        )

        val bearerKey = "Bearer $apiKey"
        binding.progressBar.visibility = View.VISIBLE

        service.extractTextFromImage(bearerKey, request).enqueue(object : Callback<GPTResponse> {
            override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
                if (!isAdded || _binding == null) return
                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val extractedText = response.body()?.choices?.firstOrNull()?.message?.content
                    binding.etExtractedText.setText(extractedText)
                } else {
                    Toast.makeText(requireContext(), "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun copyTextTOClipboard() {
        val text = binding.etExtractedText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Extracted Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
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

    private fun saveTextAsFile() {
        val text = binding.etExtractedText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "No text to save", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val fileName = "urdu_text_${System.currentTimeMillis()}.txt"
            val file = File(requireContext().filesDir, fileName)
            file.writeText(text)
            Toast.makeText(requireContext(), "Saved as file: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTextAsPdf() {
        val context = requireContext()
        val text = binding.etExtractedText.text.toString()

        if (text.isEmpty()) {
            Toast.makeText(context, "No text to save", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()
        paint.textSize = 14f
        paint.isAntiAlias = true

        val lines = text.split("\n")
        var y = 50f

        for (line in lines) {
            canvas.drawText(line, 40f, y, paint)
            y += 25f
        }

        pdfDocument.finishPage(page)

        val fileName = "ExtractedUrdu_text_${System.currentTimeMillis()}.pdf"
        val file = File(context.filesDir, fileName)

        try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            Toast.makeText(context, "PDF saved to internal storage:\n$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.stop()
        tts.shutdown()
        _binding = null
    }
}
