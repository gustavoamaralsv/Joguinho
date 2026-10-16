package com.example.joguinho.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.joguinho.R

/** Diálogo de fim de jogo (estilo Cap. 6), seguro com o ciclo de vida da Activity. */
class GameOverDialogFragment : DialogFragment() {

    fun interface Listener {
        fun onPlayAgain()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val newRecord = args.getBoolean(ARG_NEW_RECORD)
        val message = args.getString(ARG_MESSAGE).orEmpty()
        isCancelable = false
        return AlertDialog.Builder(requireContext())
            .setTitle(if (newRecord) R.string.new_record else R.string.game_over)
            .setMessage(message)
            .setPositiveButton(R.string.reset_game) { _, _ ->
                (activity as? Listener)?.onPlayAgain()
                    ?: (parentFragment as? Listener)?.onPlayAgain()
            }
            .create()
    }

    companion object {
        const val TAG = "GameOverDialog"
        private const val ARG_NEW_RECORD = "new_record"
        private const val ARG_MESSAGE = "message"

        fun newInstance(newRecord: Boolean, message: String): GameOverDialogFragment {
            return GameOverDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_NEW_RECORD, newRecord)
                    putString(ARG_MESSAGE, message)
                }
            }
        }
    }
}
