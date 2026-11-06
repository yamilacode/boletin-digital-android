package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Claves para guardar el estado al rotar la pantalla (R6)
    private static final String KEY_USUARIO = "estado_usuario";
    private static final String KEY_PASSWORD = "estado_password";

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

    // Se llama antes de que la Activity se destruya (por ejemplo, al girar)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_USUARIO,
                etUsuario.getText().toString());

        outState.putString(KEY_PASSWORD,
                etPassword.getText().toString());
    }

    // Se llama después de onStart() cuando la Activity se recrea
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        etUsuario.setText(
                savedInstanceState.getString(KEY_USUARIO, ""));

        etPassword.setText(
                savedInstanceState.getString(KEY_PASSWORD, ""));

        // Si la petición anterior quedó en la Activity vieja,
        // se vuelve a habilitar el botón en la nueva
        cargando = false;
        btnIngresar.setEnabled(true);
    }

    private void iniciarSesion() {

        if (cargando) {
            return;
        }

        String usuario =
                etUsuario.getText().toString().trim();

        String password =
                etPassword.getText().toString();

        // Validaciones de campos requeridos antes de enviar
        if (usuario.isEmpty()) {
            etUsuario.setError(
                    getString(R.string.error_usuario));
            etUsuario.requestFocus();
            return;
        }

        if (usuario.contains(" ")) {
            etUsuario.setError(
                    getString(R.string.error_usuario_espacios));
            etUsuario.requestFocus();
            return;
        }

        if (password.trim().isEmpty()) {
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

                        mensaje = getString(
                                R.string.error_api,
                                error.getCodigo(),
                                error.getMessage());

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
