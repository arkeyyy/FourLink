package com.example.fourlink

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import kotlin.math.min

abstract class GameDialogFragment(private val layout: Int) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = layoutInflater.inflate(layout, null)
        return AppCompatDialog(requireContext()).apply {
            setContentView(content)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setDimAmount(0.65f)
            bind(content)
        }
    }

    protected abstract fun bind(content: View)

    override fun onStart() {
        super.onStart()
        val density = resources.displayMetrics.density
        val decor = requireActivity().window.decorView
        val availableWidth = decor.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val availableHeight = decor.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val width = min(availableWidth - (32 * density).toInt(), (480 * density).toInt())
        val maxHeight = availableHeight - (32 * density).toInt()
        val content = requireDialog().findViewById<ViewGroup>(android.R.id.content)
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST))
        requireDialog().window?.setLayout(width, content.measuredHeight.coerceAtMost(maxHeight))
    }
}
