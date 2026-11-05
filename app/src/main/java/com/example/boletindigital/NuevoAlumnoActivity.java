
package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NuevoAlumnoActivity extends AppCompatActivity {

    private EditText etDni, etNombre, etApellido;
    private EditText etJurisdiccion, etInstitucion;
    private EditText etCurso, etSeccion, etTutor;

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

        token = getIntent().getStringExtra("token");

        btnGuardar.setOnClickListener(v -> validarDatos());
    }

    private void validarDatos() {

        if (enviando) return;

        EditText[] campos = {
                etDni, etNombre, etApellido,
                etJurisdiccion, etInstitucion,
                etCurso, etSeccion, etTutor
        };

        for (EditText campo : campos) {
            if (campo.getText().toString().trim().isEmpty()) {
                campo.setError("Campo obligatorio");
                campo.requestFocus();
                return;
            }
        }

        String dni = etDni.getText().toString().trim();

        if (!dni.matches("\\d{8}")) {
            etDni.setError("El DNI debe tener 8 números");
            etDni.requestFocus();
            return;
        }

        int institucion;

        try {
            institucion = Integer.parseInt(
                    etInstitucion.getText().toString().trim());

            if (institucion <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            etInstitucion.setError("Institución inválida");
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
                    "No hay sesión activa",
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
                            "mensaje", "Operación completada");

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
                    "Datos inválidos", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
