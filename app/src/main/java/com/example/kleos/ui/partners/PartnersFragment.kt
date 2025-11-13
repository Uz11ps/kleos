package com.example.kleos.ui.partners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.kleos.databinding.FragmentPartnersBinding
import androidx.lifecycle.lifecycleScope
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.PartnersApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        loadPartners()
    }

    private fun addRow(text: String) {
        val item = com.google.android.material.textview.MaterialTextView(requireContext())
        item.text = "• $text"
        item.textSize = 16f
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = (8 * resources.displayMetrics.density).toInt()
        binding.partnersContainer.addView(item, lp)
    }

    private fun loadPartners() {
        binding.partnersContainer.removeAllViews()
        addRow("Loading…")
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val api = ApiClient.retrofit.create(PartnersApi::class.java)
                api.list()
            }.onSuccess { list ->
                withContext(Dispatchers.Main) {
                    binding.partnersContainer.removeAllViews()
                    if (list.isEmpty()) {
                        addRow("No partners yet")
                    } else {
                        list.forEach { p ->
                            addRow("${p.name}${if (!p.description.isNullOrBlank()) " — ${p.description}" else ""}")
                        }
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    binding.partnersContainer.removeAllViews()
                    addRow("Failed to load")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


