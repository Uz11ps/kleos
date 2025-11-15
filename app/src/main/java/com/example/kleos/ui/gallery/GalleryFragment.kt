package com.example.kleos.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleos.databinding.FragmentGalleryBinding
import androidx.lifecycle.lifecycleScope
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.PartnersApi
import com.example.kleos.data.network.ProgramsApi
import com.example.kleos.ui.programs.ProgramDetailActivity
import com.example.kleos.ui.partners.PartnersSimpleAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PartnersSimpleAdapter(emptyList()) { p ->
            val intent = android.content.Intent(requireContext(), ProgramDetailActivity::class.java)
            intent.putExtra("title", p.name)
            intent.putExtra("description", p.description)
            intent.putExtra("university", "")
            intent.putExtra("tuition", "")
            intent.putExtra("duration", "")
            startActivity(intent)
        }
        binding.universitiesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.universitiesRecycler.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching {
                    val programs = ApiClient.retrofit.create(ProgramsApi::class.java).list()
                    programs.map { pr ->
                        com.example.kleos.data.network.PartnerDto(
                            id = pr.id,
                            name = pr.title,
                            description = pr.university ?: pr.level ?: "",
                            logoUrl = pr.imageUrl,
                            url = null
                        )
                    }
                }.getOrElse { emptyList() }
            }
            adapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}