package com.example.boletindigital;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Pantalla de perfil institucional para el rol Preceptor / Docente.
 * Permite consultar, editar y eliminar los datos del perfil, cerrar sesión o eliminar la cuenta local.
 */
public class PerfilDocenteActivity extends AppCompatActivity {

    private static final String PREFS_PERFIL = "boletin_perfil_docente";
    private static final String KEY_INICIALIZADO = "inicializado";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_DNI = "dni";
    private static final String KEY_LEGAJO = "legajo";
    private static final String KEY_CARGO = "cargo";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TELEFONO = "telefono";
    private static final String KEY_TURNO = "turno";
    private static final String KEY_OFICINA = "oficina";

    private static final String DEFAULT_NOMBRE = "Prof. Daniel Martínez";
    private static final String DEFAULT_DNI = "31.482.905";
    private static final String DEFAULT_LEGAJO = "LEG-DOC-2026-0148";
    private static final String DEFAULT_CARGO = "Preceptor/a Titular • Nivel Secundario";
    private static final String DEFAULT_EMAIL = "preceptor@escuela.edu.ar";
    private static final String DEFAULT_TELEFONO = "+54 9 370 462-8410";
    private static final String DEFAULT_TURNO = "Turno Mañana y Tarde • 1° a 6° Año";
    private static final String DEFAULT_OFICINA = "Preceptoría Central • Planta Baja";

    private TextView tvPerfilIniciales;
    private TextView tvPerfilNombreHeader;
    private TextView tvPerfilCargoHeader;
    private TextView tvPerfilInstitucionBadge;
    private TextView tvBadgeRolPerfil;

    private TextView tvValorPerfilNombre;
    private TextView tvValorPerfilDni;
    private TextView tvValorPerfilLegajo;
    private TextView tvValorPerfilCargo;
    private TextView tvValorPerfilEmail;
    private TextView tvValorPerfilTelefono;
    private TextView tvValorPerfilTurno;
    private TextView tvValorPerfilOficina;

    private String token;
    private String rol;
    private String nombreIntent;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_docente);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombreIntent = getIntent().getStringExtra("nombre");
        prefs = getSharedPreferences(PREFS_PERFIL, MODE_PRIVATE);

        inicializarDatosPorDefecto();
        vincularVistas();
        renderizarPerfil();

        String nombreActivo = obtenerNombreActivo(this, nombreIntent);
        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombreActivo, "perfil");
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderizarPerfil();
    }

    public static String obtenerNombreActivo(Context context, String fallback) {
        SharedPreferences p = context.getSharedPreferences(PREFS_PERFIL, MODE_PRIVATE);
        if (p.contains(KEY_NOMBRE)) {
            String guardado = p.getString(KEY_NOMBRE, "");
            if (guardado != null && !guardado.trim().isEmpty()) {
                return guardado.trim();
            }
        }
        return (fallback != null && !fallback.trim().isEmpty()) ? fallback.trim() : DEFAULT_NOMBRE;
    }

    public static void limpiarPerfilLocal(Context context) {
        context.getSharedPreferences(PREFS_PERFIL, MODE_PRIVATE).edit().clear().apply();
    }

    private void inicializarDatosPorDefecto() {
        if (!prefs.getBoolean(KEY_INICIALIZADO, false)) {
            String nombreInicial = DEFAULT_NOMBRE;
            if (nombreIntent != null
                    && !nombreIntent.trim().isEmpty()
                    && !"preceptor/a escolar".equalsIgnoreCase(nombreIntent.trim())
                    && !"usuario".equalsIgnoreCase(nombreIntent.trim())) {
                nombreInicial = nombreIntent.trim();
            }

            String cargoInicial = "docente".equalsIgnoreCase(rol)
                    ? "Docente Titular • Nivel Secundario"
                    : DEFAULT_CARGO;

            prefs.edit()
                    .putBoolean(KEY_INICIALIZADO, true)
                    .putString(KEY_NOMBRE, nombreInicial)
                    .putString(KEY_DNI, DEFAULT_DNI)
                    .putString(KEY_LEGAJO, DEFAULT_LEGAJO)
                    .putString(KEY_CARGO, cargoInicial)
                    .putString(KEY_EMAIL, DEFAULT_EMAIL)
                    .putString(KEY_TELEFONO, DEFAULT_TELEFONO)
                    .putString(KEY_TURNO, DEFAULT_TURNO)
                    .putString(KEY_OFICINA, DEFAULT_OFICINA)
                    .apply();
        }
    }

    private void vincularVistas() {
        ImageView ivVolverPerfil = findViewById(R.id.ivVolverPerfil);
        tvPerfilIniciales = findViewById(R.id.tvPerfilIniciales);
        tvPerfilNombreHeader = findViewById(R.id.tvPerfilNombreHeader);
        tvPerfilCargoHeader = findViewById(R.id.tvPerfilCargoHeader);
        tvPerfilInstitucionBadge = findViewById(R.id.tvPerfilInstitucionBadge);
        tvBadgeRolPerfil = findViewById(R.id.tvBadgeRolPerfil);

        tvValorPerfilNombre = findViewById(R.id.tvValorPerfilNombre);
        tvValorPerfilDni = findViewById(R.id.tvValorPerfilDni);
        tvValorPerfilLegajo = findViewById(R.id.tvValorPerfilLegajo);
        tvValorPerfilCargo = findViewById(R.id.tvValorPerfilCargo);
        tvValorPerfilEmail = findViewById(R.id.tvValorPerfilEmail);
        tvValorPerfilTelefono = findViewById(R.id.tvValorPerfilTelefono);
        tvValorPerfilTurno = findViewById(R.id.tvValorPerfilTurno);
        tvValorPerfilOficina = findViewById(R.id.tvValorPerfilOficina);

        ImageView btnEditarDatosPersonales = findViewById(R.id.btnEditarDatosPersonales);
        ImageView btnEliminarDatosPersonales = findViewById(R.id.btnEliminarDatosPersonales);
        ImageView btnEditarContacto = findViewById(R.id.btnEditarContacto);
        ImageView btnEliminarContacto = findViewById(R.id.btnEliminarContacto);
        MaterialButton btnCerrarSesionPerfil = findViewById(R.id.btnCerrarSesionPerfil);
        MaterialButton btnEliminarCuentaPerfil = findViewById(R.id.btnEliminarCuentaPerfil);

        if (ivVolverPerfil != null) {
            ivVolverPerfil.setOnClickListener(v -> finish());
        }

        DocenteUiHelper.aplicarBadgeRol(tvBadgeRolPerfil, rol != null ? rol : "PRECEPTOR");

        btnEditarDatosPersonales.setOnClickListener(v -> abrirDialogoEditarDatosPersonales());
        btnEliminarDatosPersonales.setOnClickListener(v -> confirmarEliminarBloque(
                "Eliminar Datos Personales",
                "¿Querés borrar tus datos personales y cargo guardados en el perfil?",
                new String[]{KEY_NOMBRE, KEY_DNI, KEY_LEGAJO, KEY_CARGO},
                "Datos personales eliminados"
        ));

        btnEditarContacto.setOnClickListener(v -> abrirDialogoEditarContacto());
        btnEliminarContacto.setOnClickListener(v -> confirmarEliminarBloque(
                "Eliminar Datos de Contacto",
                "¿Querés borrar tu correo, teléfono y asignación laboral del perfil?",
                new String[]{KEY_EMAIL, KEY_TELEFONO, KEY_TURNO, KEY_OFICINA},
                "Datos de contacto eliminados"
        ));

        btnCerrarSesionPerfil.setOnClickListener(v -> confirmarCerrarSesion());
        btnEliminarCuentaPerfil.setOnClickListener(v -> confirmarEliminarCuenta());
    }

    private void renderizarPerfil() {
        String nom = prefs.getString(KEY_NOMBRE, "");
        String dni = prefs.getString(KEY_DNI, "");
        String legajo = prefs.getString(KEY_LEGAJO, "");
        String cargo = prefs.getString(KEY_CARGO, "");
        String email = prefs.getString(KEY_EMAIL, "");
        String tel = prefs.getString(KEY_TELEFONO, "");
        String turno = prefs.getString(KEY_TURNO, "");
        String oficina = prefs.getString(KEY_OFICINA, "");

        String nombreVisible = !nom.trim().isEmpty() ? nom.trim() : "Perfil Institucional";
        tvPerfilNombreHeader.setText(nombreVisible);
        tvPerfilCargoHeader.setText(!cargo.trim().isEmpty() ? cargo.trim() : "Preceptor / Docente");
        tvPerfilInstitucionBadge.setText(InstitucionManager.getNombreEscuelaActiva(this));
        tvPerfilIniciales.setText(extraerIniciales(nombreVisible));

        renderValorCampo(tvValorPerfilNombre, nom);
        renderValorCampo(tvValorPerfilDni, dni);
        renderValorCampo(tvValorPerfilLegajo, legajo);
        renderValorCampo(tvValorPerfilCargo, cargo);
        renderValorCampo(tvValorPerfilEmail, email);
        renderValorCampo(tvValorPerfilTelefono, tel);
        renderValorCampo(tvValorPerfilTurno, turno);
        renderValorCampo(tvValorPerfilOficina, oficina);

        DocenteUiHelper.configurarFooterDocente(this, token, rol, obtenerNombreActivo(this, nombreIntent), "perfil");
    }

    private void renderValorCampo(TextView tv, String valor) {
        if (valor != null && !valor.trim().isEmpty()) {
            tv.setText(valor.trim());
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        } else {
            tv.setText("Sin especificar");
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
        }
    }

    private void abrirDialogoEditarDatosPersonales() {
        LinearLayout contenedor = crearContenedorDialogo();
        EditText etNombre = agregarCampoDialogo(contenedor, "Nombre y Apellido", prefs.getString(KEY_NOMBRE, ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText etDni = agregarCampoDialogo(contenedor, "DNI", prefs.getString(KEY_DNI, ""), InputType.TYPE_CLASS_TEXT);
        EditText etLegajo = agregarCampoDialogo(contenedor, "Legajo Institucional", prefs.getString(KEY_LEGAJO, ""), InputType.TYPE_CLASS_TEXT);
        EditText etCargo = agregarCampoDialogo(contenedor, "Cargo / Título", prefs.getString(KEY_CARGO, ""), InputType.TYPE_CLASS_TEXT);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(contenedor);

        new AlertDialog.Builder(this)
                .setTitle("Editar Datos Personales")
                .setView(scroll)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    prefs.edit()
                            .putBoolean(KEY_INICIALIZADO, true)
                            .putString(KEY_NOMBRE, textoDe(etNombre))
                            .putString(KEY_DNI, textoDe(etDni))
                            .putString(KEY_LEGAJO, textoDe(etLegajo))
                            .putString(KEY_CARGO, textoDe(etCargo))
                            .apply();
                    renderizarPerfil();
                    Toast.makeText(this, "Datos personales actualizados", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirDialogoEditarContacto() {
        LinearLayout contenedor = crearContenedorDialogo();
        EditText etEmail = agregarCampoDialogo(contenedor, "Correo Institucional", prefs.getString(KEY_EMAIL, ""), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText etTelefono = agregarCampoDialogo(contenedor, "Teléfono", prefs.getString(KEY_TELEFONO, ""), InputType.TYPE_CLASS_PHONE);
        EditText etTurno = agregarCampoDialogo(contenedor, "Turno y Cursos", prefs.getString(KEY_TURNO, ""), InputType.TYPE_CLASS_TEXT);
        EditText etOficina = agregarCampoDialogo(contenedor, "Oficina / Área", prefs.getString(KEY_OFICINA, ""), InputType.TYPE_CLASS_TEXT);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(contenedor);

        new AlertDialog.Builder(this)
                .setTitle("Editar Contacto y Asignación")
                .setView(scroll)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    prefs.edit()
                            .putBoolean(KEY_INICIALIZADO, true)
                            .putString(KEY_EMAIL, textoDe(etEmail))
                            .putString(KEY_TELEFONO, textoDe(etTelefono))
                            .putString(KEY_TURNO, textoDe(etTurno))
                            .putString(KEY_OFICINA, textoDe(etOficina))
                            .apply();
                    renderizarPerfil();
                    Toast.makeText(this, "Contacto y asignación actualizados", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private LinearLayout crearContenedorDialogo() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        ll.setPadding(pad, dp(12), pad, dp(4));
        return ll;
    }

    private EditText agregarCampoDialogo(LinearLayout parent, String label, String valorInicial, int inputType) {
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12f);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
        tvLabel.setPadding(0, dp(8), 0, dp(4));
        parent.addView(tvLabel);

        EditText et = new EditText(this);
        et.setInputType(inputType);
        et.setText(valorInicial != null ? valorInicial : "");
        et.setHint("Ingresá " + label.toLowerCase());
        et.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        et.setHintTextColor(ContextCompat.getColor(this, R.color.text_dim));
        et.setTextSize(14f);
        et.setBackgroundResource(R.drawable.bg_input_web);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        parent.addView(et);
        return et;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private String textoDe(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void confirmarEliminarBloque(String titulo, String mensaje, String[] claves, String toastOk) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    for (String k : claves) {
                        editor.putString(k, "");
                    }
                    editor.apply();
                    renderizarPerfil();
                    Toast.makeText(this, toastOk, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Deseás cerrar la sesión actual y volver a la pantalla de ingreso?")
                .setPositiveButton("Cerrar Sesión", (dialog, which) -> volverAlLogin())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuenta")
                .setMessage("¿Estás seguro de eliminar tu cuenta institucional de este dispositivo? Se borrarán todos tus datos de perfil y se cerrará tu sesión.")
                .setPositiveButton("Sí, Eliminar Cuenta", (dialog, which) -> {
                    limpiarPerfilLocal(this);
                    try {
                        FirebaseAuth.getInstance().signOut();
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show();
                    volverAlLogin();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void volverAlLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String extraerIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "PD";
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p.isEmpty() || p.endsWith(".")) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.length() > 0 ? sb.toString() : "PD";
    }
}

