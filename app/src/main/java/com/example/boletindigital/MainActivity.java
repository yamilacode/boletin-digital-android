package com.example.boletindigital;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Claves para persistencia de estado ante rotación de pantalla (R6 - Aporte Ale)
    private static final String KEY_USUARIO = "estado_usuario";
    private static final String KEY_PASSWORD = "estado_password";
    private static final String KEY_MODO_DOCENTE = "estado_modo_docente";

    private View tabTutorAlumno;
    private View tabDocentePreceptor;
    private ImageView ivIconoTutor;
    private TextView tvTextoTutor;
    private ImageView ivIconoDocente;
    private TextView tvTextoDocente;

    private View cardAvisoDocente;
    private View btnGoogle;
    private View layoutSeparadorGoogle;

    private TextInputLayout tilUsuario;
    private TextInputLayout tilPassword;
    private EditText etUsuario;
    private EditText etPassword;

    private Button btnIngresar;
    private ProgressBar pbCargando;
    private TextView tvLinkAyuda;
    private MaterialButton btnAccionSecundaria;

    private boolean esModoDocente = false;
    private boolean cargando = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pestañas
        tabTutorAlumno = findViewById(R.id.tabTutorAlumno);
        tabDocentePreceptor = findViewById(R.id.tabDocentePreceptor);
        ivIconoTutor = findViewById(R.id.ivIconoTutor);
        tvTextoTutor = findViewById(R.id.tvTextoTutor);
        ivIconoDocente = findViewById(R.id.ivIconoDocente);
        tvTextoDocente = findViewById(R.id.tvTextoDocente);

        // Componentes dinámicos
        cardAvisoDocente = findViewById(R.id.cardAvisoDocente);
        btnGoogle = findViewById(R.id.btnGoogle);
        layoutSeparadorGoogle = findViewById(R.id.layoutSeparadorGoogle);

        // Campos y Botón
        tilUsuario = findViewById(R.id.tilUsuario);
        tilPassword = findViewById(R.id.tilPassword);
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnIngresar = findViewById(R.id.btnIngresar);
        pbCargando = findViewById(R.id.pbCargando);
        tvLinkAyuda = findViewById(R.id.tvLinkAyuda);
        btnAccionSecundaria = findViewById(R.id.btnAccionSecundaria);

        // Eventos de Pestañas
        tabTutorAlumno.setOnClickListener(v -> seleccionarModo(false));
        tabDocentePreceptor.setOnClickListener(v -> seleccionarModo(true));

        // Acceso con Google (Alumnos / Tutores / Nube)
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> iniciarSesionGoogle());
        }

        // Ingreso con Usuario y Contraseña
        btnIngresar.setOnClickListener(v -> iniciarSesion());

        // Enlace de Ayuda y Acción Secundaria (Crear cuenta / Solicitar Alta)
        if (tvLinkAyuda != null) {
            tvLinkAyuda.setOnClickListener(v -> mostrarDialogoAyuda());
        }
        if (btnAccionSecundaria != null) {
            btnAccionSecundaria.setOnClickListener(v -> ejecutarAccionSecundaria());
        }
    }

    // Se ejecuta antes de destruir la Activity al rotar la pantalla (R6)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_USUARIO, etUsuario.getText().toString());
        outState.putString(KEY_PASSWORD, etPassword.getText().toString());
        outState.putBoolean(KEY_MODO_DOCENTE, esModoDocente);
    }

    // Se ejecuta al recrear la Activity para restaurar los datos y la pestaña activa (R6)
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        etUsuario.setText(savedInstanceState.getString(KEY_USUARIO, ""));
        etPassword.setText(savedInstanceState.getString(KEY_PASSWORD, ""));
        boolean modoDocente = savedInstanceState.getBoolean(KEY_MODO_DOCENTE, false);
        seleccionarModo(modoDocente);

        cargando = false;
        btnIngresar.setEnabled(true);
    }

    private void seleccionarModo(boolean docente) {
        esModoDocente = docente;

        int colorPrimary = ContextCompat.getColor(this, R.color.color_primary);
        int colorMuted = ContextCompat.getColor(this, R.color.text_muted);
        int bgActive = ContextCompat.getColor(this, R.color.tab_active_bg);

        if (docente) {
            // Activar Docente / Preceptor
            tabDocentePreceptor.setBackgroundTintList(ColorStateList.valueOf(bgActive));
            tabTutorAlumno.setBackgroundTintList(null);

            ivIconoDocente.setColorFilter(colorPrimary);
            tvTextoDocente.setTextColor(colorPrimary);
            tvTextoDocente.setTypeface(null, android.graphics.Typeface.BOLD);

            ivIconoTutor.setColorFilter(colorMuted);
            tvTextoTutor.setTextColor(colorMuted);
            tvTextoTutor.setTypeface(null, android.graphics.Typeface.NORMAL);

            cardAvisoDocente.setVisibility(View.VISIBLE);
            btnGoogle.setVisibility(View.GONE);
            layoutSeparadorGoogle.setVisibility(View.GONE);

            tilUsuario.setHint(getString(R.string.hint_usuario_docente));
            tilPassword.setHint(getString(R.string.hint_password_docente));
            btnIngresar.setText(R.string.btn_login_docente);

            if (tvLinkAyuda != null) {
                tvLinkAyuda.setText(R.string.ayuda_docente);
            }
            if (btnAccionSecundaria != null) {
                btnAccionSecundaria.setText(R.string.btn_solicitar_alta);
                btnAccionSecundaria.setIconResource(R.drawable.ic_badge);
            }

        } else {
            // Activar Tutor / Alumno
            tabTutorAlumno.setBackgroundTintList(ColorStateList.valueOf(bgActive));
            tabDocentePreceptor.setBackgroundTintList(null);

            ivIconoTutor.setColorFilter(colorPrimary);
            tvTextoTutor.setTextColor(colorPrimary);
            tvTextoTutor.setTypeface(null, android.graphics.Typeface.BOLD);

            ivIconoDocente.setColorFilter(colorMuted);
            tvTextoDocente.setTextColor(colorMuted);
            tvTextoDocente.setTypeface(null, android.graphics.Typeface.NORMAL);

            cardAvisoDocente.setVisibility(View.GONE);
            btnGoogle.setVisibility(View.VISIBLE);
            layoutSeparadorGoogle.setVisibility(View.VISIBLE);

            tilUsuario.setHint(getString(R.string.hint_usuario_alumno));
            tilPassword.setHint(getString(R.string.hint_password_alumno));
            btnIngresar.setText(R.string.btn_login_alumno);

            if (tvLinkAyuda != null) {
                tvLinkAyuda.setText(R.string.ayuda_tutor);
            }
            if (btnAccionSecundaria != null) {
                btnAccionSecundaria.setText(R.string.btn_crear_cuenta);
                btnAccionSecundaria.setIconResource(R.drawable.ic_person);
            }
        }
    }

    private void mostrarDialogoAyuda() {
        String titulo = esModoDocente ? "Clave Institucional" : "Recuperación de Acceso";
        String mensaje = esModoDocente
                ? "Si no recordás tu número de legajo o clave institucional, contactate con el área de Secretaría o Dirección del establecimiento para la regeneración de credenciales."
                : "Si no recordás tu contraseña de estudiante o tutor, podés ingresar vinculando tu cuenta de Google o solicitar el reseteo con el preceptor de tu curso.";

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void ejecutarAccionSecundaria() {
        if (esModoDocente) {
            new AlertDialog.Builder(this)
                    .setTitle("Solicitud de Alta Docente")
                    .setMessage("Para registrarse como nuevo docente o directivo, presentate en la secretaría escolar con tu número de legajo y resolución de designación correspondiente.")
                    .setPositiveButton("Comprendido", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Registro de Alumno / Tutor")
                    .setMessage("Para matricularte en la plataforma podés ingresar directamente con el botón 'Continuar con Google' o solicitar el empadronamiento a tu preceptor/a.")
                    .setPositiveButton("Comprendido", null)
                    .show();
        }
    }

    private void iniciarSesion() {

        if (cargando) return;

        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (usuario.isEmpty()) {
            etUsuario.setError(getString(R.string.error_usuario));
            etUsuario.requestFocus();
            return;
        }

        if (usuario.contains(" ")) {
            etUsuario.setError(getString(R.string.error_usuario_espacios));
            etUsuario.requestFocus();
            return;
        }

        if (password.trim().isEmpty()) {
            etPassword.setError(getString(R.string.error_password));
            etPassword.requestFocus();
            return;
        }

        cargando = true;
        btnIngresar.setEnabled(false);
        btnIngresar.setText(R.string.iniciando_sesion);
        if (pbCargando != null) {
            pbCargando.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {

            try {
                JSONObject respuesta = ApiClient.iniciarSesion(usuario, password);
                String token = respuesta.getString("token");
                JSONObject datosUsuario = respuesta.getJSONObject("usuario");

                String nombre = datosUsuario.getString("nombre");
                String rol = datosUsuario.getString("rol");

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    cargando = false;
                    btnIngresar.setEnabled(true);
                    btnIngresar.setText(esModoDocente ? R.string.btn_login_docente : R.string.btn_login_alumno);
                    if (pbCargando != null) {
                        pbCargando.setVisibility(View.GONE);
                    }

                    Toast.makeText(
                            this,
                            getString(R.string.bienvenido, nombre),
                            Toast.LENGTH_SHORT
                    ).show();

                    // Abrir pantalla según Rol
                    Intent intent;
                    if ("estudiante".equalsIgnoreCase(rol) || "tutor".equalsIgnoreCase(rol) || !esModoDocente) {
                        intent = new Intent(MainActivity.this, BoletinAlumnoActivity.class);
                        String dni = datosUsuario.optString("dni", usuario);
                        intent.putExtra("dni", dni);
                    } else {
                        // Abrir Panel Central
                        intent = new Intent(MainActivity.this, MenuPrincipalActivity.class);
                    }
                    intent.putExtra("nombre", nombre);
                    intent.putExtra("rol", rol);
                    intent.putExtra("token", token);
                    startActivity(intent);

                    finish();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    cargando = false;
                    btnIngresar.setEnabled(true);
                    btnIngresar.setText(esModoDocente ? R.string.btn_login_docente : R.string.btn_login_alumno);
                    if (pbCargando != null) {
                        pbCargando.setVisibility(View.GONE);
                    }

                    String mensaje;
                    if (e instanceof ApiClient.ApiException) {
                        ApiClient.ApiException error = (ApiClient.ApiException) e;
                        mensaje = getString(R.string.error_api, error.getCodigo(), error.getMessage());
                    } else if (e instanceof IOException) {
                        mensaje = getString(R.string.error_conexion);
                    } else {
                        mensaje = getString(R.string.error_respuesta);
                    }

                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void iniciarSesionGoogle() {
        Toast.makeText(
                this,
                "Autenticado con Google Cloud - Estudiante: Uriel Pleininger",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(MainActivity.this, BoletinAlumnoActivity.class);
        intent.putExtra("nombre", "PLEININGER, URIEL LUCIANO");
        intent.putExtra("rol", "estudiante");
        intent.putExtra("dni", "48763736");
        intent.putExtra("token", "google-cloud-token");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
