/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Martin
 */
public class comprobante {
    
    private Long id;
    
    private String clienteCuit;  // FK al cliente
    
    private String contactoCuit; // FK al proveedor/cliente de su cliente
    
    private String tipoOperacion;// "VENTA" o "COMPRA"
    
    private String fecha;        // YYYY-MM-DD
    
    private String tipoComprobante; // Factura A, B, C, etc.
    
    private double montoNeto;
    
    private double montoIva;
    
    private double montoTotal;

    public comprobante() {
    }

    public comprobante(Long id, String clienteCuit, String contactoCuit, String tipoOperacion, String fecha, String tipoComprobante, double montoNeto, double montoIva, double montoTotal) {
        this.id = id;
        this.clienteCuit = clienteCuit;
        this.contactoCuit = contactoCuit;
        this.tipoOperacion = tipoOperacion;
        this.fecha = fecha;
        this.tipoComprobante = tipoComprobante;
        this.montoNeto = montoNeto;
        this.montoIva = montoIva;
        this.montoTotal = montoTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClienteCuit() {
        return clienteCuit;
    }

    public void setClienteCuit(String clienteCuit) {
        this.clienteCuit = clienteCuit;
    }

    public String getContactoCuit() {
        return contactoCuit;
    }

    public void setContactoCuit(String contactoCuit) {
        this.contactoCuit = contactoCuit;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public double getMontoNeto() {
        return montoNeto;
    }

    public void setMontoNeto(double montoNeto) {
        this.montoNeto = montoNeto;
    }

    public double getMontoIva() {
        return montoIva;
    }

    public void setMontoIva(double montoIva) {
        this.montoIva = montoIva;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }
    
    
    
}
