package com.example.fourlink

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView

class GameEndDialogFragment : DialogFragment() {
    private var resultMessage: String? = null
    private var onRestart: (() -> Unit)? = null
    private var onMainMenu: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultMessage = arguments?.getString(ARG_MESSAGE)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (300 * resources.displayMetrics.density).toInt(),
            (235 * resources.displayMetrics.density).toInt()
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity)
        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.dialog_game_end, null)

        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val messageText = view.findViewById<TextView>(R.id.tvMessage)
        val restartButton = view.findViewById<Button>(R.id.btnRestart)
        val mainMenuButton = view.findViewById<Button>(R.id.btnMainMenu)

        val styledText = when (resultMessage) {
            "PLAYER YELLOW WINS" -> "PLAYER <font color='#FFFF33'>YELLOW</font> WINS"
            "PLAYER RED WINS" -> "PLAYER <font color='#EC1C24'>RED</font> WINS"
            else -> resultMessage
        }

        messageText.text = Html.fromHtml(styledText, Html.FROM_HTML_MODE_LEGACY)

        restartButton.setOnClickListener {
            dialog.dismiss()
            onRestart?.invoke()
        }

        mainMenuButton.setOnClickListener {
            dialog.dismiss()
            onMainMenu?.invoke()
        }

        return dialog
    }



    fun setCallbacks(onRestart: () -> Unit, onMainMenu: () -> Unit) {
        this.onRestart = onRestart
        this.onMainMenu = onMainMenu
    }

    companion object {
        // Keep the argument key so restored dialogs can read an existing result message.
        private const val ARG_MESSAGE = "arg_winner"

        fun newInstance(message: String): GameEndDialogFragment {
            val fragment = GameEndDialogFragment()
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            fragment.arguments = args
            return fragment
        }
    }
}
