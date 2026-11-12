package com.example.boletindigital;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlumnosActivity extends AppCompatActivity {

    private ListView listaAlumnos;
    private TextView tvEstado;
    private TextView tvUsuarioRol;
    private ProgressBar pbCargandoAlumnos;
    private Button btnNuevoAlumno;
    private View btnVolverAlumnos;
    private EditText etBuscarAlumno;
    private ImageView ivLimpiarBusquedaAlumno;
    private ImageView btnBuscarAlumnoIcono;

    private final ArrayList<Alumno> todosLosAlumnos = new ArrayList<>();
    private final ArrayList<Alumno> alumnosFiltrados = new ArrayList<>();
    private AlumnoAdapter adaptador;

    private String token;
    private String rol;
    private String nombre;
    private boolean puedeGestionar;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumnos);

        listaAlumnos = findViewById(R.id.listaAlumnos);
        tvEstado = findViewById(R.id.tvEstado);
        tvUsuarioRol = findViewById(R.id.tvUsuarioRol);
        pbCargandoAlumnos = findViewById(R.id.pbCargandoAlumnos);
        btnNuevoAlumno = findViewById(R.id.btnNuevoAlumno);
        btnVolverAlumnos = findViewById(R.id.btnVolverAlumnos);
        etBuscarAlumno = findViewById(R.id.etBuscarAlumno);
        ivLimpiarBusquedaAlumno = findViewById(R.id.ivLimpiarBusquedaAlumno);
        btnBuscarAlumnoIcono = findViewById(R.id.btnBuscarAlumnoIcono);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombre);

        // Botón volver al Panel Central
        if (btnVolverAlumnos != null) {
            btnVolverAlumnos.setOnClickListener(v -> finish());
        }

        actualizarEncabezadoRol();

        puedeGestionar = "preceptor".equalsIgnoreCase(rol)
                || "administrador".equalsIgnoreCase(rol)
                || "admin".equalsIgnoreCase(rol);

        adaptador = new AlumnoAdapter(this, alumnosFiltrados, puedeGestionar, new AccionAlumnoListener() {
            @Override
            public void onVerDetalle(Alumno alumno) {
                abrirDetalleAlumno(alumno);
            }

            @Override
            public void onEditar(Alumno alumno) {
                abrirEdicionAlumno(alumno);
            }

            @Override
            public void onEliminar(Alumno alumno) {
                confirmarEliminarAlumno(alumno);
            }
        });
        listaAlumnos.setAdapter(adaptador);

        configurarBuscador();

        btnNuevoAlumno.setEnabled(puedeGestionar);
        if (!puedeGestionar) {
            btnNuevoAlumno.setAlpha(0.4f);
            btnNuevoAlumno.setText("NUEVO ALUMNO (SOLO PRECEPTOR)");
        }

        btnNuevoAlumno.setOnClickListener(v -> {
            Intent intent = new Intent(this, NuevoAlumnoActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("rol", rol);
            intent.putExtra("nombre", nombre);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEncabezadoRol();
        cargarAlumnos();
    }

    private void actualizarEncabezadoRol() {
        if (tvUsuarioRol != null && rol != null) {
            String sedeCorto = InstitucionManager.getNombreCorto(InstitucionManager.getIdInstitucionActiva(this));
            String etiqueta = (nombre != null ? nombre : "USUARIO") + " • " + rol.toUpperCase() + " • " + sedeCorto;
            tvUsuarioRol.setText(etiqueta);
        }
    }

    private void configurarBuscador() {
        if (etBuscarAlumno != null) {
            etBuscarAlumno.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String consulta = s != null ? s.toString() : "";
                    if (ivLimpiarBusquedaAlumno != null) {
                        ivLimpiarBusquedaAlumno.setVisibility(consulta.trim().isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    filtrarAlumnos(consulta);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            etBuscarAlumno.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    filtrarAlumnos(etBuscarAlumno.getText() != null ? etBuscarAlumno.getText().toString() : "");
                    ocultarTeclado(etBuscarAlumno);
                    return true;
                }
                return false;
            });
        }

        if (ivLimpiarBusquedaAlumno != null) {
            ivLimpiarBusquedaAlumno.setOnClickListener(v -> {
                if (etBuscarAlumno != null) {
                    etBuscarAlumno.setText("");
                }
            });
        }

        if (btnBuscarAlumnoIcono != null) {
            btnBuscarAlumnoIcono.setOnClickListener(v -> {
                if (etBuscarAlumno != null) {
                    etBuscarAlumno.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etBuscarAlumno, InputMethodManager.SHOW_IMPLICIT);
                    }
                    filtrarAlumnos(etBuscarAlumno.getText() != null ? etBuscarAlumno.getText().toString() : "");
                }
            });
        }
    }

    private void ocultarTeclado(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void abrirDetalleAlumno(Alumno alumno) {
        Intent intent = new Intent(this, DetalleAlumnoActivity.class);
        intent.putExtra("id_alumno", alumno.getId());
        intent.putExtra("token", token);
        intent.putExtra("rol", rol);
        intent.putExtra("nombre", nombre);
        startActivity(intent);
    }

    private void abrirEdicionAlumno(Alumno alumno) {
        Intent intent = new Intent(this, NuevoAlumnoActivity.class);
        intent.putExtra("token", token);
        intent.putExtra("rol", rol);
        intent.putExtra("nombre", nombre);
        intent.putExtra("id_alumno", alumno.getId());
        intent.putExtra("alumno_dni", alumno.getDni());
        intent.putExtra("alumno_nombre", alumno.getNombre());
        intent.putExtra("alumno_apellido", alumno.getApellido());
        intent.putExtra("alumno_jurisdiccion", alumno.getJurisdiccion());
        intent.putExtra("alumno_institucion", alumno.getIdInstitucion());
        intent.putExtra("alumno_curso", alumno.getCurso());
        intent.putExtra("alumno_seccion", alumno.getSeccion());
        intent.putExtra("alumno_tutor", alumno.getNombreTutor());
        intent.putExtra("alumno_email_tutor", alumno.getEmailTutor());
        intent.putExtra("alumno_clave_acceso", alumno.getClaveAcceso());
        startActivity(intent);
    }

    private void confirmarEliminarAlumno(Alumno alumno) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Alumno")
                .setMessage("¿Deseás eliminar del padrón a " + alumno.getNombreCompleto()
                        + " (DNI " + alumno.getDni() + ")?")
                .setPositiveButton("Eliminar", (dialog, which) -> ejecutarEliminarAlumno(alumno))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarEliminarAlumno(Alumno alumno) {
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, R.string.error_sin_sesion, Toast.LENGTH_LONG).show();
            return;
        }

        if (pbCargandoAlumnos != null) {
            pbCargandoAlumnos.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                ApiClient.eliminarAlumno(alumno.getId(), token);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Alumno eliminado correctamente", Toast.LENGTH_SHORT).show();
                    cargarAlumnos();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbCargandoAlumnos != null) {
                        pbCargandoAlumnos.setVisibility(View.GONE);
                    }
                    String mensaje = e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : e instanceof IOException
                            ? getString(R.string.error_conexion)
                            : getString(R.string.error_respuesta);
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void cargarAlumnos() {
        if (todosLosAlumnos.isEmpty()) {
            tvEstado.setVisibility(View.VISIBLE);
            tvEstado.setText(R.string.cargando);
        }
        if (pbCargandoAlumnos != null) {
            pbCargandoAlumnos.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                JSONArray respuesta = ApiClient.listarAlumnos(InstitucionManager.getIdInstitucionActiva(this));
                ArrayList<Alumno> nuevos = new ArrayList<>();

                for (int i = 0; i < respuesta.length(); i++) {
                    Alumno alumno = new Alumno(respuesta.getJSONObject(i));
                    nuevos.add(alumno);
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (pbCargandoAlumnos != null) {
                        pbCargandoAlumnos.setVisibility(View.GONE);
                    }

                    todosLosAlumnos.clear();
                    todosLosAlumnos.addAll(nuevos);
                    String consulta = etBuscarAlumno != null && etBuscarAlumno.getText() != null
                            ? etBuscarAlumno.getText().toString()
                            : "";
                    filtrarAlumnos(consulta);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (pbCargandoAlumnos != null) {
                        pbCargandoAlumnos.setVisibility(View.GONE);
                    }

                    String mensaje = e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : e instanceof IOException
                            ? getString(R.string.error_conexion)
                            : getString(R.string.error_respuesta);

                    tvEstado.setVisibility(View.VISIBLE);
                    tvEstado.setText(mensaje);

                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void filtrarAlumnos(String consulta) {
        String filtroNorm = normalizarTexto(consulta);
        alumnosFiltrados.clear();

        for (Alumno alumno : todosLosAlumnos) {
            if (filtroNorm.isEmpty()) {
                alumnosFiltrados.add(alumno);
                continue;
            }

            String nombreNorm = normalizarTexto(alumno.getNombre());
            String apellidoNorm = normalizarTexto(alumno.getApellido());
            String nombreCompletoNorm = normalizarTexto(alumno.getNombreCompleto());
            String apellidoNombreNorm = normalizarTexto(alumno.getApellido() + " " + alumno.getNombre());
            String dniNorm = normalizarTexto(alumno.getDni());
            String cursoNorm = normalizarTexto(alumno.getCurso() + " " + alumno.getSeccion());

            if (nombreNorm.contains(filtroNorm)
                    || apellidoNorm.contains(filtroNorm)
                    || nombreCompletoNorm.contains(filtroNorm)
                    || apellidoNombreNorm.contains(filtroNorm)
                    || dniNorm.contains(filtroNorm)
                    || cursoNorm.contains(filtroNorm)) {
                alumnosFiltrados.add(alumno);
            }
        }

        adaptador.notifyDataSetChanged();

        if (alumnosFiltrados.isEmpty()) {
            tvEstado.setVisibility(View.VISIBLE);
            if (!todosLosAlumnos.isEmpty() && !filtroNorm.isEmpty()) {
                tvEstado.setText("No se encontraron alumnos para \"" + consulta.trim() + "\"");
            } else {
                tvEstado.setText(R.string.sin_alumnos);
            }
        } else {
            tvEstado.setVisibility(View.GONE);
        }
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private interface AccionAlumnoListener {
        void onVerDetalle(Alumno alumno);
        void onEditar(Alumno alumno);
        void onEliminar(Alumno alumno);
    }

    private static class AlumnoAdapter extends ArrayAdapter<Alumno> {

        private final boolean mostrarAcciones;
        private final AccionAlumnoListener listener;

        public AlumnoAdapter(
                @NonNull Context context,
                @NonNull List<Alumno> objects,
                boolean mostrarAcciones,
                AccionAlumnoListener listener
        ) {
            super(context, 0, objects);
            this.mostrarAcciones = mostrarAcciones;
            this.listener = listener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_alumno, parent, false);
            }

            Alumno alumno = getItem(position);
            if (alumno != null) {
                TextView tvInicial = convertView.findViewById(R.id.tvInicial);
                TextView tvItemNombre = convertView.findViewById(R.id.tvItemNombre);
                TextView tvItemDni = convertView.findViewById(R.id.tvItemDni);
                TextView tvItemCurso = convertView.findViewById(R.id.tvItemCurso);
                View llAccionesAlumno = convertView.findViewById(R.id.llAccionesAlumno);
                View btnEditarAlumno = convertView.findViewById(R.id.btnEditarAlumnoItem);
                View btnEliminarAlumno = convertView.findViewById(R.id.btnEliminarAlumnoItem);

                String nombreCompleto = alumno.getNombreCompleto();
                tvItemNombre.setText(nombreCompleto);

                String inicial = (nombreCompleto != null && !nombreCompleto.isEmpty())
                        ? String.valueOf(nombreCompleto.charAt(0)).toUpperCase()
                        : "A";
                tvInicial.setText(inicial);

                tvItemDni.setText("DNI: " + alumno.getDni());

                String curso = alumno.getCurso();
                String seccion = alumno.getSeccion();
                if (curso != null && !curso.isEmpty()) {
                    tvItemCurso.setVisibility(View.VISIBLE);
                    tvItemCurso.setText("• " + curso + (seccion != null ? " " + seccion : ""));
                } else {
                    tvItemCurso.setVisibility(View.GONE);
                }

                convertView.setOnClickListener(v -> {
                    if (listener != null) listener.onVerDetalle(alumno);
                });

                if (llAccionesAlumno != null) {
                    llAccionesAlumno.setVisibility(mostrarAcciones ? View.VISIBLE : View.GONE);
                }
                if (btnEditarAlumno != null) {
                    btnEditarAlumno.setOnClickListener(v -> {
                        if (listener != null) listener.onEditar(alumno);
                    });
                }
                if (btnEliminarAlumno != null) {
                    btnEliminarAlumno.setOnClickListener(v -> {
                        if (listener != null) listener.onEliminar(alumno);
                    });
                }
            }

            return convertView;
        }
    }
}
