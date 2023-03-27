package com.example.interviewai

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.client.Chat

class ItemAdapter @OptIn(BetaOpenAI::class) constructor(private val context: Context, private val dataset: MutableList<ChatMessage>): RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val interviewerTextView: TextView = view.findViewById(R.id.interviewerBubbleItem)
        val interviewMessageSender: TextView = view.findViewById(R.id.messageSenderInterviewer)
        val userTextView: TextView = view.findViewById(R.id.userBubbleItem)
        val userMessageSender: TextView = view.findViewById(R.id.messageSenderUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        // create a new view
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ItemViewHolder(adapterLayout)
    }

    @OptIn(BetaOpenAI::class)
    fun addItem(item: ChatMessage, position: Int) {
        this.dataset.add(item)
        notifyItemInserted(position)
    }
    @OptIn(BetaOpenAI::class)
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]

        if(item.role == ChatRole.User){
            holder.interviewerTextView.visibility = View.GONE
            holder.userTextView.visibility = View.VISIBLE
            holder.userTextView.text =  item.content
            holder.userMessageSender.visibility= View.VISIBLE
            holder.interviewMessageSender.visibility= View.GONE
            holder.userMessageSender.text = "You"

        }

        if(item.role == ChatRole.System || item.role == ChatRole.Assistant){
            holder.userTextView.visibility = View.GONE
            holder.interviewerTextView.visibility = View.VISIBLE
            holder.interviewerTextView.text =  item.content
            holder.userMessageSender.visibility= View.GONE
            holder.interviewMessageSender.visibility= View.VISIBLE
            holder.interviewMessageSender.text = "Interviewer"
        }
    }

    @OptIn(BetaOpenAI::class)
    override fun getItemCount() = dataset.size
}