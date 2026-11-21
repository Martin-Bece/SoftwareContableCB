/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;


import view.Modelos.*;

import java.io.File;
import java.time.YearMonth;
import java.util.List;


public class Servicios {
    private Servicios() {}
    
    public static class ServicioException extends RuntimeException {
        public ServicioException(String mensaje) {super(mensaje); }
        public ServicioException(String mensaje, Throwable causa) {super(mensaje, causa); }
    }
    
    public interface ClienteService {
        List<Cliente> listar(String filtro);
        Cliente buscarPorCuit(String cuit);
        void alta(Cliente cliente);
        void modificar(Cliente cliente);
        void baja(String cuit);
    }
    
    public interface ComprobanteService {
        List<Comprobante> listarComprobantes(String cuitCliente, Operacion operacion, YearMonth periodo, String filtro);
        
        void guardarComprobante(String cuitCliente, Comprobante comprobante);
        
        void eliminarComprobante(String cuitCliente, Long id);
        
        List<Comprobante> previsualizarComprobantes(File archivo, Operacion operacion);
        
        int importarComprobantes(String cuitCliente, List<Comprobante> comprobantes);
    }
    
    public interface PercepcionService {
        List<PercepcionRetencion> listarPercepciones(String cuitCliente, YearMonth periodo,
                                                     Clase clase, Tributo tributo);
 
        void guardarPercepcion(String cuitCliente, PercepcionRetencion item);
 
        void eliminarPercepcion(String cuitCliente, Long id);
 
        List<PercepcionRetencion> previsualizarPercepciones(File archivo);
 
        int importarPercepciones(String cuitCliente, List<PercepcionRetencion> items);
    }
    
    public interface ImpuestoService {
        PosicionIva determinarIva(String cuitCliente, YearMonth periodo);
        
        List<ResumenTributo> determinar(String cuitCliente, YearMonth periodo, Tributo tributo);
    }
    
    public interface ReporteService {
        List<TotalPeriodo> totalesPorPeriodo(String cuitCliente, YearMonth desde, YearMonth hasta);
    }
    
    public interface ContraparteService {
        List<Contraparte> listarClientesDeCliente(String cuitCliente, String filtro);
        List<Contraparte> listarProveedores(String cuitCliente, String filtro);
        void guardarClienteDeCliente(String cuitCliente, Contraparte c, String cuitOriginal);
        void guardarProveedor(String cuitCliente, Contraparte c, String cuitOriginal);
        void eliminarClienteDeCliente(String cuitCliente, String cuit);
        void eliminarProveedor(String cuitCliente, String cuit);
    }
}
