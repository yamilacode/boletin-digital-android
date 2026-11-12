
package com.example.boletindigital;

import org.json.JSONObject;

public class Alumno {

    private int id;
    private String dni;
    private String nombre;
    private String apellido;
    private String jurisdiccion;
    private int idInstitucion;
    private String curso;
    private String seccion;
    private String nombreTutor;
    private String claveAcceso;
    private String emailEstudiante;
    private String emailTutor;
    private String dniTutor;

    public Alumno(JSONObject json) {
        id = json.optInt("id");
        dni = json.optString("dni");
        nombre = json.optString("nombre");
        apellido = json.optString("apellido");
        jurisdiccion = json.optString("jurisdiccion");
        idInstitucion = json.optInt("id_institucion");
        curso = json.optString("curso");
        seccion = json.optString("seccion");
        nombreTutor = json.optString("nombre_tutor");
        claveAcceso = json.optString("clave_acceso", "");
        emailEstudiante = json.optString("email_estudiante", "");
        emailTutor = json.optString("email_tutor", "");
        dniTutor = json.optString("dni_tutor", "");
    }

    public int getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getJurisdiccion() {
        return jurisdiccion;
    }

    public int getIdInstitucion() {
        return idInstitucion;
    }

    public String getCurso() {
        return curso;
    }

    public String getSeccion() {
        return seccion;
    }

    public String getNombreTutor() {
        return nombreTutor;
    }

    public String getClaveAcceso() {
        return claveAcceso != null ? claveAcceso : "";
    }

    public String getEmailEstudiante() {
        return emailEstudiante != null ? emailEstudiante : "";
    }

    public String getEmailTutor() {
        return emailTutor != null ? emailTutor : "";
    }

    public String getDniTutor() {
        return dniTutor != null ? dniTutor : "";
    }

    public String getClaveProvisoriaEfectiva() {
        if (claveAcceso != null && !claveAcceso.trim().isEmpty()) {
            return claveAcceso.trim();
        }
        String d = dni != null ? dni.trim() : "";
        return d.length() >= 4 ? d.substring(d.length() - 4) : d;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}

