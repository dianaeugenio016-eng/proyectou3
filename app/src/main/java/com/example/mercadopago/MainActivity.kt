package com.example.mercadopago

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mercadopago.databinding.ActivityMainBinding
import java.util.concurrent.Executor

/**
 * Pantalla inicial de autenticación biométrica.
 * Impide el acceso a las demás pantallas hasta validar la huella del usuario.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var canceladoPorUsuario = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        executor = ContextCompat.getMainExecutor(this)
        configurarBiometria()

        binding.btnIrPanel.setOnClickListener {
            canceladoPorUsuario = false
            iniciarAutenticacionBiometrica()
        }

        binding.btnIrConfig.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    override fun onStart() {
        super.onStart()
        if (!canceladoPorUsuario) {
            iniciarAutenticacionBiometrica()
        }
    }

    private fun configurarBiometria() {
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (isFinishing || isDestroyed) return

                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        canceladoPorUsuario = true
                    }

                    val mensaje = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                            getString(R.string.biometric_error_canceled)

                        BiometricPrompt.ERROR_LOCKOUT ->
                            getString(R.string.biometric_error_lockout)

                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                            getString(R.string.biometric_error_lockout_permanent)

                        BiometricPrompt.ERROR_NO_BIOMETRICS ->
                            getString(R.string.biometric_error_no_biometrics)

                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                            getString(R.string.biometric_error_hw_unavailable)

                        else -> getString(R.string.biometric_error_generic, errString)
                    }
                    Toast.makeText(applicationContext, mensaje, Toast.LENGTH_LONG).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (isFinishing || isDestroyed) return

                    canceladoPorUsuario = false
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.biometric_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@MainActivity, PanelActivity::class.java))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (isFinishing || isDestroyed) return

                    Toast.makeText(
                        applicationContext,
                        getString(R.string.biometric_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_negative_button))
            .build()
    }

    private fun iniciarAutenticacionBiometrica() {
        if (puedeUsarBiometria()) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun puedeUsarBiometria(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, getString(R.string.biometric_no_hardware), Toast.LENGTH_LONG).show()
                false
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, getString(R.string.biometric_error_hw_unavailable), Toast.LENGTH_LONG).show()
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, getString(R.string.biometric_none_enrolled), Toast.LENGTH_LONG).show()
                false
            }

            else -> {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_LONG).show()
                false
            }
        }
    }
}