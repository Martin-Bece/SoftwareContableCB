/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import view.Modelos.Cliente;
import view.Servicios.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;


public class AppContext {
    
    public interface Listener {
        void contextoCambio(AppContext ctx);
    }
    
    private final ClienteService clientes;
    private final ComprobanteService comprobantes;
    private final PercepcionService percepciones;
    private final ImpuestoService impuestos;
    private final ReporteService reportes;
    private final ContraparteService contrapartes;
    
    private final List<Listener> listeners = new ArrayList<>();
    
    private Cliente clienteActivo;
    private YearMonth periodo = YearMonth.now();
    
    public AppContext(ClienteService clientes, ComprobanteService comprobantes,
                      PercepcionService percepciones, ImpuestoService impuestos,
                      ReporteService reportes, ContraparteService contrapartes) {
        this.clientes = clientes;
        this.comprobantes = comprobantes;
        this.percepciones = percepciones;
        this.impuestos = impuestos;
        this.reportes = reportes;
        this.contrapartes = contrapartes;
    }
    
    public ClienteService clientes() { return clientes; }
    public ComprobanteService comprobantes() { return comprobantes; }
    public PercepcionService percepciones() { return percepciones; }
    public ImpuestoService impuestos() { return impuestos; }
    public ReporteService reportes() { return reportes; }
    public ContraparteService contrapartes() { return contrapartes; }
    
    public Cliente getClienteActivo() { return clienteActivo; }
    
    public String cuitActivo() {
        return clienteActivo == null ? null : clienteActivo.getCuit();
    }
    
    public boolean hayClienteActivo() { return clienteActivo != null; }
    
    public void setClienteActivo(Cliente cliente) {
        if(cliente == clienteActivo) return;
        this.clienteActivo = cliente;
        notificar();
    }
    
    public YearMonth getPeriodo() { return periodo; }
    
    public void setPeriodo(YearMonth periodo) {
        if (periodo == null || periodo.equals(this.periodo)) return;
        this.periodo = periodo;
        notificar();
    }
    
    public void addListener(Listener l){
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }
    
    public void removeListener(Listener l){
        listeners.remove(l);
    }
    
    public void notificar(){
        for(Listener l : new ArrayList<>(listeners)) {
            l.contextoCambio(this);
        }
    }


}
