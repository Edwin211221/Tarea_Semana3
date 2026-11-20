package com.example.loggin

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class VerificarCedulaActivity : AppCompatActivity() {

    private val requestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verificar_cedula)

        aplicarInsets(findViewById(R.id.cedulaContainer))

        val inputCedula = findViewById<EditText>(R.id.input_cedula)
        val botonVerificar = findViewById<Button>(R.id.btn_verificar)
        val resultado = findViewById<TextView>(R.id.tv_resultado)

        botonVerificar.setOnClickListener {
            val cedula = inputCedula.text.toString()
            if (!cedula.matches(Regex("[0-9]{10}"))) {
                mostrarResultado(resultado, "Ingresa una cédula válida de 10 dígitos.", R.color.result_error)
                inputCedula.requestFocus()
                return@setOnClickListener
            }

            ocultarTeclado(inputCedula)
            botonVerificar.isEnabled = false
            mostrarResultado(resultado, "Consultando…", R.color.text_secondary)

            val datos = JSONObject().apply {
                put("accion", "verificarCedula")
                put("cedula", cedula)
            }

            val solicitud = JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.PERSONA_URL,
                datos,
                { respuesta ->
                    botonVerificar.isEnabled = true
                    when (leerEstado(respuesta)) {
                        true -> mostrarResultado(
                            resultado,
                            "La cédula ya está registrada.",
                            R.color.result_error
                        )

                        false -> mostrarResultado(
                            resultado,
                            "La cédula está disponible.",
                            R.color.result_success
                        )

                        null -> mostrarResultado(
                            resultado,
                            "El servicio devolvió una respuesta inválida.",
                            R.color.result_error
                        )
                    }
                },
                { error ->
                    botonVerificar.isEnabled = true
                    mostrarResultado(resultado, mensajeDeError(error), R.color.result_error)
                }
            ).apply {
                setShouldCache(false)
            }

            requestQueue.add(solicitud)
        }
    }

    private fun leerEstado(respuesta: JSONObject): Boolean? = when (val estado = respuesta.opt("estado")) {
        is Boolean -> estado
        is String -> when (estado.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
        else -> null
    }

    private fun mensajeDeError(error: VolleyError): String = when (error) {
        is NoConnectionError -> "Sin conexión con el servicio. Revisa tu red."
        is TimeoutError -> "El servicio tardó demasiado. Inténtalo de nuevo."
        is ServerError -> "El servicio respondió con un error."
        else -> "No se pudo completar la consulta. Inténtalo de nuevo."
    }

    private fun mostrarResultado(resultado: TextView, mensaje: String, colorRes: Int) {
        resultado.text = mensaje
        resultado.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun ocultarTeclado(campo: EditText) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(campo.windowToken, 0)
        campo.clearFocus()
    }

    private fun aplicarInsets(contenedor: ScrollView) {
        val paddingLeft = contenedor.paddingLeft
        val paddingTop = contenedor.paddingTop
        val paddingRight = contenedor.paddingRight
        val paddingBottom = contenedor.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(contenedor) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                paddingLeft + systemBars.left,
                paddingTop + systemBars.top,
                paddingRight + systemBars.right,
                paddingBottom + systemBars.bottom
            )
            windowInsets
        }
    }

    companion object {
        const val EXTRA_CODIGO_USUARIO = "codigo_usuario"
    }
}
