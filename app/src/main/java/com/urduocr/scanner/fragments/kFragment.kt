package com.urduocr.scanner.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import androidx.fragment.app.Fragment
import com.urduocr.scanner.databinding.FragmentKBinding

class kFragment : Fragment() {

    private var _binding: FragmentKBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editor.setEditorHeight(200)
        binding.editor.setEditorFontSize(16)
        binding.editor.setEditorFontColor(Color.BLACK)
        binding.editor.setPadding(10, 10, 10, 10)
        binding.editor.setPlaceholder("Start writing here...")
        binding.editor.setWebChromeClient(WebChromeClient())

        binding.actionBold.setOnClickListener { binding.editor.setBold() }
        binding.actionItalic.setOnClickListener { binding.editor.setItalic() }
        binding.actionUnderline.setOnClickListener { binding.editor.setUnderline() }
        binding.actionStrikethrough.setOnClickListener { binding.editor.setStrikeThrough() }
        binding.actionSubscript.setOnClickListener { binding.editor.setSubscript() }
        binding.actionSuperscript.setOnClickListener { binding.editor.setSuperscript() }

        binding.actionJustifyLeft.setOnClickListener { binding.editor.setAlignLeft() }
        binding.actionJustifyCenter.setOnClickListener { binding.editor.setAlignCenter() }
        binding.actionJustifyRight.setOnClickListener { binding.editor.setAlignRight() }

        binding.actionH1.setOnClickListener { binding.editor.setHeading(1) }
        binding.actionH2.setOnClickListener { binding.editor.setHeading(2) }
        binding.actionH3.setOnClickListener { binding.editor.setHeading(3) }
        binding.actionH4.setOnClickListener { binding.editor.setHeading(4) }
        binding.actionH5.setOnClickListener { binding.editor.setHeading(5) }
        binding.actionH6.setOnClickListener { binding.editor.setHeading(6) }

        binding.actionUndo.setOnClickListener { binding.editor.undo() }
        binding.actionRedo.setOnClickListener { binding.editor.redo() }

        binding.actionIndent.setOnClickListener { binding.editor.setIndent() }
        binding.actionOutdent.setOnClickListener { binding.editor.setOutdent() }

        binding.actionUl.setOnClickListener { binding.editor.setBullets() }
        binding.actionOl.setOnClickListener { binding.editor.setNumbers() }

        binding.actionInsertImage.setOnClickListener {
            binding.editor.insertImage("https://placekitten.com/200/300", "Kitten")
        }

        binding.actionInsertYoutube.setOnClickListener {
            binding.editor.insertYoutubeVideo("https://www.youtube.com/watch?v=QHH3iSeDBLo")
        }

        binding.actionInsertVideo.setOnClickListener {
            binding.editor.insertVideo("https://www.w3schools.com/html/mov_bbb.mp4")
        }

        binding.actionInsertAudio.setOnClickListener {
            binding.editor.insertAudio("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
        }

        binding.actionInsertLink.setOnClickListener {
            binding.editor.insertLink("https://openai.com", "OpenAI")
        }

        binding.actionCheckbox.setOnClickListener {
            binding.editor.insertTodo()
        }

        binding.actionTextColor.setOnClickListener {
            binding.editor.setTextColor(Color.RED)
        }

        binding.actionBgColor.setOnClickListener {
            binding.editor.setTextBackgroundColor(Color.YELLOW)
        }

        binding.actionFontSize.setOnClickListener {
            binding.editor.setFontSize(24)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
