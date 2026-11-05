
package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etUsuario;
    private EditText etPassword;
    private Button btnIngresar;

    private boolean cargando = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnIngresar = findViewById(R.id.btnIngresar);

        btnIngresar.setOnClickListener(v -> iniciarSesion());
    }

    private void iniciarSesion() {

        if (cargando) {
            return;
        }

        String usuario =
                etUsuario.getText().toString().trim();

        String password =
                etPassword.getText().toString();

        // Validación antes de enviar
        if (usuario.isEmpty()) {
            etUsuario.setError(
                    getString(R.string.error_usuario));
            etUsuario.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError(
                    getString(R.string.error_password));
            etPassword.requestFocus();
            return;
        }

        cargando = true;
        btnIngresar.setEnabled(false);

        // La conexión no se hace en el hilo principal
        executor.execute(() -> {

            try {
                JSONObject respuesta =
                        ApiClient.iniciarSesion(usuario, password);

                String token = respuesta.getString("token");

                JSONObject datosUsuario =
                        respuesta.getJSONObject("usuario");

                String nombre =
                        datosUsuario.getString("nombre");

                String rol =
                        datosUsuario.getString("rol");

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    cargando = false;
                    btnIngresar.setEnabled(true);

                    Toast.makeText(
                            this,
                            getString(R.string.bienvenido, nombre),
                            Toast.LENGTH_SHORT
                    ).show();

                    // Abrir listado y enviar datos por Intent
                    Intent intent = new Intent(
                            MainActivity.this,
                            AlumnosActivity.class);

                    intent.putExtra("nombre", nombre);
                    intent.putExtra("rol", rol);
                    intent.putExtra("token", token);

                    startActivity(intent);

                    // Evitar volver al login con Atrás
                    finish();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    cargando = false;
                    btnIngresar.setEnabled(true);

                    String mensaje;

                    if (e instanceof ApiClient.ApiException) {
                        ApiClient.ApiException error =
                                (ApiClient.ApiException) e;

                        mensaje = "API (" + error.getCodigo()
                                + "): " + error.getMessage();

                    } else if (e instanceof IOException) {
                        mensaje = getString(
                                R.string.error_conexion);

                    } else {
                        mensaje = getString(
                                R.string.error_respuesta);
                    }

                    Toast.makeText(
                            this, mensaje, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
