package com.example.kleos.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.kleos.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        binding.messagesList.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            adapter.clear()
            adapter.addAll(msgs.map { m ->
                val prefix = if (m.sender == "user") "Вы: " else "Поддержка: "
                "$prefix${m.text}"
            })
            adapter.notifyDataSetChanged()
            binding.messagesList.post { binding.messagesList.setSelection(adapter.count - 1) }
        }

        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text?.toString().orEmpty()
            viewModel.sendUserMessage(text)
            binding.messageEditText.setText("")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


