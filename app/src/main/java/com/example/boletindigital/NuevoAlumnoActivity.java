package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NuevoAlumnoActivity extends AppCompatActivity {

    // Claves para persistencia ante rotación de pantalla (R6 - Aporte Ale)
    private static final String KEY_DNI = "estado_dni";
    private static final String KEY_NOMBRE = "estado_nombre";
    private static final String KEY_APELLIDO = "estado_apellido";
    private static final String KEY_JURISDICCION = "estado_jurisdiccion";
    private static final String KEY_INSTITUCION = "estado_institucion";
    private static final String KEY_CURSO = "estado_curso";
    private static final String KEY_SECCION = "estado_seccion";
    private static final String KEY_TUTOR = "estado_tutor";

    // Expresiones regulares para validaciones (R7 - Aporte Ale)
    private static final String REGEX_DNI = "^\\d{7,8}$";
    private static final String REGEX_SOLO_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$";

    private EditText etDni, etNombre, etApellido;
    private EditText etJurisdiccion, etInstitucion;
    private EditText etCurso, etSeccion, etTutor;
    private View btnVolverNuevoAlumno;

    private Button btnGuardar;
    private String token;
    private boolean enviando = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> confirmar =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::alVolverDeConfirmacion);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nuevo_alumno);

        etDni = findViewById(R.id.etDni);
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etJurisdiccion = findViewById(R.id.etJurisdiccion);
        etInstitucion = findViewById(R.id.etInstitucion);
        etCurso = findViewById(R.id.etCurso);
        etSeccion = findViewById(R.id.etSeccion);
        etTutor = findViewById(R.id.etTutor);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVolverNuevoAlumno = findViewById(R.id.btnVolverNuevoAlumno);

        if (btnVolverNuevoAlumno != null) {
            btnVolverNuevoAlumno.setOnClickListener(v -> finish());
        }

        token = getIntent().getStringExtra("token");

        btnGuardar.setOnClickListener(v -> validarDatos());
    }

    // Guarda lo que el usuario escribió antes de rotar la pantalla (R6)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_DNI, texto(etDni));
        outState.putString(KEY_NOMBRE, texto(etNombre));
        outState.putString(KEY_APELLIDO, texto(etApellido));
        outState.putString(KEY_JURISDICCION, texto(etJurisdiccion));
        outState.putString(KEY_INSTITUCION, texto(etInstitucion));
        outState.putString(KEY_CURSO, texto(etCurso));
        outState.putString(KEY_SECCION, texto(etSeccion));
        outState.putString(KEY_TUTOR, texto(etTutor));
    }

    // Recupera los datos tras recrear la Activity al rotar (R6)
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        etDni.setText(savedInstanceState.getString(KEY_DNI, ""));
        etNombre.setText(savedInstanceState.getString(KEY_NOMBRE, ""));
        etApellido.setText(savedInstanceState.getString(KEY_APELLIDO, ""));
        etJurisdiccion.setText(savedInstanceState.getString(KEY_JURISDICCION, ""));
        etInstitucion.setText(savedInstanceState.getString(KEY_INSTITUCION, ""));
        etCurso.setText(savedInstanceState.getString(KEY_CURSO, ""));
        etSeccion.setText(savedInstanceState.getString(KEY_SECCION, ""));
        etTutor.setText(savedInstanceState.getString(KEY_TUTOR, ""));
    }

    private String texto(EditText campo) {
        return campo != null ? campo.getText().toString() : "";
    }

    private void validarDatos() {

        if (enviando) return;

        EditText[] campos = {
                etDni, etNombre, etApellido,
                etJurisdiccion, etInstitucion,
                etCurso, etSeccion, etTutor
        };

        // 1. Campos obligatorios
        for (EditText campo : campos) {
            if (campo.getText().toString().trim().isEmpty()) {
                campo.setError(getString(R.string.error_campo_obligatorio));
                campo.requestFocus();
                return;
            }
        }

        // 2. DNI numérico obligatorio (7 u 8 dígitos)
        String dni = etDni.getText().toString().trim();
        if (!dni.matches(REGEX_DNI)) {
            etDni.setError(getString(R.string.error_dni));
            etDni.requestFocus();
            return;
        }

        // 3. Nombre, apellido y tutor: solo letras
        EditText[] camposTexto = {etNombre, etApellido, etTutor};
        for (EditText campo : camposTexto) {
            if (!campo.getText().toString().trim().matches(REGEX_SOLO_LETRAS)) {
                campo.setError(getString(R.string.error_solo_letras));
                campo.requestFocus();
                return;
            }
        }

        // 4. ID de institución: número entero mayor a 0
        int institucion;
        try {
            institucion = Integer.parseInt(
                    etInstitucion.getText().toString().trim());

            if (institucion <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            etInstitucion.setError(getString(R.string.error_institucion));
            etInstitucion.requestFocus();
            return;
        }

        // Confirmar antes de realizar el POST
        Intent intent = new Intent(
                this, ConfirmarAlumnoActivity.class);

        intent.putExtra("nombre",
                etNombre.getText().toString().trim());

        intent.putExtra("apellido",
                etApellido.getText().toString().trim());

        intent.putExtra("dni", dni);

        confirmar.launch(intent);
    }

    private void alVolverDeConfirmacion(ActivityResult resultado) {

        if (resultado.getResultCode() == RESULT_OK) {
            guardarAlumno();
        }
    }

    private void guardarAlumno() {

        if (enviando) return;

        if (token == null || token.isEmpty()) {
            Toast.makeText(this,
                    R.string.error_sin_sesion,
                    Toast.LENGTH_LONG).show();
            return;
        }

        enviando = true;
        btnGuardar.setEnabled(false);

        try {
            JSONObject datos = new JSONObject();

            datos.put("dni", etDni.getText().toString().trim());
            datos.put("nombre", etNombre.getText().toString().trim());
            datos.put("apellido", etApellido.getText().toString().trim());
            datos.put("jurisdiccion",
                    etJurisdiccion.getText().toString().trim());
            datos.put("id_institucion", Integer.parseInt(
                    etInstitucion.getText().toString().trim()));
            datos.put("curso", etCurso.getText().toString().trim());
            datos.put("seccion", etSeccion.getText().toString().trim());
            datos.put("nombre_tutor", etTutor.getText().toString().trim());

            executor.execute(() -> {

                try {
                    JSONObject respuesta =
                            ApiClient.crearAlumno(datos, token);

                    String mensaje = respuesta.optString(
                            "mensaje", getString(R.string.operacion_completada));

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        enviando = false;

                        Toast.makeText(
                                this, mensaje, Toast.LENGTH_LONG).show();

                        setResult(RESULT_OK);
                        finish();
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        enviando = false;
                        btnGuardar.setEnabled(true);

                        String mensaje = e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : e instanceof IOException
                            ? getString(R.string.error_conexion)
                            : getString(R.string.error_respuesta);

                        Toast.makeText(
                                this, mensaje, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            enviando = false;
            btnGuardar.setEnabled(true);
            Toast.makeText(this,
                    R.string.error_datos_invalidos, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
