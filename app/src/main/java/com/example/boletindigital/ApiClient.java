
package com.example.boletindigital;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // URL del backend PHP en Railway
    private static final String BASE_URL =
            "https://boletin-digital-escolar-production.up.railway.app/api/";

    // Clase para manejar los errores de la API
    public static class ApiException extends Exception {

        private final int codigo;

        public ApiException(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }

        public int getCodigo() {
            return codigo;
        }
    }

    // Se lanza cuando el dispositivo no tiene conexión a Internet
    // (modo avión o sin señal), detectado antes de intentar la petición.
    public static class SinConexionException extends IOException {

        public SinConexionException() {
            super("No hay conexión a Internet. Revisá si el modo avión "
                    + "está activado o si tenés Wi-Fi / datos móviles.");
        }
    }

    // Metodo general para realizar peticiones HTTP
    private static String solicitar(
            String metodo,
            String ruta,
            Object datos,
            String token
    ) throws Exception {

        if (!NetworkUtils.hayConexion()) {
            throw new SinConexionException();
        }

        HttpURLConnection conexion = null;

        try {
            URL url = new URL(BASE_URL + ruta);

            conexion = (HttpURLConnection) url.openConnection();

            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(10000);
            conexion.setReadTimeout(10000);

            conexion.setRequestProperty(
                    "Accept", "application/json");

            // Agregar token cuando sea necesario
            if (token != null && !token.isEmpty()) {
                conexion.setRequestProperty(
                        "Authorization", "Bearer " + token);
            }

            // Enviar datos en formato JSON
            if (datos != null) {

                conexion.setDoOutput(true);

                conexion.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8");

                try (OutputStream salida =
                             conexion.getOutputStream()) {

                    byte[] contenido =
                            datos.toString().getBytes(
                                    StandardCharsets.UTF_8);

                    salida.write(contenido);
                }
            }

            // Obtener el codigo de respuesta
            int codigo = conexion.getResponseCode();

            InputStream entrada =
                    codigo >= 200 && codigo < 300
                            ? conexion.getInputStream()
                            : conexion.getErrorStream();

            StringBuilder respuesta = new StringBuilder();

            // Leer la respuesta del servidor
            if (entrada != null) {

                try (BufferedReader lector =
                             new BufferedReader(
                                     new InputStreamReader(
                                             entrada,
                                             StandardCharsets.UTF_8))) {

                    String linea;

                    while ((linea = lector.readLine()) != null) {
                        respuesta.append(linea);
                    }
                }
            }

            // Comprobar errores de la API
            if (codigo < 200 || codigo >= 300) {

                String mensaje = "Error del servidor";

                try {
                    JSONObject error =
                            new JSONObject(respuesta.toString());

                    mensaje = error.optString(
                            "error", mensaje);

                } catch (Exception ignorado) {

                    if (codigo == 404) {
                        mensaje = "Recurso no encontrado";
                    }
                }

                throw new ApiException(codigo, mensaje);
            }

            return respuesta.toString();

        } catch (SocketTimeoutException e) {

            throw new IOException(
                    "No hubo respuesta del servidor a tiempo.", e);

        } finally {

            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    // POST: iniciar sesion
    public static JSONObject iniciarSesion(
            String email,
            String password
    ) throws Exception {
        return iniciarSesion(email, password, "credenciales", null);
    }

    public static JSONObject iniciarSesion(
            String email,
            String password,
            String metodo,
            String nombre
    ) throws Exception {

        JSONObject datos = new JSONObject();

        datos.put("email", email);
        datos.put("password", password != null ? password : "");
        if (metodo != null && !metodo.trim().isEmpty()) {
            datos.put("metodo", metodo.trim());
        }
        if (nombre != null && !nombre.trim().isEmpty()) {
            datos.put("nombre", nombre.trim());
        }

        String respuesta = solicitar(
                "POST",
                "auth.php",
                datos,
                null
        );

        return new JSONObject(respuesta);
    }

    public static JSONObject iniciarSesionGoogle(
            String email,
            String nombre
    ) throws Exception {
        return iniciarSesion(email, "", "google", nombre);
    }

    // GET: listar todos los alumnos
    public static JSONArray listarAlumnos() throws Exception {
        return listarAlumnos(0);
    }

    // GET: listar alumnos filtrados por institución (o todos si idInstitucion <= 0)
    public static JSONArray listarAlumnos(int idInstitucion) throws Exception {
        String ruta = idInstitucion > 0
                ? "alumnos.php?id_institucion=" + idInstitucion
                : "alumnos.php";

        String respuesta = solicitar(
                "GET",
                ruta,
                null,
                null
        );

        return new JSONArray(respuesta);
    }

    // GET: consultar un alumno por ID
    public static JSONObject obtenerAlumno(
            int id
    ) throws Exception {

        String respuesta = solicitar(
                "GET",
                "alumnos.php?id=" + id,
                null,
                null
        );

        return new JSONObject(respuesta);
    }

    // POST: registrar un nuevo alumno
    public static JSONObject crearAlumno(
            JSONObject alumno,
            String token
    ) throws Exception {

        String respuesta = solicitar(
                "POST",
                "alumnos.php",
                alumno,
                token
        );

        return new JSONObject(respuesta);
    }

    // PUT: actualizar datos de un alumno existente
    public static JSONObject actualizarAlumno(
            int id,
            JSONObject alumno,
            String token
    ) throws Exception {

        String respuesta = solicitar(
                "PUT",
                "alumnos.php?id=" + id,
                alumno,
                token
        );

        return new JSONObject(respuesta);
    }

    // DELETE: eliminar un alumno por ID
    public static JSONObject eliminarAlumno(
            int id,
            String token
    ) throws Exception {

        String respuesta = solicitar(
                "DELETE",
                "alumnos.php?id=" + id,
                null,
                token
        );

        return new JSONObject(respuesta);
    }

    // GET: consultar el boletín académico oficial por DNI
    public static JSONObject obtenerBoletin(
            String dni
    ) throws Exception {
        return obtenerBoletin(dni, 0);
    }

    // GET: consultar el boletín académico oficial por DNI e institución
    public static JSONObject obtenerBoletin(
            String dni,
            int idInstitucion
    ) throws Exception {
        String ruta = "boletin.php?dni=" + dni;
        if (idInstitucion > 0) {
            ruta += "&id_institucion=" + idInstitucion;
        }

        String respuesta = solicitar(
                "GET",
                ruta,
                null,
                null
        );

        return new JSONObject(respuesta);
    }

    // GET: listar calificaciones por institución y opcionalmente materia / período
    public static JSONArray listarNotas(
            int idInstitucion,
            String materia,
            String periodo
    ) throws Exception {
        StringBuilder ruta = new StringBuilder("notas.php");
        String sep = "?";
        if (idInstitucion > 0) {
            ruta.append(sep).append("id_institucion=").append(idInstitucion);
            sep = "&";
        }
        if (materia != null && !materia.trim().isEmpty()) {
            ruta.append(sep).append("materia=").append(codificarParam(materia.trim()));
            sep = "&";
        }
        if (periodo != null && !periodo.trim().isEmpty()) {
            ruta.append(sep).append("periodo=").append(codificarParam(periodo.trim()));
        }

        String respuesta = solicitar(
                "GET",
                ruta.toString(),
                null,
                null
        );

        return new JSONArray(respuesta);
    }

    // POST: guardar calificaciones en lote (Docente / Preceptor)
    public static JSONObject guardarNotas(
            JSONArray listaNotas,
            String token
    ) throws Exception {

        String respuesta = solicitar(
                "POST",
                "notas.php",
                listaNotas,
                token
        );

        return new JSONObject(respuesta);
    }

    // DELETE: eliminar calificación por alumno, materia y período (Docente / Preceptor)
    public static JSONObject eliminarNota(
            String dniEstudiante,
            String materia,
            String periodo,
            int idInstitucion,
            String token
    ) throws Exception {
        StringBuilder ruta = new StringBuilder("notas.php");
        String sep = "?";
        if (dniEstudiante != null && !dniEstudiante.trim().isEmpty()) {
            ruta.append(sep).append("dni_estudiante=").append(codificarParam(dniEstudiante.trim()));
            sep = "&";
        }
        if (materia != null && !materia.trim().isEmpty()) {
            ruta.append(sep).append("materia=").append(codificarParam(materia.trim()));
            sep = "&";
        }
        if (periodo != null && !periodo.trim().isEmpty()) {
            ruta.append(sep).append("periodo=").append(codificarParam(periodo.trim()));
            sep = "&";
        }
        if (idInstitucion > 0) {
            ruta.append(sep).append("id_institucion=").append(idInstitucion);
        }

        String respuesta = solicitar(
                "DELETE",
                ruta.toString(),
                null,
                token
        );

        return new JSONObject(respuesta);
    }

    private static String codificarParam(String valor) {
        try {
            return URLEncoder.encode(valor != null ? valor : "", "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return valor != null ? valor : "";
        }
    }
    // GET: consultar datos de instituciones y sedes
    public static JSONArray obtenerInstituciones() throws Exception {
        String respuesta = solicitar(
                "GET",
                "instituciones.php",
                null,
                null
        );
        return new JSONArray(respuesta);
    }
    // POST: guardar asistencias en lote (Preceptor / Docente)
    public static JSONObject guardarAsistencias(
            JSONArray listaAsistencias,
            String token
    ) throws Exception {
        String respuesta = solicitar(
                "POST",
                "asistencia.php",
                listaAsistencias,
                token
        );
        return new JSONObject(respuesta);
    }

    // POST: publicar o editar comunicado institucional (Preceptor)
    public static JSONObject publicarNotificacion(
            JSONObject datosAviso,
            String token
    ) throws Exception {
        String respuesta = solicitar(
                "POST",
                "notificaciones.php",
                datosAviso,
                token
        );
        return new JSONObject(respuesta);
    }

    // POST: eliminar comunicado institucional (Preceptor)
    public static JSONObject eliminarNotificacion(
            String id,
            int idDb,
            String token
    ) throws Exception {
        JSONObject body = new JSONObject();
        body.put("accion", "eliminar_notificacion");
        body.put("id", id != null ? id : "");
        body.put("id_db", idDb);
        String respuesta = solicitar(
                "POST",
                "notificaciones.php",
                body,
                token
        );
        return new JSONObject(respuesta);
    }

    // GET: listar comunicados y avisos oficiales
    public static JSONArray listarNotificaciones() throws Exception {
        return listarNotificaciones(0, "");
    }

    // GET: listar comunicados oficiales filtrados por institución y opcionalmente DNI
    public static JSONArray listarNotificaciones(int idInstitucion, String dni) throws Exception {
        StringBuilder ruta = new StringBuilder("notificaciones.php");
        String sep = "?";
        if (idInstitucion > 0) {
            ruta.append(sep).append("id_institucion=").append(idInstitucion);
            sep = "&";
        }
        if (dni != null && !dni.trim().isEmpty()) {
            ruta.append(sep).append("dni=").append(dni.trim());
        }
        String respuesta = solicitar(
                "GET",
                ruta.toString(),
                null,
                null
        );
        return new JSONArray(respuesta);
    }

    // GET: consultar registros de asistencia por DNI de estudiante
    public static JSONArray obtenerAsistenciasPorDni(String dni) throws Exception {
        return obtenerAsistenciasPorDni(dni, 0);
    }

    // GET: consultar registros de asistencia por DNI e institución
    public static JSONArray obtenerAsistenciasPorDni(String dni, int idInstitucion) throws Exception {
        String ruta = "asistencia.php?dni=" + dni;
        if (idInstitucion > 0) {
            ruta += "&id_institucion=" + idInstitucion;
        }
        String respuesta = solicitar(
                "GET",
                ruta,
                null,
                null
        );
        return new JSONArray(respuesta);
    }

    // GET: listar mesas de examen oficiales (opcionalmente por id_institucion)
    public static JSONArray listarMesas(int idInstitucion) throws Exception {
        String ruta = idInstitucion > 0
                ? "mesas.php?id_institucion=" + idInstitucion
                : "mesas.php";
        String respuesta = solicitar("GET", ruta, null, null);
        return new JSONArray(respuesta);
    }

    // POST: crear o editar mesa de examen (Preceptor / Docente)
    public static JSONObject guardarMesaExamen(JSONObject datosMesa, String token) throws Exception {
        String respuesta = solicitar("POST", "mesas.php", datosMesa, token);
        return new JSONObject(respuesta);
    }

    // POST: eliminar mesa de examen (Preceptor / Docente)
    public static JSONObject eliminarMesaExamen(String id, int idDb, String token) throws Exception {
        JSONObject body = new JSONObject();
        body.put("accion", "eliminar_mesa");
        body.put("id", id != null ? id : "");
        body.put("id_db", idDb);
        String respuesta = solicitar("POST", "mesas.php", body, token);
        return new JSONObject(respuesta);
    }

    // POST: dar de baja la inscripción de un estudiante en una mesa
    public static JSONObject bajaInscripcionMesa(String mesaId, String dni, String token) throws Exception {
        JSONObject body = new JSONObject();
        body.put("accion", "baja_inscripcion");
        body.put("mesa_id", mesaId);
        body.put("dni", dni);
        String respuesta = solicitar("POST", "mesas.php", body, token);
        return new JSONObject(respuesta);
    }

    // POST: inscribir estudiante a una mesa de examen
    public static JSONObject inscribirAMesa(JSONObject datosInscripcion, String token) throws Exception {
        String respuesta = solicitar("POST", "mesas.php", datosInscripcion, token);
        return new JSONObject(respuesta);
    }

    // GET: listar materias previas (todas o filtradas por DNI)
    public static JSONArray listarPrevias(String dni) throws Exception {
        String ruta = (dni != null && !dni.trim().isEmpty())
                ? "previas.php?dni=" + dni.trim()
                : "previas.php";
        String respuesta = solicitar("GET", ruta, null, null);
        return new JSONArray(respuesta);
    }

    // POST: cargar nueva materia previa de un alumno
    public static JSONObject crearPrevia(JSONObject datosPrevia, String token) throws Exception {
        String respuesta = solicitar("POST", "previas.php", datosPrevia, token);
        return new JSONObject(respuesta);
    }

    // PUT: actualizar materia previa existente (estado, nota, curso, fecha)
    public static JSONObject actualizarPrevia(JSONObject datosPrevia, String token) throws Exception {
        String respuesta = solicitar("PUT", "previas.php", datosPrevia, token);
        return new JSONObject(respuesta);
    }

    // DELETE: eliminar materia previa por ID
    public static JSONObject eliminarPrevia(int id, String token) throws Exception {
        String respuesta = solicitar("DELETE", "previas.php?id=" + id, null, token);
        return new JSONObject(respuesta);
    }
}
