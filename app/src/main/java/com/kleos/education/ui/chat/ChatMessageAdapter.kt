package com.kleos.education.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.kleos.education.R
import com.kleos.education.data.model.Message

class ChatMessageAdapter(
    private var messages: List<Message> = emptyList()
) : BaseAdapter() {

    override fun getCount(): Int = messages.size

    override fun getItem(position: Int): Message = messages[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).sender == "user") 0 else 1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val message = getItem(position)
        val isUser = message.sender == "user"
        val viewType = getItemViewType(position)
        
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(
            if (isUser) R.layout.item_message_user else R.layout.item_message_support,
            parent,
            false
        )
        
        val messageText: TextView = view.findViewById(R.id.messageText)
        messageText.text = message.text
        
        return view
    }
    
    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}


