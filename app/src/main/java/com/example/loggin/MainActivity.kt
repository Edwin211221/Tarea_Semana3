package com.example.loggin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {

    private val requestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        aplicarInsets(findViewById(R.id.loginContainer))

        val usuario = findViewById<EditText>(R.id.txt_usu)
        val clave = findViewById<EditText>(R.id.txt_cla)
        val botonIngresar = findViewById<Button>(R.id.btn_ingresar)

        botonIngresar.setOnClickListener {
            val nombreUsuario = usuario.text.toString().trim()
            val password = clave.text.toString()

            if (nombreUsuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa tu usuario y contraseña.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ocultarTeclado(clave)
            botonIngresar.isEnabled = false

            val datos = JSONObject().apply {
                put("accion", "loggin")
                put("usuario", nombreUsuario)
                put("clave", password)
            }

            val solicitud = JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.PERSONA_URL,
                datos,
                { respuesta ->
                    botonIngresar.isEnabled = true
                    when (val estado = leerEstado(respuesta)) {
                        true -> {
                            val codigo = respuesta.optString("codigo")
                            if (codigo.isBlank()) {
                                Toast.makeText(
                                    this,
                                    "El servicio devolvió una respuesta inválida.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                startActivity(
                                    Intent(this, VerificarCedulaActivity::class.java)
                                        .putExtra(VerificarCedulaActivity.EXTRA_CODIGO_USUARIO, codigo)
                                )
                            }
                        }

                        false -> Toast.makeText(
                            this,
                            respuesta.optString("mensaje", "Usuario o contraseña incorrectos."),
                            Toast.LENGTH_LONG
                        ).show()

                        null -> Toast.makeText(
                            this,
                            "El servicio devolvió una respuesta inválida.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                { error ->
                    botonIngresar.isEnabled = true
                    Toast.makeText(this, mensajeDeError(error), Toast.LENGTH_LONG).show()
                }
            )

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
        else -> "No se pudo iniciar sesión. Inténtalo de nuevo."
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

}
