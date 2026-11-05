
package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlumnosActivity extends AppCompatActivity {

    private ListView listaAlumnos;
    private TextView tvEstado;
    private Button btnNuevoAlumno;

    private final ArrayList<Alumno> alumnos = new ArrayList<>();
    private final ArrayList<String> nombres = new ArrayList<>();

    private ArrayAdapter<String> adaptador;

    private String token;
    private String rol;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alumnos);

        listaAlumnos = findViewById(R.id.listaAlumnos);
        tvEstado = findViewById(R.id.tvEstado);
        btnNuevoAlumno = findViewById(R.id.btnNuevoAlumno);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");

        adaptador = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                nombres
        );

        listaAlumnos.setAdapter(adaptador);

        // Abrir detalle enviando el ID
        listaAlumnos.setOnItemClickListener(
                (parent, view, position, id) -> {

                    Alumno alumno = alumnos.get(position);

                    Intent intent = new Intent(
                            this, DetalleAlumnoActivity.class);

                    intent.putExtra("id_alumno", alumno.getId());
                    startActivity(intent);
                });

        // Alta solo para roles autorizados
        boolean puedeCrear = "preceptor".equals(rol)
                || "administrador".equals(rol)
                || "admin".equals(rol);

        btnNuevoAlumno.setEnabled(puedeCrear);

        btnNuevoAlumno.setOnClickListener(v -> {
            Intent intent = new Intent(
                    this, NuevoAlumnoActivity.class);

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

        tvEstado.setText(R.string.cargando);

        executor.execute(() -> {

            try {
                JSONArray respuesta = ApiClient.listarAlumnos();

                ArrayList<Alumno> nuevos = new ArrayList<>();
                ArrayList<String> textos = new ArrayList<>();

                for (int i = 0; i < respuesta.length(); i++) {

                    Alumno alumno = new Alumno(
                            respuesta.getJSONObject(i));

                    nuevos.add(alumno);
                    textos.add(alumno.getNombreCompleto()
                            + " - DNI: " + alumno.getDni());
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    alumnos.clear();
                    alumnos.addAll(nuevos);

                    nombres.clear();
                    nombres.addAll(textos);

                    adaptador.notifyDataSetChanged();

                    tvEstado.setText(
                            nombres.isEmpty()
                                    ? R.string.sin_alumnos
                                    : R.string.lista_cargada);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    String mensaje = e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : e instanceof IOException
                            ? getString(R.string.error_conexion)
                            : getString(R.string.error_respuesta);

                    tvEstado.setText(mensaje);

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
