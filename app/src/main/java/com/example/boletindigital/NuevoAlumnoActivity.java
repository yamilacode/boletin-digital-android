package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    private static final String KEY_EMAIL_TUTOR = "estado_email_tutor";
    private static final String KEY_CLAVE_ACCESO = "estado_clave_acceso";

    // Expresiones regulares para validaciones (R7 - Aporte Ale)
    private static final String REGEX_DNI = "^\\d{7,8}$";
    private static final String REGEX_SOLO_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ.,' -]+$";

    private EditText etDni, etNombre, etApellido;
    private EditText etJurisdiccion, etInstitucion;
    private EditText etCurso, etSeccion, etTutor, etEmailTutor, etClaveAlumno;
    private TextView tvPreviewCredenciales;
    private View btnVolverNuevoAlumno;
    private TextView tvTituloModoAlumno;

    private Button btnGuardar;
    private String token;
    private int idAlumnoEditar = -1;
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
        etEmailTutor = findViewById(R.id.etEmailTutor);
        etClaveAlumno = findViewById(R.id.etClaveAlumno);
        tvPreviewCredenciales = findViewById(R.id.tvPreviewCredenciales);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVolverNuevoAlumno = findViewById(R.id.btnVolverNuevoAlumno);
        tvTituloModoAlumno = findViewById(R.id.tvTituloModoAlumno);

        if (btnVolverNuevoAlumno != null) {
            btnVolverNuevoAlumno.setOnClickListener(v -> finish());
        }

        token = getIntent().getStringExtra("token");
        idAlumnoEditar = getIntent().getIntExtra("id_alumno", -1);
        DocenteUiHelper.configurarFooterDocente(
                this,
                token,
                getIntent().getStringExtra("rol"),
                getIntent().getStringExtra("nombre"));

        if (idAlumnoEditar > 0) {
            if (tvTituloModoAlumno != null) tvTituloModoAlumno.setText("EDITAR ALUMNO");
            if (btnGuardar != null) btnGuardar.setText("GUARDAR CAMBIOS");
        }

        if (savedInstanceState == null) {
            if (idAlumnoEditar > 0) {
                etDni.setText(valorExtra("alumno_dni"));
                etNombre.setText(valorExtra("alumno_nombre"));
                etApellido.setText(valorExtra("alumno_apellido"));
                etJurisdiccion.setText(valorExtra("alumno_jurisdiccion"));
                int instExtra = getIntent().getIntExtra("alumno_institucion", InstitucionManager.getIdInstitucionActiva(this));
                etInstitucion.setText(String.valueOf(instExtra > 0 ? instExtra : InstitucionManager.getIdInstitucionActiva(this)));
                etCurso.setText(valorExtra("alumno_curso"));
                etSeccion.setText(valorExtra("alumno_seccion"));
                etTutor.setText(valorExtra("alumno_tutor"));
                if (etEmailTutor != null) etEmailTutor.setText(valorExtra("alumno_email_tutor"));
                if (etClaveAlumno != null) etClaveAlumno.setText(valorExtra("alumno_clave_acceso"));
            } else {
                int idInstActiva = InstitucionManager.getIdInstitucionActiva(this);
                if (etJurisdiccion != null && texto(etJurisdiccion).isEmpty()) {
                    etJurisdiccion.setText(InstitucionManager.getJurisdiccionActiva(this));
                }
                if (etInstitucion != null && texto(etInstitucion).isEmpty()) {
                    etInstitucion.setText(String.valueOf(idInstActiva));
                }
            }
        }

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarPreviewCredenciales();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        if (etDni != null) etDni.addTextChangedListener(previewWatcher);
        if (etClaveAlumno != null) etClaveAlumno.addTextChangedListener(previewWatcher);
        actualizarPreviewCredenciales();

        btnGuardar.setOnClickListener(v -> validarDatos());
    }

    private String calcularClaveProvisoria(String dni, String claveManual) {
        String manual = claveManual != null ? claveManual.trim() : "";
        if (!manual.isEmpty()) return manual;
        String dniLimpio = dni != null ? dni.replaceAll("\\D+", "") : "";
        if (dniLimpio.length() >= 4) {
            return dniLimpio.substring(dniLimpio.length() - 4);
        }
        return dniLimpio.isEmpty() ? "últimos 4 del DNI" : dniLimpio;
    }

    private void actualizarPreviewCredenciales() {
        if (tvPreviewCredenciales == null) return;
        String dniActual = texto(etDni).trim();
        String claveManual = texto(etClaveAlumno).trim();
        if (dniActual.isEmpty()) {
            tvPreviewCredenciales.setText("Acceso clásico del alumno: Usuario = DNI • Clave inicial = últimos 4 dígitos del DNI");
        } else {
            String clave = calcularClaveProvisoria(dniActual, claveManual);
            tvPreviewCredenciales.setText("Acceso al portal: Usuario " + dniActual + " • Clave inicial: " + clave);
        }
    }

    private String valorExtra(String clave) {
        String val = getIntent().getStringExtra(clave);
        return val != null ? val : "";
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
        outState.putString(KEY_EMAIL_TUTOR, texto(etEmailTutor));
        outState.putString(KEY_CLAVE_ACCESO, texto(etClaveAlumno));
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
        if (etEmailTutor != null) etEmailTutor.setText(savedInstanceState.getString(KEY_EMAIL_TUTOR, ""));
        if (etClaveAlumno != null) etClaveAlumno.setText(savedInstanceState.getString(KEY_CLAVE_ACCESO, ""));
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
        intent.putExtra("clave_provisoria", calcularClaveProvisoria(dni, texto(etClaveAlumno)));
        intent.putExtra("token", token);
        intent.putExtra("rol", getIntent().getStringExtra("rol"));
        intent.putExtra("nombre_usuario", getIntent().getStringExtra("nombre"));
        intent.putExtra("modo_edicion", idAlumnoEditar > 0);

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
            String dniGuardar = etDni.getText().toString().trim();
            String emailTutorStr = texto(etEmailTutor).trim();
            String claveManualStr = texto(etClaveAlumno).trim();
            String claveFinal = calcularClaveProvisoria(dniGuardar, claveManualStr);

            JSONObject datos = new JSONObject();

            datos.put("dni", dniGuardar);
            datos.put("nombre", etNombre.getText().toString().trim());
            datos.put("apellido", etApellido.getText().toString().trim());
            datos.put("jurisdiccion",
                    etJurisdiccion.getText().toString().trim());
            datos.put("id_institucion", Integer.parseInt(
                    etInstitucion.getText().toString().trim()));
            datos.put("curso", etCurso.getText().toString().trim());
            datos.put("seccion", etSeccion.getText().toString().trim());
            datos.put("nombre_tutor", etTutor.getText().toString().trim());
            if (!emailTutorStr.isEmpty()) {
                datos.put("email_tutor", emailTutorStr);
            }
            datos.put("clave_acceso", claveFinal);

            executor.execute(() -> {

                try {
                    JSONObject respuesta = idAlumnoEditar > 0
                            ? ApiClient.actualizarAlumno(idAlumnoEditar, datos, token)
                            : ApiClient.crearAlumno(datos, token);

                    String mensaje = respuesta.optString(
                            "mensaje", getString(R.string.operacion_completada));
                    JSONObject credResp = respuesta.optJSONObject("credenciales_alumno");
                    String userResp = credResp != null ? credResp.optString("usuario", dniGuardar) : dniGuardar;
                    String passResp = credResp != null ? credResp.optString("clave_provisoria", claveFinal) : claveFinal;

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        enviando = false;

                        if (idAlumnoEditar <= 0) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Alta Exitosa")
                                    .setMessage(mensaje
                                            + "\n\nCredenciales para que el alumno ingrese al portal:\n"
                                            + "• Usuario (DNI): " + userResp + "\n"
                                            + "• Clave inicial provisoria: " + passResp)
                                    .setCancelable(false)
                                    .setPositiveButton("Aceptar", (d, w) -> {
                                        setResult(RESULT_OK);
                                        finish();
                                    })
                                    .show();
                        } else {
                            Toast.makeText(
                                    this, mensaje, Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                        }
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
