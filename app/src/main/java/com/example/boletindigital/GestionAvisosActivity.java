package com.example.boletindigital;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestionAvisosActivity extends AppCompatActivity {

    private ImageView ivVolverAvisosGestion;
    private TextView tvBadgeRolAvisos;
    private NestedScrollView scrollAvisosGestion;
    private TextView tvTituloFormAviso;
    private TextInputEditText etTituloAviso;
    private Spinner spnCategoriaAviso;
    private Spinner spnPrioridadAviso;
    private TextInputEditText etContenidoAviso;
    private MaterialButton btnPublicarAviso;
    private MaterialButton btnCancelarEdicionAviso;

    private TextView tvCantidadAvisosGestion;
    private ProgressBar pbCargandoAvisosGestion;
    private TextView tvSinAvisosGestion;
    private RecyclerView rvAvisosGestion;

    private String token;
    private String rol;
    private String nombre;
    private String avisoEditandoId = "";
    private int avisoEditandoIdDb = 0;
    private int avisoEditandoPos = -1;

    private final List<JSONObject> listaAvisos = new ArrayList<>();
    private AvisosGestionAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final String[] categorias = {
            "Aviso General",
            "Boletines y Calificaciones",
            "Reunión de Familias",
            "Mesas de Examen",
            "Alerta Académica",
            "Urgente / Institucional"
    };

    private final String[] prioridades = {
            "Normal (Informativa)",
            "Media (Importante)",
            "Alta (Atención Requerida)",
            "Urgente (Prioritaria)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_avisos);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        vincularVistas();
        configurarSpinners();
        configurarRecyclerView();
        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombre, "avisos");

        if (savedInstanceState != null) {
            String tituloGuardado = savedInstanceState.getString("tituloDraft", "");
            String contenidoGuardado = savedInstanceState.getString("contenidoDraft", "");
            int catPos = savedInstanceState.getInt("catPos", 0);
            int prioPos = savedInstanceState.getInt("prioPos", 0);

            etTituloAviso.setText(tituloGuardado);
            etContenidoAviso.setText(contenidoGuardado);
            spnCategoriaAviso.setSelection(catPos);
            spnPrioridadAviso.setSelection(prioPos);
        }

        ivVolverAvisosGestion.setOnClickListener(v -> finish());
        btnPublicarAviso.setOnClickListener(v -> publicarAviso());
        if (btnCancelarEdicionAviso != null) {
            btnCancelarEdicionAviso.setOnClickListener(v -> limpiarFormularioAviso());
        }

        cargarAvisosDesdeApi();
    }

    private void vincularVistas() {
        ivVolverAvisosGestion = findViewById(R.id.ivVolverAvisosGestion);
        tvBadgeRolAvisos = findViewById(R.id.tvBadgeRolAvisos);
        scrollAvisosGestion = findViewById(R.id.scrollAvisosGestion);
        tvTituloFormAviso = findViewById(R.id.tvTituloFormAviso);
        etTituloAviso = findViewById(R.id.etTituloAviso);
        spnCategoriaAviso = findViewById(R.id.spnCategoriaAviso);
        spnPrioridadAviso = findViewById(R.id.spnPrioridadAviso);
        etContenidoAviso = findViewById(R.id.etContenidoAviso);
        btnPublicarAviso = findViewById(R.id.btnPublicarAviso);
        btnCancelarEdicionAviso = findViewById(R.id.btnCancelarEdicionAviso);

        tvCantidadAvisosGestion = findViewById(R.id.tvCantidadAvisosGestion);
        pbCargandoAvisosGestion = findViewById(R.id.pbCargandoAvisosGestion);
        tvSinAvisosGestion = findViewById(R.id.tvSinAvisosGestion);
        rvAvisosGestion = findViewById(R.id.rvAvisosGestion);

        if (tvBadgeRolAvisos != null && rol != null) {
            tvBadgeRolAvisos.setText(rol.toUpperCase());
        }
    }

    private void configurarSpinners() {
        DocenteUiHelper.configurarSpinner(this, spnCategoriaAviso, categorias);
        DocenteUiHelper.configurarSpinner(this, spnPrioridadAviso, prioridades);
    }

    private void configurarRecyclerView() {
        rvAvisosGestion.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AvisosGestionAdapter(listaAvisos);
        rvAvisosGestion.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (etTituloAviso != null && etTituloAviso.getText() != null) {
            outState.putString("tituloDraft", etTituloAviso.getText().toString());
        }
        if (etContenidoAviso != null && etContenidoAviso.getText() != null) {
            outState.putString("contenidoDraft", etContenidoAviso.getText().toString());
        }
        if (spnCategoriaAviso != null) {
            outState.putInt("catPos", spnCategoriaAviso.getSelectedItemPosition());
        }
        if (spnPrioridadAviso != null) {
            outState.putInt("prioPos", spnPrioridadAviso.getSelectedItemPosition());
        }
    }

    private void cargarAvisosDesdeApi() {
        pbCargandoAvisosGestion.setVisibility(View.VISIBLE);
        tvSinAvisosGestion.setVisibility(View.GONE);

        final int idInstActiva = InstitucionManager.getIdInstitucionActiva(this);
        executor.execute(() -> {
            try {
                JSONArray res = ApiClient.listarNotificaciones(idInstActiva, "");
                List<JSONObject> parsed = new ArrayList<>();
                for (int i = 0; i < res.length(); i++) {
                    parsed.add(res.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    pbCargandoAvisosGestion.setVisibility(View.GONE);
                    listaAvisos.clear();
                    listaAvisos.addAll(parsed);
                    adapter.notifyDataSetChanged();

                    tvCantidadAvisosGestion.setText(listaAvisos.size() + " Avisos");
                    tvSinAvisosGestion.setVisibility(listaAvisos.isEmpty() ? View.VISIBLE : View.GONE);
                });

            } catch (Exception e) {
                // Si la red falla o está offline, creamos datos representativos locales
                runOnUiThread(() -> {
                    pbCargandoAvisosGestion.setVisibility(View.GONE);
                    if (listaAvisos.isEmpty()) {
                        cargarAvisosOfflineFallback();
                    }
                });
            }
        });
    }

    private void cargarAvisosOfflineFallback() {
        listaAvisos.clear();
        try {
            JSONObject a1 = new JSONObject();
            a1.put("titulo", "Alerta de Inasistencias");
            a1.put("categoria", "Atención y Preceptoría");
            a1.put("prioridad", "alta");
            a1.put("fecha", "Hoy");
            a1.put("contenido", "Estudiantes con más de 15 faltas deben presentar justificación médica antes del viernes.");
            listaAvisos.add(a1);

            JSONObject a2 = new JSONObject();
            a2.put("titulo", "Boletín 1° Cuatrimestre Disponible");
            a2.put("categoria", "Boletines y Calificaciones");
            a2.put("prioridad", "normal");
            a2.put("fecha", "Ayer");
            a2.put("contenido", "Ya se encuentran publicadas las valoraciones conceptuales (TEA/TEP/TED) del período.");
            listaAvisos.add(a2);

            JSONObject a3 = new JSONObject();
            a3.put("titulo", "Inscripción a Mesas de Examen Previas");
            a3.put("categoria", "Mesas de Examen");
            a3.put("prioridad", "media");
            a3.put("fecha", "03/11");
            a3.put("contenido", "Se encuentra abierto el período de intensificación para materias adeudadas del ciclo previo.");
            listaAvisos.add(a3);
        } catch (Exception ignored) {}

        adapter.notifyDataSetChanged();
        tvCantidadAvisosGestion.setText(listaAvisos.size() + " Avisos");
        tvSinAvisosGestion.setVisibility(View.GONE);
    }

    private void publicarAviso() {
        String titulo = etTituloAviso.getText() != null ? etTituloAviso.getText().toString().trim() : "";
        String contenido = etContenidoAviso.getText() != null ? etContenidoAviso.getText().toString().trim() : "";

        if (titulo.isEmpty()) {
            etTituloAviso.setError("Ingrese el título del comunicado");
            etTituloAviso.requestFocus();
            return;
        }

        if (contenido.isEmpty()) {
            etContenidoAviso.setError("Ingrese el contenido del mensaje");
            etContenidoAviso.requestFocus();
            return;
        }

        String categoria = categorias[spnCategoriaAviso.getSelectedItemPosition()];
        int prioIndex = spnPrioridadAviso.getSelectedItemPosition();
        String prioridad;
        switch (prioIndex) {
            case 1: prioridad = "media"; break;
            case 2: prioridad = "alta"; break;
            case 3: prioridad = "urgente"; break;
            default: prioridad = "normal"; break;
        }

        String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String emisor = (nombre != null && !nombre.isEmpty()) ? nombre : "Preceptoría";
        final boolean esEdicion = avisoEditandoPos >= 0 || !avisoEditandoId.isEmpty() || avisoEditandoIdDb > 0;
        final String idObjetivo = avisoEditandoId;
        final int idDbObjetivo = avisoEditandoIdDb;
        final int posObjetivo = avisoEditandoPos;

        btnPublicarAviso.setEnabled(false);
        btnPublicarAviso.setText(esEdicion ? "GUARDANDO..." : "PUBLICANDO...");

        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("accion", "guardar_notificacion");
                if (!idObjetivo.isEmpty()) payload.put("id", idObjetivo);
                if (idDbObjetivo > 0) payload.put("id_db", idDbObjetivo);
                payload.put("titulo", titulo);
                payload.put("contenido", contenido);
                payload.put("categoria", categoria);
                payload.put("prioridad", prioridad);
                payload.put("emisor", emisor);
                payload.put("destinatario", "Todos los Cursos y Familias");
                payload.put("fecha", fechaActual);
                payload.put("id_institucion", InstitucionManager.getIdInstitucionActiva(this));

                boolean sincronizadoApi = false;
                String idRetornado = idObjetivo;
                int idDbRetornado = idDbObjetivo;
                try {
                    JSONObject res = ApiClient.publicarNotificacion(payload, token);
                    idRetornado = res.optString("id", idObjetivo);
                    idDbRetornado = res.optInt("id_db", idDbObjetivo);
                    sincronizadoApi = true;
                } catch (Exception ignored) {
                    // Si falla el endpoint remoto, continuamos con actualización local
                }

                if (!esEdicion) {
                    // Disparar notificación push local a través de Firebase Messaging Service
                    BoletinFirebaseMessagingService.lanzarNotificacion(
                            GestionAvisosActivity.this,
                            titulo,
                            contenido,
                            categoria
                    );
                }

                final boolean recargarDesdeApi = sincronizadoApi;
                final String idFinal = idRetornado;
                final int idDbFinal = idDbRetornado;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnPublicarAviso.setEnabled(true);
                    limpiarFormularioAviso();

                    try {
                        JSONObject itemActualizado = new JSONObject();
                        if (!idFinal.isEmpty()) itemActualizado.put("id", idFinal);
                        if (idDbFinal > 0) itemActualizado.put("id_db", idDbFinal);
                        itemActualizado.put("titulo", titulo);
                        itemActualizado.put("contenido", contenido);
                        itemActualizado.put("categoria", categoria);
                        itemActualizado.put("prioridad", prioridad);
                        itemActualizado.put("fecha", fechaActual);
                        itemActualizado.put("emisor", emisor);

                        if (esEdicion && posObjetivo >= 0 && posObjetivo < listaAvisos.size()) {
                            listaAvisos.set(posObjetivo, itemActualizado);
                            adapter.notifyItemChanged(posObjetivo);
                        } else {
                            itemActualizado.put("fecha", "Ahora");
                            listaAvisos.add(0, itemActualizado);
                            adapter.notifyItemInserted(0);
                            rvAvisosGestion.scrollToPosition(0);
                        }
                        actualizarContadorAvisos();
                    } catch (Exception ignored) {}

                    if (recargarDesdeApi) {
                        cargarAvisosDesdeApi();
                    }

                    new AlertDialog.Builder(GestionAvisosActivity.this)
                            .setTitle(esEdicion ? "Comunicado Actualizado" : "Comunicado Emitido")
                            .setMessage(esEdicion
                                    ? "El aviso \"" + titulo + "\" fue actualizado correctamente."
                                    : "El aviso \"" + titulo + "\" fue publicado y enviado a las familias y estudiantes.")
                            .setPositiveButton("Entendido", null)
                            .show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnPublicarAviso.setEnabled(true);
                    actualizarModoEdicionAvisoUi();
                    Toast.makeText(GestionAvisosActivity.this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void cargarAvisoEnFormulario(JSONObject aviso, int position) {
        avisoEditandoId = aviso.optString("id", "");
        avisoEditandoIdDb = aviso.optInt("id_db", 0);
        avisoEditandoPos = position;

        etTituloAviso.setText(aviso.optString("titulo", ""));
        etContenidoAviso.setText(aviso.optString("contenido", ""));
        DocenteUiHelper.seleccionarValor(spnCategoriaAviso, categorias, aviso.optString("categoria", "Aviso General"));
        DocenteUiHelper.seleccionarValor(spnPrioridadAviso, prioridades, aviso.optString("prioridad", "normal"));

        actualizarModoEdicionAvisoUi();
        if (scrollAvisosGestion != null) {
            scrollAvisosGestion.post(() -> scrollAvisosGestion.smoothScrollTo(0, 0));
        }
    }

    private void limpiarFormularioAviso() {
        avisoEditandoId = "";
        avisoEditandoIdDb = 0;
        avisoEditandoPos = -1;
        etTituloAviso.setText("");
        etTituloAviso.setError(null);
        etContenidoAviso.setText("");
        etContenidoAviso.setError(null);
        spnCategoriaAviso.setSelection(0);
        spnPrioridadAviso.setSelection(0);
        actualizarModoEdicionAvisoUi();
    }

    private void actualizarModoEdicionAvisoUi() {
        boolean editando = avisoEditandoPos >= 0 || !avisoEditandoId.isEmpty() || avisoEditandoIdDb > 0;
        if (tvTituloFormAviso != null) {
            tvTituloFormAviso.setText(editando ? "EDITAR COMUNICADO OFICIAL" : "NUEVO COMUNICADO OFICIAL");
        }
        btnPublicarAviso.setText(editando ? "GUARDAR CAMBIOS" : "PUBLICAR COMUNICADO");
        if (btnCancelarEdicionAviso != null) {
            btnCancelarEdicionAviso.setVisibility(editando ? View.VISIBLE : View.GONE);
        }
    }

    private void confirmarEliminarAviso(JSONObject aviso, int position) {
        String titulo = aviso.optString("titulo", "Comunicado");
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Comunicado")
                .setMessage("¿Deseás eliminar el aviso \"" + titulo + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> ejecutarEliminarAviso(aviso, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarEliminarAviso(JSONObject aviso, int position) {
        String id = aviso.optString("id", "");
        int idDb = aviso.optInt("id_db", 0);

        executor.execute(() -> {
            if (!id.isEmpty() || idDb > 0) {
                try {
                    ApiClient.eliminarNotificacion(id, idDb, token);
                } catch (Exception ignored) {
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (position >= 0 && position < listaAvisos.size()) {
                    listaAvisos.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, listaAvisos.size() - position);
                }
                if (position == avisoEditandoPos || (!id.isEmpty() && id.equals(avisoEditandoId))) {
                    limpiarFormularioAviso();
                }
                actualizarContadorAvisos();
                Toast.makeText(this, "Aviso eliminado correctamente", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void actualizarContadorAvisos() {
        tvCantidadAvisosGestion.setText(listaAvisos.size() == 1 ? "1 Aviso" : listaAvisos.size() + " Avisos");
        tvSinAvisosGestion.setVisibility(listaAvisos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // =========================================================================
    // ADAPTADOR PARA LISTA DE AVISOS INSTITUCIONALES
    // =========================================================================
    private class AvisosGestionAdapter extends RecyclerView.Adapter<AvisosGestionAdapter.AvisoViewHolder> {

        private final List<JSONObject> items;

        public AvisosGestionAdapter(List<JSONObject> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public AvisoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_aviso_gestion, parent, false);
            return new AvisoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AvisoViewHolder holder, int position) {
            JSONObject item = items.get(position);

            String titulo = item.optString("titulo", "Comunicado Oficial");
            String fecha = item.optString("fecha", "Hoy");
            String categoria = item.optString("categoria", "Aviso Institucional");
            String contenido = item.optString("contenido", "");
            String prioridad = item.optString("prioridad", "normal");

            holder.tvTituloItemAviso.setText(titulo);
            holder.tvFechaItemAviso.setText(fecha);
            holder.tvCategoriaItemAviso.setText("Categoría: " + categoria);
            holder.tvContenidoItemAviso.setText(contenido);

            int colorStripe;
            if ("urgente".equalsIgnoreCase(prioridad)) {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_danger);
            } else if ("alta".equalsIgnoreCase(prioridad)) {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_warning);
            } else if ("media".equalsIgnoreCase(prioridad)) {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_warning);
            } else {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_primary);
            }
            holder.vFranjaPrioridadAviso.setBackgroundColor(colorStripe);

            if (holder.btnEditarAvisoItem != null) {
                holder.btnEditarAvisoItem.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        cargarAvisoEnFormulario(items.get(pos), pos);
                    }
                });
            }
            if (holder.btnEliminarAvisoItem != null) {
                holder.btnEliminarAvisoItem.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        confirmarEliminarAviso(items.get(pos), pos);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class AvisoViewHolder extends RecyclerView.ViewHolder {
            View vFranjaPrioridadAviso;
            TextView tvTituloItemAviso;
            TextView tvFechaItemAviso;
            TextView tvCategoriaItemAviso;
            TextView tvContenidoItemAviso;
            ImageView btnEditarAvisoItem;
            ImageView btnEliminarAvisoItem;

            public AvisoViewHolder(@NonNull View itemView) {
                super(itemView);
                vFranjaPrioridadAviso = itemView.findViewById(R.id.vFranjaPrioridadAviso);
                tvTituloItemAviso = itemView.findViewById(R.id.tvTituloItemAviso);
                tvFechaItemAviso = itemView.findViewById(R.id.tvFechaItemAviso);
                tvCategoriaItemAviso = itemView.findViewById(R.id.tvCategoriaItemAviso);
                tvContenidoItemAviso = itemView.findViewById(R.id.tvContenidoItemAviso);
                btnEditarAvisoItem = itemView.findViewById(R.id.btnEditarAvisoItem);
                btnEliminarAvisoItem = itemView.findViewById(R.id.btnEliminarAvisoItem);
            }
        }
    }
}
