package com.kleos.education.ui.partners

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kleos.education.databinding.FragmentPartnersBinding
import androidx.lifecycle.lifecycleScope
import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.PartnersApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PartnersFragment : Fragment() {

    private var _binding: FragmentPartnersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PartnerAdapter

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
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // Настройка RecyclerView
        adapter = PartnerAdapter(emptyList()) { partner ->
            val intent = Intent(requireContext(), PartnerDetailActivity::class.java)
            intent.putExtra("id", partner.id)
            intent.putExtra("name", partner.name)
            intent.putExtra("description", partner.description)
            intent.putExtra("logoUrl", partner.logoUrl)
            intent.putExtra("url", partner.url)
            startActivity(intent)
        }
        
        binding.partnersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.partnersRecyclerView.adapter = adapter
        
        loadPartners()
    }

    private fun loadPartners() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val api = ApiClient.retrofit.create(PartnersApi::class.java)
                api.list()
            }.onSuccess { list ->
                withContext(Dispatchers.Main) {
                    if (list.isEmpty()) {
                        adapter.submitList(emptyList())
                    } else {
                        adapter.submitList(list)
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    adapter.submitList(emptyList())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



