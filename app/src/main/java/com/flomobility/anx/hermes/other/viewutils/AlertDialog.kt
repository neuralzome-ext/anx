package com.flomobility.anx.hermes.other.viewutils

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.flomobility.anx.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlertDialog : DialogFragment() {

    private var yesListener: (() -> Unit)? = null

    private var noListener: (() -> Unit)? = null

    private var cancellable: Boolean = true

    fun setYesListener(listener: () -> Unit) {
        yesListener = listener
    }

    private var title: String = "Alert Dialog"

    private var message: String = "Alert Dialog "

    private var iconResId: Int = R.drawable.ic_warning

    var yesText: String = "OK"

    var noText: String = "No"

    var shouldSetNegativeButton = true

    companion object {

        const val TAG = "AlertDialog"

        fun getInstance(
            title: String,
            message: String,
            yesText: String,
            noText: String = "",
            iconResId: Int = R.drawable.ic_warning,
            cancellable: Boolean = false,
            noListener: (() -> Unit)? = null,
            yesListener: (() -> Unit)? = null,
        ) = AlertDialog().apply {
            this.title = title
            this.message = message
            this.iconResId = iconResId
            this.cancellable = cancellable
            this.yesText = yesText
            if(noText.isEmpty()) {
                shouldSetNegativeButton = false
            }
            this.noText = noText
            this.yesListener = yesListener
            this.noListener = noListener
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme).apply {
            setTitle(title)
            setMessage(message)
            setIcon(iconResId)
            isCancelable = cancellable
            setPositiveButton(yesText) { _, _ ->
                yesListener?.let { yes ->
                    yes()
                    return@setPositiveButton
                }
                dismiss()
            }
            if (shouldSetNegativeButton) {
                setNegativeButton(noText) { dialogInterface, _ ->
                    noListener?.let { no ->
                        no()
                        return@setNegativeButton
                    }
                    dialogInterface.cancel()
                }
            }
            setOnDismissListener {
                yesListener = null
                noListener = null
            }
        }.create()
    }
}
