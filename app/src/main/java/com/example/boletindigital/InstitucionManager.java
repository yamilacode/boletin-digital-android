package com.example.boletindigital;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestor global de la Institución Educativa activa en toda la aplicación
 * (sincroniza Preceptor/Docente y Estudiante/Tutor entre EPES N° 19 Formosa y E.E.S. N° 5 PBA).
 */
public final class InstitucionManager {

    public static final int ID_FORMOSA = 1;
    public static final int ID_PBA = 2;

    private static final String PREFS_NAME = "boletin_institucion_prefs";
    private static final String KEY_ID = "id_institucion";
    private static final String KEY_JURISDICCION = "jurisdiccion";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_SUBTITULO = "subtitulo";
    private static final String KEY_CUE = "cue";
    private static final String KEY_REGIMEN = "regimen";

    private InstitucionManager() {
    }

    public static int getIdInstitucionActiva(Context context) {
        if (context == null) return ID_PBA;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int id = prefs.getInt(KEY_ID, ID_PBA);
        return (id == ID_FORMOSA) ? ID_FORMOSA : ID_PBA;
    }

    public static String getJurisdiccionActiva(Context context) {
        return getIdInstitucionActiva(context) == ID_FORMOSA ? "formosa" : "pba";
    }

    public static boolean isPba(Context context) {
        return getIdInstitucionActiva(context) == ID_PBA;
    }

    public static boolean isFormosa(Context context) {
        return getIdInstitucionActiva(context) == ID_FORMOSA;
    }

    public static void setInstitucionActiva(Context context, int idInstitucion) {
        if (context == null) return;
        int idValido = (idInstitucion == ID_FORMOSA) ? ID_FORMOSA : ID_PBA;
        boolean formosa = (idValido == ID_FORMOSA);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_ID, idValido)
                .putString(KEY_JURISDICCION, formosa ? "formosa" : "pba")
                .putString(KEY_NOMBRE, formosa ? "EPES N° 19 Vuelta de Obligado" : "E.E.S. N° 5")
                .putString(KEY_SUBTITULO, formosa ? "Delegación Zonal Pilcomayo | Laguna Naineck" : "Distrito PUAN | Ciclo Lectivo 2026")
                .putString(KEY_CUE, formosa ? "3400124-00" : "0604123-00")
                .putString(KEY_REGIMEN, formosa ? "Secundaria 3 Trimestres con PAIR" : "Secundaria Cuatrimestral (R.I.T.E.)")
                .apply();
    }

    public static void setInstitucionActiva(Context context, JSONObject instJson) {
        if (context == null) return;
        if (instJson == null) {
            return;
        }
        String juris = instJson.optString("jurisdiccion", "").trim().toLowerCase();
        int id = instJson.optInt("id_db", 0);
        if (id != ID_FORMOSA && id != ID_PBA) {
            id = "formosa".equals(juris) ? ID_FORMOSA : ID_PBA;
        }
        boolean formosa = (id == ID_FORMOSA);

        String nombre = instJson.optString("nombre", formosa ? "EPES N° 19 Vuelta de Obligado" : "E.E.S. N° 5");
        String subtitulo = instJson.optString("subtitulo", formosa ? "Delegación Zonal Pilcomayo | Laguna Naineck" : "Distrito PUAN | Ciclo Lectivo 2026");
        String cue = instJson.optString("cue", formosa ? "3400124-00" : "0604123-00");
        String regimen = instJson.optString("regimen", formosa ? "Secundaria 3 Trimestres con PAIR" : "Secundaria Cuatrimestral (R.I.T.E.)");

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(KEY_ID, id)
                .putString(KEY_JURISDICCION, formosa ? "formosa" : "pba")
                .putString(KEY_NOMBRE, nombre)
                .putString(KEY_SUBTITULO, subtitulo)
                .putString(KEY_CUE, cue)
                .putString(KEY_REGIMEN, regimen)
                .apply();
    }

    public static String getNombreEscuelaActiva(Context context) {
        int id = getIdInstitucionActiva(context);
        String def = (id == ID_FORMOSA) ? "EPES N° 19 Vuelta de Obligado" : "E.E.S. N° 5";
        if (context == null) return def;
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NOMBRE, def);
    }

    public static String getNombreCorto(int idInst) {
        return idInst == ID_FORMOSA ? "EPES N° 19 (Formosa)" : "E.E.S. N° 5 (Bs. As.)";
    }

    public static String getNombreCortoActiva(Context context) {
        return getNombreCorto(getIdInstitucionActiva(context));
    }

    public static String getRegimenCorto(int idInst) {
        return idInst == ID_FORMOSA ? "Trimestral (PAIR)" : "Cuatrimestral (R.I.T.E.)";
    }

    public static String getDniAlumnoPorDefecto(int idInst) {
        return idInst == ID_FORMOSA ? "34567890" : "48763736";
    }

    public static String[] getCursosPorDefecto(int idInst) {
        if (idInst == ID_FORMOSA) {
            return new String[]{
                    "2° III (Ciclo Básico)",
                    "1° I (Ciclo Básico)",
                    "1° II (Ciclo Básico)",
                    "3° I (Ciclo Básico)",
                    "4° I (Ciencias Sociales)",
                    "4° I (Ciencias Naturales)",
                    "5° I (Ciencias Sociales)",
                    "6° I (Ciencias Sociales)"
            };
        }
        return new String[]{
                "4° Única (Secundario Orientado)",
                "1° Única (Ciclo Básico)",
                "2° Única (Ciclo Básico)",
                "3° Única (Ciclo Básico)",
                "5° Única (Secundario Orientado)",
                "6° Única (Secundario Orientado)"
        };
    }

    public static String[] extraerCursosFormateados(JSONObject instJson, int idInst) {
        if (instJson != null && instJson.has("cursos")) {
            JSONArray arr = instJson.optJSONArray("cursos");
            if (arr != null && arr.length() > 0) {
                List<String> lista = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c == null) continue;
                    String nombre = c.optString("nombre", "").trim();
                    String orient = c.optString("orientacion", "").trim();
                    if (!nombre.isEmpty()) {
                        String etiqueta = (!orient.isEmpty() && !orient.equalsIgnoreCase("null"))
                                ? nombre + " (" + orient + ")"
                                : nombre;
                        if (!lista.contains(etiqueta)) {
                            lista.add(etiqueta);
                        }
                    }
                }
                if (!lista.isEmpty()) {
                    return lista.toArray(new String[0]);
                }
            }
        }
        return getCursosPorDefecto(idInst);
    }

    public static class CursoOption {
        public final String id;
        public final String nombre;
        public final String ciclo;
        public final int anio;
        public final String division;
        public final String orientacion;
        public final String etiqueta;

        public CursoOption(String id, String nombre, String ciclo, int anio, String division, String orientacion) {
            this.id = id != null ? id : "";
            this.nombre = nombre != null ? nombre : "";
            this.ciclo = ("orientado".equalsIgnoreCase(ciclo) || anio >= 4) ? "orientado" : "basico";
            this.anio = anio;
            this.division = division != null ? division : "";
            this.orientacion = (orientacion != null && !orientacion.isEmpty() && !"null".equalsIgnoreCase(orientacion))
                    ? orientacion
                    : ("orientado".equals(this.ciclo) ? "Ciencias Sociales" : "Ciclo Básico");
            this.etiqueta = !this.orientacion.isEmpty() ? this.nombre + " (" + this.orientacion + ")" : this.nombre;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    public static List<CursoOption> getCursosPorDefectoDetalle(int idInst) {
        List<CursoOption> lista = new ArrayList<>();
        if (idInst == ID_FORMOSA) {
            lista.add(new CursoOption("1-1", "1° I", "basico", 1, "I", "Ciclo Básico"));
            lista.add(new CursoOption("1-2", "1° II", "basico", 1, "II", "Ciclo Básico"));
            lista.add(new CursoOption("1-3", "1° III", "basico", 1, "III", "Ciclo Básico"));
            lista.add(new CursoOption("2-1", "2° I", "basico", 2, "I", "Ciclo Básico"));
            lista.add(new CursoOption("2-2", "2° II", "basico", 2, "II", "Ciclo Básico"));
            lista.add(new CursoOption("2-3", "2° III", "basico", 2, "III", "Ciclo Básico"));
            lista.add(new CursoOption("3-1", "3° I", "basico", 3, "I", "Ciclo Básico"));
            lista.add(new CursoOption("3-2", "3° II", "basico", 3, "II", "Ciclo Básico"));
            lista.add(new CursoOption("3-3", "3° III", "basico", 3, "III", "Ciclo Básico"));
            lista.add(new CursoOption("4-cn-1", "4° I", "orientado", 4, "I", "Ciencias Naturales"));
            lista.add(new CursoOption("4-cn-2", "4° II", "orientado", 4, "II", "Ciencias Naturales"));
            lista.add(new CursoOption("5-cn-1", "5° I", "orientado", 5, "I", "Ciencias Naturales"));
            lista.add(new CursoOption("5-cn-2", "5° II", "orientado", 5, "II", "Ciencias Naturales"));
            lista.add(new CursoOption("6-cn-1", "6° I", "orientado", 6, "I", "Ciencias Naturales"));
            lista.add(new CursoOption("4-cs-1", "4° I", "orientado", 4, "I", "Ciencias Sociales"));
            lista.add(new CursoOption("4-cs-2", "4° II", "orientado", 4, "II", "Ciencias Sociales"));
            lista.add(new CursoOption("5-cs-1", "5° I", "orientado", 5, "I", "Ciencias Sociales"));
            lista.add(new CursoOption("5-cs-2", "5° II", "orientado", 5, "II", "Ciencias Sociales"));
            lista.add(new CursoOption("6-cs-1", "6° I", "orientado", 6, "I", "Ciencias Sociales"));
            lista.add(new CursoOption("6-cs-2", "6° II", "orientado", 6, "II", "Ciencias Sociales"));
        } else {
            lista.add(new CursoOption("pba-1-u", "1° Única", "basico", 1, "Única", "Ciclo Básico"));
            lista.add(new CursoOption("pba-2-u", "2° Única", "basico", 2, "Única", "Ciclo Básico"));
            lista.add(new CursoOption("pba-3-u", "3° Única", "basico", 3, "Única", "Ciclo Básico"));
            lista.add(new CursoOption("pba-4-u", "4° Única", "orientado", 4, "Única", "Ciencias Sociales"));
            lista.add(new CursoOption("pba-5-u", "5° Única", "orientado", 5, "Única", "Ciencias Sociales"));
            lista.add(new CursoOption("pba-6-u", "6° Única", "orientado", 6, "Única", "Ciencias Sociales"));
        }
        return lista;
    }

    public static List<CursoOption> extraerOpcionesCurso(JSONObject instJson, int idInst) {
        if (instJson != null && instJson.has("cursos")) {
            JSONArray arr = instJson.optJSONArray("cursos");
            if (arr != null && arr.length() > 0) {
                List<CursoOption> lista = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c == null) continue;
                    String nombre = c.optString("nombre", "").trim();
                    if (nombre.isEmpty()) continue;

                    String id = c.optString("id", c.optString("codigo_curso", "")).trim();
                    int anio = c.optInt("anio", 0);
                    if (anio <= 0) {
                        anio = extraerNumeroAnio(nombre);
                    }
                    String division = c.optString("division", "").trim();
                    if (division.isEmpty() || "null".equalsIgnoreCase(division)) {
                        division = nombre.replaceFirst("^\\d+\\s*[°º]?\\s*", "").trim();
                    }
                    String ciclo = c.optString("ciclo", anio >= 4 ? "orientado" : "basico").trim();
                    String orient = c.optString("orientacion", "").trim();

                    CursoOption opt = new CursoOption(id, nombre, ciclo, anio, division, orient);
                    boolean duplicado = false;
                    for (CursoOption existente : lista) {
                        if (existente.etiqueta.equalsIgnoreCase(opt.etiqueta)) {
                            duplicado = true;
                            break;
                        }
                    }
                    if (!duplicado) {
                        lista.add(opt);
                    }
                }
                if (!lista.isEmpty()) {
                    return lista;
                }
            }
        }
        return getCursosPorDefectoDetalle(idInst);
    }

    public static String[] getMateriasPorCiclo(int idInst, String ciclo) {
        boolean orientado = "orientado".equalsIgnoreCase(ciclo);
        if (idInst == ID_FORMOSA) {
            if (orientado) {
                return new String[]{
                        "Lengua y Literatura",
                        "Matemática",
                        "Historia",
                        "Geografía",
                        "Física",
                        "Química",
                        "Filosofía",
                        "Economía",
                        "Lengua Extranjera (Inglés)",
                        "Educación Física",
                        "Tecnologías de la Información y Comunicación"
                };
            }
            return new String[]{
                    "Lengua y Literatura",
                    "Matemática",
                    "Geografía",
                    "Seminarios o Talleres de Historia",
                    "Formación Ética y Compromiso Comunitario",
                    "Educación Física",
                    "Físico - Química",
                    "Biología",
                    "Educación Tecnológica",
                    "Lengua Extranjera (Inglés)",
                    "Educación Artística (Música)",
                    "Tutoría: Fortalecimiento del Estudiante"
            };
        }

        if (orientado) {
            return new String[]{
                    "Literatura",
                    "Matemática",
                    "Historia",
                    "Geografía",
                    "Introducción a la Física",
                    "Biología",
                    "Inglés",
                    "Educación Física",
                    "NTICX",
                    "Psicología",
                    "Introducción a la Comunicación",
                    "Construcción Ciudadana"
            };
        }
        return new String[]{
                "Prácticas del Lenguaje",
                "Matemática",
                "Ciencias Sociales",
                "Ciencias Naturales",
                "Inglés",
                "Educación Física",
                "Educación Artística",
                "Construcción de Ciudadanía"
        };
    }

    public static boolean perteneceAlCurso(JSONObject alumnoJson, CursoOption cursoOpt) {
        if (alumnoJson == null || cursoOpt == null) return false;

        String cursoAlum = normalizarSimple(alumnoJson.optString("curso", ""));
        String secAlum = normalizarSimple(alumnoJson.optString("seccion", ""));

        int anioAlum = extraerNumeroAnio(cursoAlum);
        if (anioAlum <= 0 || (cursoOpt.anio > 0 && anioAlum != cursoOpt.anio)) {
            return false;
        }

        String divAlum = secAlum.replaceAll("\\(.*?\\)", "").replaceAll("\\s+", " ").trim();
        if (divAlum.isEmpty()) {
            divAlum = cursoAlum.replaceFirst("^\\d+\\s*[°º]?\\s*", "").replaceAll("\\(.*?\\)", "").trim();
        }
        String divCurso = normalizarSimple(cursoOpt.division).replaceAll("\\s+", " ").trim();

        if (!divCurso.isEmpty() && !divAlum.isEmpty()) {
            boolean divCoincide = divAlum.equals(divCurso)
                    || ("UNICA".equals(divCurso) && "U".equals(divAlum))
                    || divAlum.matches("^" + Pattern.quote(divCurso) + "(\\s+.*)?$");
            if (!divCoincide) {
                return false;
            }
        }

        String oriCurso = normalizarSimple(cursoOpt.orientacion);
        boolean oriEsCs = oriCurso.contains("SOCIAL");
        boolean oriEsCn = oriCurso.contains("NATURAL");
        boolean oriEsBasico = oriCurso.contains("BASICO");

        boolean secEsCs = secAlum.contains("CS") || secAlum.contains("SOC");
        boolean secEsCn = secAlum.contains("CN") || secAlum.contains("NAT");

        if (secEsCs && (oriEsCn || oriEsBasico)) return false;
        if (secEsCn && (oriEsCs || oriEsBasico)) return false;

        return true;
    }

    public static boolean sonMateriasEquivalentes(String a, String b) {
        String na = normalizarMateriaClave(a);
        String nb = normalizarMateriaClave(b);
        if (na.isEmpty() || nb.isEmpty()) return false;
        if (na.equals(nb)) return true;

        if ((na.equals("construccion ciudadana") && nb.equals("construccion de ciudadania"))
                || (nb.equals("construccion ciudadana") && na.equals("construccion de ciudadania"))) {
            return true;
        }
        if ((na.contains("ingles") && nb.contains("ingles"))
                || (na.equals("fisico-quimica") && nb.equals("fisico quimica"))
                || (nb.equals("fisico-quimica") && na.equals("fisico quimica"))) {
            return true;
        }
        return false;
    }

    private static int extraerNumeroAnio(String texto) {
        if (texto == null) return 0;
        Matcher m = Pattern.compile("(\\d+)").matcher(texto);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static String normalizarMateriaClave(String s) {
        return normalizarSimple(s)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s*-\\s*", "-")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizarSimple(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toUpperCase(Locale.ROOT).trim();
    }

    public static String[] getPeriodosNotas(int idInst) {
        if (idInst == ID_FORMOSA) {
            return new String[]{
                    "1° Trimestre",
                    "2° Trimestre",
                    "3° Trimestre",
                    "Recuperatorio Diciembre",
                    "PAIR"
            };
        }
        return new String[]{
                "1° Cuatrimestre",
                "2° Cuatrimestre",
                "RITE Diciembre",
                "RITE Febrero"
        };
    }

    public static String[] getPeriodosAsistencia(int idInst) {
        if (idInst == ID_FORMOSA) {
            return new String[]{
                    "1° Trimestre",
                    "2° Trimestre",
                    "3° Trimestre",
                    "Anual Completo"
            };
        }
        return new String[]{
                "1° Cuatrimestre",
                "2° Cuatrimestre",
                "Anual Completo"
        };
    }
}

