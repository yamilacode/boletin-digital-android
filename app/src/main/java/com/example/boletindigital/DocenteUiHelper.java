package com.example.boletindigital;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper compartido para la navegación inferior Docente/Preceptor (Panel | Avisos | Perfil),
 * configuración de Spinners y formateo de fechas en módulos de gestión.
 */
public final class DocenteUiHelper {

    private DocenteUiHelper() {
    }

    public static void configurarFooterDocente(
            AppCompatActivity activity,
            String token,
            String rol,
            String nombre
    ) {
        configurarFooterDocente(activity, token, rol, nombre, "panel");
    }

    public static void configurarFooterDocente(
            AppCompatActivity activity,
            String token,
            String rol,
            String nombre,
            String seccionActiva
    ) {
        View tabPanel = activity.findViewById(R.id.tabDocentePanel);
        View tabAvisos = activity.findViewById(R.id.tabDocenteAvisos);
        View tabPerfil = activity.findViewById(R.id.tabDocentePerfil);

        ImageView ivPanel = activity.findViewById(R.id.ivDocentePanel);
        TextView tvPanel = activity.findViewById(R.id.tvDocentePanel);
        ImageView ivAvisos = activity.findViewById(R.id.ivDocenteAvisos);
        TextView tvAvisos = activity.findViewById(R.id.tvDocenteAvisos);
        ImageView ivPerfil = activity.findViewById(R.id.ivDocentePerfil);
        TextView tvPerfil = activity.findViewById(R.id.tvDocentePerfil);

        boolean enAvisos = "avisos".equalsIgnoreCase(seccionActiva);
        boolean enPerfil = "perfil".equalsIgnoreCase(seccionActiva);
        boolean enPanel = !enAvisos && !enPerfil;

        int colorActivo = ContextCompat.getColor(activity, R.color.color_primary);
        int colorInactivo = ContextCompat.getColor(activity, R.color.text_muted);

        if (ivPanel != null) ivPanel.setColorFilter(enPanel ? colorActivo : colorInactivo);
        if (tvPanel != null) {
            tvPanel.setTextColor(enPanel ? colorActivo : colorInactivo);
            tvPanel.setTypeface(null, enPanel ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivAvisos != null) ivAvisos.setColorFilter(enAvisos ? colorActivo : colorInactivo);
        if (tvAvisos != null) {
            tvAvisos.setTextColor(enAvisos ? colorActivo : colorInactivo);
            tvAvisos.setTypeface(null, enAvisos ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivPerfil != null) ivPerfil.setColorFilter(enPerfil ? colorActivo : colorInactivo);
        if (tvPerfil != null) {
            tvPerfil.setTextColor(enPerfil ? colorActivo : colorInactivo);
            tvPerfil.setTypeface(null, enPerfil ? Typeface.BOLD : Typeface.NORMAL);
        }

        String nombreActivo = PerfilDocenteActivity.obtenerNombreActivo(activity, nombre);

        if (tabPanel != null) {
            tabPanel.setOnClickListener(v -> {
                Intent intent = new Intent(activity, MenuPrincipalActivity.class);
                intent.putExtra("token", token);
                intent.putExtra("rol", rol);
                intent.putExtra("nombre", nombreActivo);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        if (tabAvisos != null) {
            tabAvisos.setOnClickListener(v -> {
                if (enAvisos) return;
                Intent intent = new Intent(activity, GestionAvisosActivity.class);
                intent.putExtra("token", token);
                intent.putExtra("rol", rol);
                intent.putExtra("nombre", nombreActivo);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        if (tabPerfil != null) {
            tabPerfil.setOnClickListener(v -> {
                if (enPerfil) return;
                mostrarMenuPerfil(activity, token, rol, nombreActivo);
                activity.finish();
            });
        }
    }

    public static void mostrarMenuPerfil(AppCompatActivity activity, String rol, String nombre) {
        mostrarMenuPerfil(activity, "", rol, nombre);
    }

    public static void mostrarMenuPerfil(AppCompatActivity activity, String token, String rol, String nombre) {
        Intent intent = new Intent(activity, PerfilDocenteActivity.class);
        intent.putExtra("token", token);
        intent.putExtra("rol", rol);
        intent.putExtra("nombre", PerfilDocenteActivity.obtenerNombreActivo(activity, nombre));
        activity.startActivity(intent);
    }

    public static void aplicarBadgeRol(TextView badge, String rol) {
        if (badge != null && rol != null && !rol.trim().isEmpty()) {
            badge.setText(rol.trim().toUpperCase());
        }
    }

    /** Adapter estilizado con la paleta oscura web (#0B111E / #0F172A / #38BDF8) para todos los Spinners. */
    public static class WebSpinnerAdapter<T> extends ArrayAdapter<T> {
        private final Spinner spinnerRef;

        public WebSpinnerAdapter(Context context, Spinner spinnerRef, List<T> items) {
            super(context, R.layout.item_spinner_web, android.R.id.text1, items);
            this.spinnerRef = spinnerRef;
            setDropDownViewResource(R.layout.item_spinner_dropdown_web);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                boolean seleccionado = spinnerRef != null && spinnerRef.getSelectedItemPosition() == position;
                if (view instanceof CheckedTextView) {
                    ((CheckedTextView) view).setChecked(seleccionado);
                }
                if (seleccionado) {
                    tv.setBackgroundResource(R.color.color_badge_blue_bg);
                    tv.setTextColor(ContextCompat.getColor(getContext(), R.color.color_primary));
                    tv.setTypeface(null, Typeface.BOLD);
                } else {
                    tv.setBackgroundResource(R.drawable.bg_spinner_dropdown_item);
                    tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_main));
                    tv.setTypeface(null, Typeface.NORMAL);
                }
            }
            return view;
        }
    }

    public static void estilizarSpinnerWeb(Spinner spinner) {
        if (spinner == null) return;
        float density = spinner.getResources().getDisplayMetrics().density;
        spinner.setBackgroundResource(R.drawable.bg_spinner_web);
        spinner.setBackgroundTintList(null);
        spinner.setPadding(0, 0, 0, 0);
        spinner.setPopupBackgroundResource(R.drawable.bg_spinner_popup_web);
        spinner.setMinimumHeight(Math.round(42 * density));
        spinner.setDropDownVerticalOffset(Math.round(4 * density));
    }

    public static <T> ArrayAdapter<T> crearAdapterSpinner(Context context, Spinner spinner, List<T> items) {
        List<T> lista = items != null ? items : new ArrayList<>();
        return new WebSpinnerAdapter<>(context, spinner, lista);
    }

    public static void configurarSpinner(Context context, Spinner spinner, String[] opciones) {
        if (spinner == null) return;
        List<String> lista = new ArrayList<>();
        if (opciones != null) {
            Collections.addAll(lista, opciones);
        }
        configurarSpinner(context, spinner, lista);
    }

    public static <T> void configurarSpinner(Context context, Spinner spinner, List<T> opciones) {
        if (spinner == null) return;
        estilizarSpinnerWeb(spinner);
        spinner.setAdapter(crearAdapterSpinner(context, spinner, opciones));
    }

    public static void seleccionarValor(Spinner spinner, String[] opciones, String buscado) {
        if (spinner == null || opciones == null || buscado == null) return;
        String clean = buscado.trim();
        for (int i = 0; i < opciones.length; i++) {
            if (opciones[i].equalsIgnoreCase(clean)
                    || opciones[i].toLowerCase().contains(clean.toLowerCase())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    public static String formatearFecha(String fechaIso) {
        if (fechaIso == null || fechaIso.trim().isEmpty()) {
            return "A confirmar";
        }
        String f = fechaIso.trim();
        if (f.contains("-")) {
            String[] partes = f.split("-");
            if (partes.length == 3 && partes[0].length() == 4) {
                return partes[2] + "/" + partes[1] + "/" + partes[0];
            }
        }
        return f;
    }
}

