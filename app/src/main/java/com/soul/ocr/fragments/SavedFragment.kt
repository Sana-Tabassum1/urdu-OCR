package com.soul.ocr.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.soul.ocr.Adaptors.FilesPagerAdapter
import com.soul.ocr.R
import com.soul.ocr.databinding.FragmentSavedBinding

class SavedFragment : Fragment(), SavedPageFragment.SelectionChangeListener {

    private lateinit var binding: FragmentSavedBinding
    private lateinit var pagerAdapter: FilesPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = FilesPagerAdapter(this)
        binding.viewPagerFiles.adapter = pagerAdapter

        // ---------- TAB SWITCH ----------
        binding.btnAll.setOnClickListener { switchTab(0) }
        binding.btnPdf.setOnClickListener { switchTab(1) }
        binding.btnTxt.setOnClickListener { switchTab(2) }

        // ---------- SELECTION ACTIONS ----------
        binding.ivDelete.setOnClickListener { currentPage()?.deleteSelectedFiles() }
        binding.ivShare.setOnClickListener { currentPage()?.shareSelectedFiles() }
        binding.ivBackSelection.setOnClickListener { currentPage()?.clearSelection() }
        binding.ivPin.setOnClickListener { currentPage()?.togglePinSelectedFiles() }

        // ---------- SEARCH ----------
        setupSearchUi()

        // ---------- PAGE‑CHANGE LISTENER ----------
        binding.viewPagerFiles.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                highlightTab(position)
                updateTabTextColor(position)
                // reset search when user changes tab
                clearSearch()
            }
        })

        binding.btndaimond.setOnClickListener {
            findNavController().navigate(R.id.action_savedFragment_to_modelScreenFragment)
        }
    }

    /* -------------------- SEARCH HANDLING -------------------- */

    private fun setupSearchUi() {
        // Open keyboard when user taps search icon
        binding.ivSearch.setOnClickListener {
            binding.etSearch.requestFocus()
            showKeyboard()
        }

        // Remove text + close keyboard when cross pressed
        binding.ivClear.setOnClickListener { clearSearch() }

        // Live filter while typing
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { /* no‑op */ }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no‑op */ }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                binding.ivClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                currentPage()?.filterFiles(query)
            }
        })
    }

    private fun clearSearch() {
        binding.etSearch.text?.clear()
        binding.ivClear.visibility = View.GONE
        binding.etSearch.clearFocus()
        hideKeyboard()
        currentPage()?.filterFiles("")       // show full list again
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    /* -------------------- UTILS -------------------- */

    private fun switchTab(index: Int) {
        binding.viewPagerFiles.currentItem = index
        highlightTab(index)
        updateTabTextColor(index)
    }

    private fun highlightTab(position: Int) {
        binding.btnAll.setBackgroundResource(if (position == 0) R.drawable.bg_selected_filter else R.drawable.bg_unselected_filter)
        binding.btnPdf.setBackgroundResource(if (position == 1) R.drawable.bg_selected_filter else R.drawable.bg_unselected_filter)
        binding.btnTxt.setBackgroundResource(if (position == 2) R.drawable.bg_selected_filter else R.drawable.bg_unselected_filter)
    }

    private fun updateTabTextColor(selectedIndex: Int) {
        val black = ContextCompat.getColor(requireContext(), R.color.black)
        val green = ContextCompat.getColor(requireContext(), R.color.green2)

        binding.btnAll.setTextColor(if (selectedIndex == 0) black else green)
        binding.btnPdf.setTextColor(if (selectedIndex == 1) black else green)
        binding.btnTxt.setTextColor(if (selectedIndex == 2) black else green)
    }

    private fun currentPage(): SavedPageFragment? =
        childFragmentManager.findFragmentByTag("f" + binding.viewPagerFiles.currentItem) as? SavedPageFragment

    /* -------------------- SELECTION CALLBACK -------------------- */

    override fun onSelectionChanged(isSelecting: Boolean, selectedCount: Int) {
        if (isSelecting) {
            binding.selectionLayout.visibility = View.VISIBLE
            binding.fileTypeFilter.visibility = View.GONE
            binding.tvSelectedCount.text = "$selectedCount× selected"
        } else {
            binding.selectionLayout.visibility = View.GONE
            binding.fileTypeFilter.visibility = View.VISIBLE
        }
    }
}
