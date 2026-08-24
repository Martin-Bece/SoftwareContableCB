/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Martin
 */
public class contacto {
    
    private String cuit; //primary key
    
    private String razonSocial;
    
    private String tipo; // CLIENTE, PROVEEDOR, AMBOS

    public contacto() {
    }

    public contacto(String cuit, String razonSocial, String tipo) {
        this.cuit = cuit;
        this.razonSocial = razonSocial;
        this.tipo = tipo;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    
    
}
