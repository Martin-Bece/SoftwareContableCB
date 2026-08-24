/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Martin
 */
public class cliente {
    
    private String cuit; // Primary Key
    
    private String razonSocial;
    
    private String nombreFantasia;
    
    private String direccion;
    
    private String actividades;
    
    private String impuestosActivos;
    
    private boolean tieneEmpleados;

    public cliente() {
    }

    public cliente(String cuit, String razonSocial, String nombreFantasia, String direccion, String actividades, String impuestosActivos, boolean tieneEmpleados) {
        this.cuit = cuit;
        this.razonSocial = razonSocial;
        this.nombreFantasia = nombreFantasia;
        this.direccion = direccion;
        this.actividades = actividades;
        this.impuestosActivos = impuestosActivos;
        this.tieneEmpleados = tieneEmpleados;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreFantasia() {
        return nombreFantasia;
    }

    public void setNombreFantasia(String nombreFantasia) {
        this.nombreFantasia = nombreFantasia;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getActividades() {
        return actividades;
    }

    public void setActividades(String actividades) {
        this.actividades = actividades;
    }

    public String getImpuestosActivos() {
        return impuestosActivos;
    }

    public void setImpuestosActivos(String impuestosActivos) {
        this.impuestosActivos = impuestosActivos;
    }

    public boolean isTieneEmpleados() {
        return tieneEmpleados;
    }

    public void setTieneEmpleados(boolean tieneEmpleados) {
        this.tieneEmpleados = tieneEmpleados;
    }
    
    
    
    
}
