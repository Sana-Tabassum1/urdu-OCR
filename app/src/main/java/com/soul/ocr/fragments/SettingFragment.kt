package com.soul.ocr.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.soul.ocr.ModelSelectorBottomSheet
import com.soul.ocr.R
import com.soul.ocr.ViewModel.VoiceSettings
import com.soul.ocr.ViewModel.VoiceSettingsViewModel
import com.soul.ocr.databinding.FragmentSettingBinding
import com.soul.ocr.databinding.SelectModeDialogBinding
import com.soul.ocr.databinding.TextSizeDialogBinding


class SettingFragment : Fragment() {
    private lateinit var binding: FragmentSettingBinding
    private val voiceSettingsViewModel: VoiceSettingsViewModel by activityViewModels()

    private var isVoiceSettingVisible = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
      binding= FragmentSettingBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fun Float.round1() = String.format("%.1f", this).toFloat()

        binding.speechRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = (progress / 100f).coerceIn(0.5f, 2.0f).round1()
                binding.speechRateValue.setText(rate.toString())
                saveSettingsFromUI()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.voiceToneSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val tone = (progress / 100f).coerceIn(0.5f, 2.0f).round1()
                binding.voiceToneValue.setText(tone.toString())
                saveSettingsFromUI()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.audioClaritySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clarity = (progress / 100f).coerceIn(0.5f, 2.0f).round1()
                binding.audioClarityValue.setText(clarity.toString())
                saveSettingsFromUI()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.responseDelaySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.responseDelayValue.setText(progress.toString())
                saveSettingsFromUI()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })



        // Default: hide voice setting layout
        binding.voicesetting.visibility = View.GONE

        binding.voicsettinglayout.setOnClickListener {
            isVoiceSettingVisible = !isVoiceSettingVisible

            if (isVoiceSettingVisible) {
                binding.voicesetting.visibility = View.VISIBLE
                binding.voicebtn.setImageResource(R.drawable.toparrow) // 👈 your upward arrow
            } else {
                binding.voicesetting.visibility = View.GONE
                binding.voicebtn.setImageResource(R.drawable.bottomarrow) // 👈 your downward arrow
            }
        }
        binding.btncredits.setOnClickListener {
            findNavController().navigate(R.id.action_settingFragment_to_modelScreenFragment)
        }
       // setAppVersion()
//        binding.textsizecardview.setOnClickListener {
//            showTextSizeDialog()
//        }
//        binding.modeCardview.setOnClickListener {
//            showSelectModeDialog()
//        }
        binding.info.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        binding.privacy.setOnClickListener {
            val url = "https://yourdomain.com/privacy-policy"  // 🔁 Replace with your real URL
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        binding.sharelinear.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Check out this amazing OCR Urdu app: https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            )
            startActivity(Intent.createChooser(shareIntent, "Share App via"))
        }
        binding.langlinaer.setOnClickListener {
            val languages = arrayOf("English", "Urdu")
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Select Language")
            builder.setItems(languages) { dialog, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(requireContext(), "English Selected", Toast.LENGTH_SHORT).show()
                        // TODO: Language change logic here (optional)
                    }
                    1 -> {
                        Toast.makeText(requireContext(), "Urdu Selected", Toast.LENGTH_SHORT).show()
                        // TODO: Language change logic here (optional)
                    }
                }
            }
            builder.show()
        }

        binding.modelCardview.setOnClickListener {
            val bottomSheet = ModelSelectorBottomSheet { selectedModel ->
                // User ne jo select kia, wo yahan milta hai
                Toast.makeText(
                    requireContext(),
                    "Selected: $selectedModel",
                    Toast.LENGTH_SHORT
                ).show()
            }
            bottomSheet.show(parentFragmentManager, "ModelSheet")
        }
        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.action_settingFragment_to_modelScreenFragment)
        }
    }
    private fun showTextSizeDialog() {
        val dialog = Dialog(requireActivity())

        // Inflate your custom layout using ViewBinding
        val dialogBinding = TextSizeDialogBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        // Set default progress
        dialogBinding.seekBarTextSize.progress = 16


        // Listen to seekbar changes (optional)
        dialogBinding.seekBarTextSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                // Example: Update title text size dynamically
                dialogBinding.txtTitle.textSize = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //  seekbar drag
                val selectedSize = seekBar?.progress ?: 16
                Toast.makeText(
                    requireActivity(),
                    "Selected text size: $selectedSize",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
        })

        dialog.show()
    }

    private fun showSelectModeDialog() {
        val dialog = Dialog(requireActivity())
        val dialogBinding = SelectModeDialogBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        // Dialog window width , height set
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Default selection
        dialogBinding.radioLight.isChecked = true

        dialogBinding.radioLight.setOnClickListener {
            Toast.makeText(requireActivity(), "Light Mode Selected", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            //  logic
        }

        dialogBinding.radioDark.setOnClickListener {
            Toast.makeText(requireActivity(), "Dark Mode Selected", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

        }

        dialog.show()
    }
    private fun setAppVersion() {
        val packageManager = requireContext().packageManager
        val packageName = requireContext().packageName
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
       // binding.subtitle4.text = versionName
    }
    private fun openPlayStoreReview() {
        try {
            val uri = Uri.parse("market://details?id=${requireActivity().packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.android.vending")
            startActivity(intent)
        } catch (e: Exception) {
            // Play Store not available, open in browser
            val browserUri = Uri.parse("https://play.google.com/store/apps/details?id=${requireActivity().packageName}")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun saveSettingsFromUI() {
        val speechRate = binding.speechRateValue.text.toString().toFloatOrNull() ?: 1.0f
        val voiceTone = binding.voiceToneValue.text.toString().toFloatOrNull() ?: 1.0f
        val clarity = binding.audioClarityValue.text.toString().toFloatOrNull() ?: 1.0f
        val delay = binding.responseDelayValue.text.toString().toIntOrNull() ?: 0

        val updatedSettings = VoiceSettings(
            speechRate = speechRate,
            voiceTone = voiceTone,
            audioClarity = clarity,
            responseDelay = delay
        )
        voiceSettingsViewModel.updateSettings(updatedSettings)
    }

}