package com.soul.ocr.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.soul.ocr.R
import com.soul.ocr.databinding.FragmentModelScreenBinding


class ModelScreenFragment : Fragment() {
    private lateinit var binding: FragmentModelScreenBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentModelScreenBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.crossbtn.setOnClickListener {
            findNavController().navigate(R.id.action_modelScreenFragment_to_nav_settings)
        }
    }

}