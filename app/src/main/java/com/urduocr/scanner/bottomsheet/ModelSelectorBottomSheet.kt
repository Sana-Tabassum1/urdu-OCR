package com.urduocr.scanner.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.urduocr.scanner.R
import com.urduocr.scanner.datastore.PreferenceDataStoreAPI
import com.urduocr.scanner.datastore.PreferenceDataStoreKeysConstants
import com.urduocr.scanner.datastore.PreferencesDataStoreHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ModelSelectorBottomSheet(
    private val onModelSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var dataStoreAPI: PreferenceDataStoreAPI
    private var selectedId: Int = -1
    private val models = listOf(
        Pair(R.id.basicocr, "OCR-Basic"),
        Pair(R.id.ocr_refine, "OCR-Refine"),
        Pair(R.id.ocr_context, "OCR-Context"),
        Pair(R.id.ocr_visual, "OCR-Visual"),
        Pair(R.id.ocr_semantic, "OCR-Semantic")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.model_selector_bottomsheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        dataStoreAPI = PreferencesDataStoreHelper(requireActivity())

        lifecycleScope.launch {
            val selectedModel =
                dataStoreAPI.getPreference(PreferenceDataStoreKeysConstants.OCR_MODEL, "OCR-Basic")
                    .first().takeIf { it.isNotEmpty() }
            selectedModel.takeIf { it!!.isNotEmpty() }?.let { modelTitle ->
                // Filter the models list based on the model title
                val selectedModelPair = models.firstOrNull { it.second == modelTitle }
                selectedModelPair?.let {
                    selectedId = it.first
                    updateSelection(selectedId)
                }
            }
        }

        models.forEachIndexed { _, (layoutId, modelName) ->
            val layout = view.findViewById<LinearLayout>(layoutId)
            layout.setOnClickListener {
                lifecycleScope.launch {
                    selectedId = layoutId
                    updateSelection(selectedId)
                    dataStoreAPI.putPreference(PreferenceDataStoreKeysConstants.OCR_MODEL, modelName)
                    onModelSelected(modelName)
                    view.postDelayed({ dismiss() }, 250)
                }
            }
        }
    }

    private fun updateSelection(newSelectedId: Int) {
        models.forEach { (id, _) ->
            val layout = view?.findViewById<LinearLayout>(id)
            val color = if (id == newSelectedId) R.color.clay else R.color.white
            layout?.setBackgroundResource(R.drawable.bg_rounded)
            layout?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), color)
        }
    }
}