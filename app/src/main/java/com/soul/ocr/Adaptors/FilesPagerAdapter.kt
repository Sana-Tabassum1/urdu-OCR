package com.soul.ocr.Adaptors

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.soul.ocr.fragments.SavedFragment
import com.soul.ocr.fragments.SavedPageFragment

class FilesPagerAdapter(private val parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {

    private val fragmentList = mutableMapOf<Int, SavedPageFragment>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val type = when (position) {
            0 -> "ALL"
            1 -> "PDF"
            2 -> "TXT"
            else -> throw IllegalArgumentException("Invalid position")
        }
        val fragment = SavedPageFragment.newInstance(type)
        fragment.selectionListener = parentFragment as? SavedPageFragment.SelectionChangeListener
        fragmentList[position] = fragment
        return fragment
    }

    fun getFragment(position: Int): SavedPageFragment? {
        return fragmentList[position]
    }

}
