package com.example.fourlink

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf

class SurrenderDialogFragment : GameDialogFragment(R.layout.dialog_surrender) {
    override fun bind(content: View) {
        val player = arguments?.getString("arg_current_player")
        val yellow = getString(R.string.yellow)
        val red = getString(R.string.red)
        content.findViewById<TextView>(R.id.surrender_consequence).text =
            getString(R.string.surrender_body, if (player == "YELLOW") yellow else red,
                if (player == "YELLOW") red else yellow)
        content.findViewById<View>(R.id.btnYes).setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT, Bundle())
            dismiss()
        }
        content.findViewById<View>(R.id.btnNo).setOnClickListener { dismiss() }
    }

    companion object {
        const val RESULT = "surrender_confirmed"
        fun newInstance(currentPlayer: String) = SurrenderDialogFragment().apply {
            arguments = bundleOf("arg_current_player" to currentPlayer)
        }
    }
}
