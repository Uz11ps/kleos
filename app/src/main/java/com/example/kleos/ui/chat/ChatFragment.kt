package com.example.kleos.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.kleos.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var faqAdapter: FaqAdapter

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

        setupFaqRecycler()
        // Показать блок FAQ, скрыть чат до выбора
        binding.faqRecycler.visibility = View.VISIBLE
        binding.messagesList.visibility = View.GONE
        binding.messageInputBar.visibility = View.GONE

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

    private fun setupFaqRecycler() {
        val rv: RecyclerView = binding.faqRecycler
        val grid = GridLayoutManager(requireContext(), 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Первая карточка (CTA) занимает две колонки, остальные — одну
                return if (position == 0) 2 else 1
            }
        }
        rv.layoutManager = grid
        faqAdapter = FaqAdapter(loadFaqItems(), { item ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.question)
                .setMessage(item.answer)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }) {
            // CTA: скрыть FAQ и показать чат
            binding.faqRecycler.visibility = View.GONE
            binding.messagesList.visibility = View.VISIBLE
            binding.messageInputBar.visibility = View.VISIBLE
        }
        rv.adapter = faqAdapter
        rv.setHasFixedSize(true)
        // simple spacing by item margins already set in item layout
    }

    private fun loadFaqItems(): List<FaqItem> {
        val questions = resources.getStringArray(com.example.kleos.R.array.faq_questions)
        val answers = resources.getStringArray(com.example.kleos.R.array.faq_answers)
        val size = minOf(questions.size, answers.size)
        val list = ArrayList<FaqItem>(size)
        for (i in 0 until size) {
            val id = "faq_%03d".format(i + 1)
            list.add(FaqItem(id, "", questions[i], answers[i]))
        }
        return list
    }
}


