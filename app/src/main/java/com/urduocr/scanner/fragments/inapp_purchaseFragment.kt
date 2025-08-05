package com.urduocr.scanner.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.urduocr.scanner.R
import com.urduocr.scanner.databinding.FragmentInappPurchaseBinding


class inapp_purchaseFragment : Fragment() {
    private lateinit var binding: FragmentInappPurchaseBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentInappPurchaseBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.crossbtn.setOnClickListener {
            // This handles back navigation properly respecting the back stack
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

}