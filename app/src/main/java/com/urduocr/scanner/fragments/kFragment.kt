package com.urduocr.scanner.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.urduocr.scanner.R
import com.urduocr.scanner.adapters.HomeSliderAdapter
import com.urduocr.scanner.databinding.FragmentKBinding
import com.urduocr.scanner.models.SliderItem

class kFragment : Fragment() {

    private lateinit var binding: FragmentKBinding
    private lateinit var sliderAdapter: HomeSliderAdapter
    private lateinit var sliderHandler: Handler
    private lateinit var sliderRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentKBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sliderItems = listOf(
            SliderItem(R.drawable.urduu, "Most accurate Urdu OCR","Whether its handwriting or a book,\n" +
                    "Urdu OCR recogize text with 90% accuracy"),
            SliderItem(R.drawable.file, "Image to Urdu image","Type Urdu and generate image of Urdu text.\n" +
                    "Choose from five different Urdu fonts."),
            SliderItem(R.drawable.photo, "Organize your files","Type Urdu and generate image of Urdu text.\n" +
                    "Choose from five different Urdu fonts.")
        )


    }

    override fun onDestroyView() {
        super.onDestroyView()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}
