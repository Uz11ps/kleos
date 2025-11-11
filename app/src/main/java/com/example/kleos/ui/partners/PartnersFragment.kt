package com.example.kleos.ui.partners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kleos.databinding.FragmentPartnersBinding

class PartnersFragment : Fragment() {

    private var _binding: FragmentPartnersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartnersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val partners = listOf(
            "Google — образовательные инициативы",
            "Microsoft — академические программы",
            "Coursera — онлайн-курсы",
            "Local IT Partners — стажировки"
        )
        binding.partnersContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        partners.forEach { name ->
            val item = com.google.android.material.textview.MaterialTextView(requireContext())
            item.text = "• $name"
            item.textSize = 16f
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (8 * resources.displayMetrics.density).toInt()
            binding.partnersContainer.addView(item, lp)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


