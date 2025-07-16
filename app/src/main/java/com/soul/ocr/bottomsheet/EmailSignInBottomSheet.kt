package com.soul.ocr.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.soul.ocr.databinding.DialogEnterEmailBinding
import com.soul.ocr.databinding.DialogSignInBinding

class EmailSignInBottomSheet: BottomSheetDialogFragment() {
    private lateinit var binding: DialogEnterEmailBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogEnterEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}