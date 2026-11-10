package com.example.boletindigital;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlumnosActivity extends AppCompatActivity {

    private ListView listaAlumnos;
    private TextView tvEstado;
    private TextView tvUsuarioRol;
    private ProgressBar pbCargandoAlumnos;
    private Button btnNuevoAlumno;
    private View btnVolverAlumnos;

    private final ArrayList<Alumno> alumnos = new ArrayList<>();
    private AlumnoAdapter adaptador;

    private String token;
    private String rol;
    private String nombre;

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

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        // Botón volver al Panel Central
        if (btnVolverAlumnos != null) {
            btnVolverAlumnos.setOnClickListener(v -> finish());
        }

        if (tvUsuarioRol != null && rol != null) {
            String etiqueta = (nombre != null ? nombre : "USUARIO") + " • ROL: " + rol.toUpperCase();
            tvUsuarioRol.setText(etiqueta);
        }

        adaptador = new AlumnoAdapter(this, alumnos);
        listaAlumnos.setAdapter(adaptador);

        // Abrir detalle enviando el ID del alumno
        listaAlumnos.setOnItemClickListener((parent, view, position, id) -> {
            Alumno alumno = alumnos.get(position);
            Intent intent = new Intent(this, DetalleAlumnoActivity.class);
            intent.putExtra("id_alumno", alumno.getId());
            startActivity(intent);
        });

        // Alta solo para roles autorizados
        boolean puedeCrear = "preceptor".equals(rol)
                || "administrador".equals(rol)
                || "admin".equals(rol);

        btnNuevoAlumno.setEnabled(puedeCrear);
        if (!puedeCrear) {
            btnNuevoAlumno.setAlpha(0.4f);
            btnNuevoAlumno.setText("NUEVO ALUMNO (SOLO PRECEPTOR)");
        }

        btnNuevoAlumno.setOnClickListener(v -> {
            Intent intent = new Intent(this, NuevoAlumnoActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarAlumnos();
    }

    private void cargarAlumnos() {
        tvEstado.setVisibility(View.VISIBLE);
        tvEstado.setText(R.string.cargando);
        if (pbCargandoAlumnos != null) {
            pbCargandoAlumnos.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                JSONArray respuesta = ApiClient.listarAlumnos();
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

                    alumnos.clear();
                    alumnos.addAll(nuevos);
                    adaptador.notifyDataSetChanged();

                    if (alumnos.isEmpty()) {
                        tvEstado.setVisibility(View.VISIBLE);
                        tvEstado.setText(R.string.sin_alumnos);
                    } else {
                        tvEstado.setVisibility(View.GONE);
                    }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private static class AlumnoAdapter extends ArrayAdapter<Alumno> {

        public AlumnoAdapter(@NonNull Context context, @NonNull List<Alumno> objects) {
            super(context, 0, objects);
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
            }

            return convertView;
        }
    }
}
