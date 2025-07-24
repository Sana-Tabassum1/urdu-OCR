package com.urduocr.scanner.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.urduocr.scanner.viewmodels.BatchScanningViewModel
import com.urduocr.scanner.databinding.FragmentFullScreenImageBinding


class FullScreenImageFragment : Fragment() {
    private lateinit var binding: FragmentFullScreenImageBinding
    private val viewModel: BatchScanningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentFullScreenImageBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = arguments?.getInt("position") ?: 0
        val bitmap = viewModel.bitmapImages.value?.getOrNull(position)

        bitmap?.let {
            binding.fullImageView.setImageBitmap(it)
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
            // requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

}

