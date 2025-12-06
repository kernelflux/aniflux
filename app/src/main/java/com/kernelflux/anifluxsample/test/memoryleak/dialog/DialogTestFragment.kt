package com.kernelflux.anifluxsample.test.memoryleak.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.into
import com.kernelflux.anifluxsample.R
import com.kernelflux.pag.PAGImageView
import com.kernelflux.anifluxsample.util.AniFluxLogger

/**
 * Dialog/PopupWindow test scenario
 * Tests memory leak prevention when Dialog/PopupWindow is dismissed
 */
class DialogTestFragment : Fragment() {

    private lateinit var tvInfo: TextView
    private lateinit var btnShowDialog: Button
    private lateinit var btnShowPopup: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dialog_test, container, false)

        tvInfo = view.findViewById(R.id.tv_info)
        btnShowDialog = view.findViewById(R.id.btn_show_dialog)
        btnShowPopup = view.findViewById(R.id.btn_show_popup)

        setupViews()

        return view
    }

    private fun setupViews() {
        tvInfo.text = """
            Dialog/PopupWindow Test
            =======================
            • Click buttons to show Dialog/PopupWindow with animations
            • Dismiss them and check resource cleanup
            • Verify animations are properly released
            • Monitor memory usage before and after dismiss
        """.trimIndent()

        btnShowDialog.setOnClickListener {
            showTestDialog()
        }

        btnShowPopup.setOnClickListener {
            showTestPopup()
        }
    }

    private fun showTestDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_test_animation)

        val pagImageView: PAGImageView = dialog.findViewById(R.id.pag_image_view)
        val btnDismiss: Button = dialog.findViewById(R.id.btn_dismiss)

        // Load animation
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AniFlux.with(requireContext())
            .asPAG()
            .load(pagUrl)
            .into(pagImageView)

        btnDismiss.setOnClickListener {
            dialog.dismiss()
            AniFluxLogger.i("Dialog dismissed - resources should be released")
        }

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.setOnDismissListener {
            AniFluxLogger.i("Dialog onDismiss - resources should be released")
        }

        dialog.show()
    }

    private fun showTestPopup() {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_test_animation, null)
        val pagImageView: PAGImageView = popupView.findViewById(R.id.pag_image_view)
        val btnDismiss: Button = popupView.findViewById(R.id.btn_dismiss)

        // Load animation
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AniFlux.with(requireContext())
            .asPAG()
            .load(pagUrl)
            .into(pagImageView)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        btnDismiss.setOnClickListener {
            popupWindow.dismiss()
            AniFluxLogger.i("PopupWindow dismissed - resources should be released")
        }

        popupWindow.setOnDismissListener {
            AniFluxLogger.i("PopupWindow onDismiss - resources should be released")
        }

        // Show popup at center
        popupWindow.showAsDropDown(btnShowPopup, 0, -200)
    }
}

