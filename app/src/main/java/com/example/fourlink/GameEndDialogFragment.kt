package com.example.fourlink

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf

class GameEndDialogFragment : GameDialogFragment(R.layout.dialog_game_end) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun bind(content: View) {
        content.findViewById<TextView>(R.id.tvMessage).text = requireArguments().getString("arg_winner")
        content.findViewById<View>(R.id.btnRestart).setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT, bundleOf("restart" to true))
            dismiss()
        }
        content.findViewById<View>(R.id.btnMainMenu).setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT, bundleOf("restart" to false))
            dismiss()
        }
    }

    companion object {
        const val RESULT = "game_end_action"
        fun newInstance(message: String) = GameEndDialogFragment().apply {
            arguments = bundleOf("arg_winner" to message)
        }
    }
}
