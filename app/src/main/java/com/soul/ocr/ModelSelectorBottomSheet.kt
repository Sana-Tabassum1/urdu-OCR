package com.soul.ocr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ModelSelectorBottomSheet(
    private val onModelSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.model_selector_bottomsheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val models = listOf(
            Pair(R.id.basicocr, "OCR-Basic"),
            Pair(R.id.ocr_refine, "OCR-Refine"),
            Pair(R.id.ocr_context, "OCR-Context"),
            Pair(R.id.ocr_visual, "OCR-Visual"),
            Pair(R.id.ocr_semantic, "OCR-Semantic")
        )

        fun updateSelection(newSelectedId: Int) {
            models.forEach { (id, _) ->
                val layout = view.findViewById<LinearLayout>(id)
                val color = if (id == newSelectedId) R.color.clay else R.color.white
                layout.setBackgroundResource(R.drawable.bg_rounded)
                layout.backgroundTintList = ContextCompat.getColorStateList(requireContext(), color)
            }
        }

        models.forEachIndexed { index, (layoutId, modelName) ->
            val layout = view.findViewById<LinearLayout>(layoutId)
            layout.setOnClickListener {
                selectedId = layoutId
                updateSelection(selectedId)
                onModelSelected(modelName)
                view.postDelayed({ dismiss() }, 250)
            }
        }
    }
}
