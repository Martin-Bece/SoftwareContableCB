package view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DTOs que consumen las vistas. Son deliberadamente planos: la idea es que
 * mapees aca tus entidades del backend (o que reemplaces este archivo por ellas).
 */
public final class Modelos {

    private Modelos() { }

    public enum Operacion {
        VENTA("Ventas"), COMPRA("Compras");
        public final String rotulo;
        Operacion(String rotulo) { this.rotulo = rotulo; }
        @Override public String toString() { return rotulo; }
    }

    public enum Tributo {
        IVA("IVA"), GANANCIAS("Ganancias"), SUSS("SUSS"), IIBB("Ingresos Brutos");
        public final String rotulo;
        Tributo(String rotulo) { this.rotulo = rotulo; }
        @Override public String toString() { return rotulo; }
    }

    public enum Clase {
        PERCEPCION("Percepcion"), RETENCION("Retencion");
        public final String rotulo;
        Clase(String rotulo) { this.rotulo = rotulo; }
        @Override public String toString() { return rotulo; }
    }

    public static final String[] TIPOS_COMPROBANTE = {
            "Factura A", "Factura B", "Factura C", "Factura M",
            "Nota de credito A", "Nota de credito B", "Nota de credito C",
            "Nota de debito A", "Nota de debito B", "Nota de debito C",
            "Tique factura", "Despacho de importacion"
    };

    public static final String[] IMPUESTOS_DISPONIBLES = {
            "IVA", "Ganancias", "SUSS", "Ingresos Brutos", "Monotributo", "Bienes Personales"
    };

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal dosDecimales(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP);
    }

    /* ------------------------------------------------------------------ */
    public static class Actividad {
        public String codigo = "";
        public String nombre = "";

        public Actividad() { }

        public Actividad(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }

        /** Formato de persistencia: "codigo - nombre". */
        public static Actividad desdeTexto(String texto) {
            if (texto == null || texto.trim().isEmpty()) return new Actividad();
            String[] partes = texto.split("-", 2);
            return partes.length == 2
                    ? new Actividad(partes[0].trim(), partes[1].trim())
                    : new Actividad("", texto.trim());
        }

        @Override public String toString() {
            return codigo.isEmpty() ? nombre : codigo + " - " + nombre;
        }
    }

    /* ------------------------------------------------------------------ */
    public static class Cliente {
        private String cuit = "";
        private String razonSocial = "";
        private String nombreFantasia = "";
        private String direccion = "";
        private final List<Actividad> actividades = new ArrayList<>();
        private final Set<String> impuestos = new LinkedHashSet<>();
        private boolean tieneEmpleados;

        public Cliente() { }

        public Cliente(String cuit, String razonSocial, String nombreFantasia,
                       String direccion, boolean tieneEmpleados) {
            this.cuit = cuit;
            this.razonSocial = razonSocial;
            this.nombreFantasia = nombreFantasia;
            this.direccion = direccion;
            this.tieneEmpleados = tieneEmpleados;
        }

        public String getCuit() { return cuit; }
        public void setCuit(String cuit) { this.cuit = cuit; }

        public String getRazonSocial() { return razonSocial; }
        public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

        public String getNombreFantasia() { return nombreFantasia; }
        public void setNombreFantasia(String nombreFantasia) { this.nombreFantasia = nombreFantasia; }

        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }

        public List<Actividad> getActividades() { return actividades; }

        public Set<String> getImpuestos() { return impuestos; }

        /** Requisito: impuestos como String separado por comas. */
        public String getImpuestosComoTexto() {
            return String.join(", ", impuestos);
        }

        public void setImpuestosDesdeTexto(String texto) {
            impuestos.clear();
            if (texto == null) return;
            for (String parte : texto.split(",")) {
                String t = parte.trim();
                if (!t.isEmpty()) impuestos.add(t);
            }
        }

        public boolean isTieneEmpleados() { return tieneEmpleados; }
        public void setTieneEmpleados(boolean tieneEmpleados) { this.tieneEmpleados = tieneEmpleados; }

        public String etiquetaCorta() {
            String base = (nombreFantasia != null && !nombreFantasia.isEmpty()) ? nombreFantasia : razonSocial;
            return base + "  ·  " + cuit;
        }

        @Override public String toString() { return razonSocial; }
    }

    /* ------------------------------------------------------------------ */
    /** Contraparte: sirve tanto para "clientes del cliente" como para proveedores. */
    public static class Contraparte {
        public String cuit = "";
        public String razonSocial = "";

        public Contraparte() { }

        public Contraparte(String cuit, String razonSocial) {
            this.cuit = cuit;
            this.razonSocial = razonSocial;
        }

        @Override public String toString() { return razonSocial + " (" + cuit + ")"; }
    }

    /* ------------------------------------------------------------------ */
    public static class Comprobante {
        public Long id;
        public Operacion operacion = Operacion.VENTA;
        public LocalDate fecha = LocalDate.now();
        public String tipo = TIPOS_COMPROBANTE[0];
        public String puntoVenta = "0001";
        public String numero = "";
        public String cuitContraparte = "";
        public String razonSocial = "";

        /* Segmento impuestos de la factura */
        public BigDecimal neto21 = BigDecimal.ZERO;
        public BigDecimal iva21 = BigDecimal.ZERO;
        public BigDecimal neto105 = BigDecimal.ZERO;
        public BigDecimal iva105 = BigDecimal.ZERO;
        public BigDecimal neto27 = BigDecimal.ZERO;
        public BigDecimal iva27 = BigDecimal.ZERO;
        public BigDecimal noGravado = BigDecimal.ZERO;
        public BigDecimal exento = BigDecimal.ZERO;
        public BigDecimal percepciones = BigDecimal.ZERO;
        public BigDecimal otrosTributos = BigDecimal.ZERO;

        public String origen = "Manual"; // Manual | CSV | Excel | PDF AFIP

        public String comprobanteCompleto() {
            return tipo + " " + puntoVenta + "-" + numero;
        }

        public BigDecimal netoGravado() {
            return dosDecimales(nz(neto21).add(nz(neto105)).add(nz(neto27)));
        }

        public BigDecimal ivaTotal() {
            return dosDecimales(nz(iva21).add(nz(iva105)).add(nz(iva27)));
        }

        public BigDecimal total() {
            return dosDecimales(netoGravado()
                    .add(ivaTotal())
                    .add(nz(noGravado))
                    .add(nz(exento))
                    .add(nz(percepciones))
                    .add(nz(otrosTributos)));
        }

        /** Las notas de credito restan en la posicion del periodo. */
        public boolean esNotaCredito() {
            return tipo != null && tipo.toLowerCase().startsWith("nota de credito");
        }

        public int signo() {
            return esNotaCredito() ? -1 : 1;
        }
    }

    /* ------------------------------------------------------------------ */
    public static class PercepcionRetencion {
        public Long id;
        public Clase clase = Clase.PERCEPCION;
        public Tributo tributo = Tributo.IVA;
        public LocalDate fecha = LocalDate.now();
        public String regimen = "";
        public String jurisdiccion = "";
        public String comprobante = "";
        public String cuitAgente = "";
        public String razonSocialAgente = "";
        public BigDecimal importe = BigDecimal.ZERO;
        public String origen = "Manual";
    }

    /* ------------------------------------------------------------------ */
    /** Resultado del calculo de la posicion mensual de IVA. */
    public static class PosicionIva {
        public YearMonth periodo;
        public int cantidadVentas;
        public int cantidadCompras;
        public BigDecimal netoVentas = BigDecimal.ZERO;
        public BigDecimal netoCompras = BigDecimal.ZERO;
        public BigDecimal debitoFiscal = BigDecimal.ZERO;      // IVA de ventas
        public BigDecimal creditoFiscal = BigDecimal.ZERO;     // IVA de compras
        public BigDecimal percepcionesSufridas = BigDecimal.ZERO;
        public BigDecimal retencionesSufridas = BigDecimal.ZERO;
        public BigDecimal saldoPeriodoAnterior = BigDecimal.ZERO;

        public BigDecimal saldoTecnico() {
            return dosDecimales(nz(debitoFiscal).subtract(nz(creditoFiscal)));
        }

        public BigDecimal ingresosDirectos() {
            return dosDecimales(nz(percepcionesSufridas).add(nz(retencionesSufridas)));
        }

        public BigDecimal saldoFinal() {
            return dosDecimales(saldoTecnico()
                    .subtract(ingresosDirectos())
                    .subtract(nz(saldoPeriodoAnterior)));
        }

        public boolean aPagar() {
            return saldoFinal().compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /* ------------------------------------------------------------------ */
    /** Fila de los reportes mensuales. */
    public static class TotalPeriodo {
        public YearMonth periodo;
        public int cantidadVentas;
        public int cantidadCompras;
        public BigDecimal netoVentas = BigDecimal.ZERO;
        public BigDecimal ivaVentas = BigDecimal.ZERO;
        public BigDecimal netoCompras = BigDecimal.ZERO;
        public BigDecimal ivaCompras = BigDecimal.ZERO;

        public TotalPeriodo() { }

        public TotalPeriodo(YearMonth periodo) { this.periodo = periodo; }

        public BigDecimal totalVentas() { return dosDecimales(nz(netoVentas).add(nz(ivaVentas))); }
        public BigDecimal totalCompras() { return dosDecimales(nz(netoCompras).add(nz(ivaCompras))); }
        public BigDecimal resultado() { return dosDecimales(nz(netoVentas).subtract(nz(netoCompras))); }
    }

    /* ------------------------------------------------------------------ */
    /** Resumen de Ganancias / SUSS para la vista de impuestos. */
    public static class ResumenTributo {
        public Tributo tributo;
        public String concepto = "";
        public String detalle = "";
        public BigDecimal baseImponible = BigDecimal.ZERO;
        public BigDecimal alicuota = BigDecimal.ZERO;
        public BigDecimal determinado = BigDecimal.ZERO;
        public BigDecimal ingresosACuenta = BigDecimal.ZERO;

        public BigDecimal aPagar() {
            return dosDecimales(nz(determinado).subtract(nz(ingresosACuenta)));
        }
    }
}