package view;

import view.Modelos.*;
import view.Servicios.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Implementacion en memoria con datos de demostracion.
 * Sirve para ver y probar las vistas; se descarta al conectar el backend real.
 *
 * <p>Tiene dos modos:</p>
 * <ul>
 *   <li>{@link #ServiciosMock()}: todo en memoria con datos de ejemplo (modo demo).</li>
 *   <li>{@link #ServiciosMock(ClienteService, ComprobanteService)}: modo hibrido. Clientes y
 *       comprobantes vienen del backend real; percepciones y contrapartes siguen en memoria
 *       (vacias, se pierden al cerrar) y los calculos de impuestos y reportes se hacen
 *       sobre los comprobantes reales.</li>
 * </ul>
 */
public class ServiciosMock implements ClienteService, ComprobanteService, PercepcionService,
        ImpuestoService, ReporteService, ContraparteService {

    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private final Map<String, List<Comprobante>> comprobantes = new LinkedHashMap<>();
    private final Map<String, List<PercepcionRetencion>> percepciones = new LinkedHashMap<>();
    private final Map<String, List<Contraparte>> clientesDeCliente = new LinkedHashMap<>();
    private final Map<String, List<Contraparte>> proveedores = new LinkedHashMap<>();
    private final Random rnd = new Random(20260919L);
    private long secuencia = 1;

    /** De donde salen clientes y comprobantes para los calculos (este mismo mock o el backend real). */
    private final ClienteService fuenteClientes;
    private final ComprobanteService fuenteComprobantes;

    public ServiciosMock() {
        this.fuenteClientes = this;
        this.fuenteComprobantes = this;
        semilla();
    }

    /** Modo hibrido: ver la documentacion de la clase. No carga datos de ejemplo. */
    public ServiciosMock(ClienteService clientesReales, ComprobanteService comprobantesReales) {
        this.fuenteClientes = Objects.requireNonNull(clientesReales);
        this.fuenteComprobantes = Objects.requireNonNull(comprobantesReales);
    }

    /* ================================================================
     *  Datos de demostracion
     * ================================================================ */
    private void semilla() {
        Cliente c1 = new Cliente("30-71234567-9", "Distribuidora del Cuyo S.R.L.", "DisCuyo",
                "Av. Illia 1240, San Luis", true);
        c1.getActividades().add(new Actividad("463199", "Venta al por mayor de productos alimenticios"));
        c1.getActividades().add(new Actividad("492280", "Servicio de transporte automotor de cargas"));
        c1.setImpuestosDesdeTexto("IVA, Ganancias, SUSS, Ingresos Brutos");

        Cliente c2 = new Cliente("27-28456123-4", "Marcela Ferreyra", "Estudio Ferreyra",
                "Rivadavia 455, Villa Mercedes", false);
        c2.getActividades().add(new Actividad("692000", "Servicios de contabilidad y auditoria"));
        c2.setImpuestosDesdeTexto("IVA, Ganancias");

        Cliente c3 = new Cliente("30-70998877-1", "Metalurgica Pringles S.A.", "MetalPringles",
                "Parque Industrial Norte, San Luis", true);
        c3.getActividades().add(new Actividad("259999", "Fabricacion de productos metalicos"));
        c3.setImpuestosDesdeTexto("IVA, Ganancias, SUSS");

        for (Cliente c : Arrays.asList(c1, c2, c3)) {
            clientes.put(c.getCuit(), c);
            comprobantes.put(c.getCuit(), new ArrayList<>());
            percepciones.put(c.getCuit(), new ArrayList<>());
            clientesDeCliente.put(c.getCuit(), new ArrayList<>());
            proveedores.put(c.getCuit(), new ArrayList<>());
        }

        String[][] compradores = {
                {"30-65478912-3", "Supermercados Andinos S.A."},
                {"30-71122334-5", "Almacen Don Pedro S.R.L."},
                {"27-30112233-8", "Lucia Maidana"},
                {"30-69887766-2", "Hotel Potrero S.A."},
                {"33-71559988-9", "Cooperativa La Estancia Ltda."}
        };
        String[][] vendedores = {
                {"30-50001122-7", "Papelera del Centro S.A."},
                {"30-71998877-4", "Combustibles Cuyanos S.R.L."},
                {"30-61234098-6", "Servicios Electricos Union S.A."},
                {"30-70445566-1", "Insumos Industriales Sur S.R.L."},
                {"20-24556677-3", "Ramon Oviedo"}
        };

        for (String cuit : clientes.keySet()) {
            for (String[] p : compradores) {
                clientesDeCliente.get(cuit).add(new Contraparte(p[0], p[1]));
            }
            for (String[] p : vendedores) {
                proveedores.get(cuit).add(new Contraparte(p[0], p[1]));
            }
        }

        YearMonth hasta = YearMonth.now();
        for (String cuit : clientes.keySet()) {
            double escala = cuit.equals(c2.getCuit()) ? 0.25 : cuit.equals(c3.getCuit()) ? 1.6 : 1.0;
            for (int i = 17; i >= 0; i--) {
                YearMonth ym = hasta.minusMonths(i);
                generarMes(cuit, ym, escala, compradores, vendedores);
            }
        }
    }

    private void generarMes(String cuit, YearMonth ym, double escala,
                            String[][] compradores, String[][] vendedores) {
        int ventas = 6 + rnd.nextInt(9);
        int compras = 4 + rnd.nextInt(7);
        List<Comprobante> lista = comprobantes.get(cuit);

        for (int i = 0; i < ventas; i++) {
            Comprobante c = base(ym, Operacion.VENTA, compradores);
            double neto = (120_000 + rnd.nextInt(900_000)) * escala;
            if (rnd.nextInt(12) == 0) {
                c.tipo = "Nota de credito A";
                neto = neto * 0.25;
            }
            c.neto21 = redondear(neto);
            c.iva21 = redondear(neto * 0.21);
            if (rnd.nextInt(5) == 0) {
                c.neto105 = redondear(neto * 0.3);
                c.iva105 = redondear(neto * 0.3 * 0.105);
            }
            if (rnd.nextInt(4) == 0) c.percepciones = redondear(neto * 0.03);
            lista.add(c);
        }

        for (int i = 0; i < compras; i++) {
            Comprobante c = base(ym, Operacion.COMPRA, vendedores);
            double neto = (80_000 + rnd.nextInt(620_000)) * escala;
            c.neto21 = redondear(neto);
            c.iva21 = redondear(neto * 0.21);
            if (rnd.nextInt(6) == 0) c.exento = redondear(neto * 0.12);
            lista.add(c);
        }

        List<PercepcionRetencion> percs = percepciones.get(cuit);
        int cantidad = 2 + rnd.nextInt(4);
        for (int i = 0; i < cantidad; i++) {
            PercepcionRetencion p = new PercepcionRetencion();
            p.id = secuencia++;
            p.clase = rnd.nextBoolean() ? Clase.PERCEPCION : Clase.RETENCION;
            Tributo[] tributos = {Tributo.IVA, Tributo.GANANCIAS, Tributo.IIBB, Tributo.SUSS};
            p.tributo = tributos[rnd.nextInt(tributos.length)];
            p.fecha = ym.atDay(1 + rnd.nextInt(ym.lengthOfMonth()));
            p.regimen = p.tributo == Tributo.IVA ? "RG 2408" : p.tributo == Tributo.GANANCIAS ? "RG 830" : "Conv. Multilateral";
            p.jurisdiccion = p.tributo == Tributo.IIBB ? "San Luis" : "Nacional";
            String[] agente = vendedores[rnd.nextInt(vendedores.length)];
            p.cuitAgente = agente[0];
            p.razonSocialAgente = agente[1];
            p.comprobante = "FA 0003-" + String.format("%08d", 10000 + rnd.nextInt(80000));
            p.importe = redondear((4_000 + rnd.nextInt(90_000)) * escala);
            percs.add(p);
        }
    }

    private Comprobante base(YearMonth ym, Operacion op, String[][] contrapartes) {
        Comprobante c = new Comprobante();
        c.id = secuencia++;
        c.operacion = op;
        c.fecha = ym.atDay(1 + rnd.nextInt(ym.lengthOfMonth()));
        c.tipo = rnd.nextInt(4) == 0 ? "Factura B" : "Factura A";
        c.puntoVenta = String.format("%04d", 1 + rnd.nextInt(3));
        c.numero = String.format("%08d", 1000 + rnd.nextInt(90000));
        String[] cp = contrapartes[rnd.nextInt(contrapartes.length)];
        c.cuitContraparte = cp[0];
        c.razonSocial = cp[1];
        c.origen = rnd.nextInt(3) == 0 ? "PDF AFIP" : rnd.nextInt(2) == 0 ? "CSV" : "Manual";
        return c;
    }

    private BigDecimal redondear(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    /* ================================================================
     *  ClienteService
     * ================================================================ */
    @Override
    public List<Cliente> listar(String filtro) {
        List<Cliente> out = new ArrayList<>();
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        for (Cliente c : clientes.values()) {
            if (f.isEmpty()
                    || c.getRazonSocial().toLowerCase().contains(f)
                    || c.getCuit().toLowerCase().contains(f)
                    || c.getNombreFantasia().toLowerCase().contains(f)) {
                out.add(c);
            }
        }
        return out;
    }

    @Override
    public Cliente buscarPorCuit(String cuit) {
        return clientes.get(cuit);
    }

    @Override
    public void alta(Cliente cliente) {
        if (clientes.containsKey(cliente.getCuit())) {
            throw new ServicioException("Ya existe un cliente con el CUIT " + cliente.getCuit() + ".");
        }
        clientes.put(cliente.getCuit(), cliente);
        comprobantes.put(cliente.getCuit(), new ArrayList<>());
        percepciones.put(cliente.getCuit(), new ArrayList<>());
        clientesDeCliente.put(cliente.getCuit(), new ArrayList<>());
        proveedores.put(cliente.getCuit(), new ArrayList<>());
    }

    @Override
    public void modificar(Cliente cliente) {
        if (!clientes.containsKey(cliente.getCuit())) {
            throw new ServicioException("No existe el cliente " + cliente.getCuit() + ".");
        }
        clientes.put(cliente.getCuit(), cliente);
    }

    @Override
    public void baja(String cuit) {
        List<Comprobante> cs = comprobantes.get(cuit);
        if (cs != null && !cs.isEmpty()) {
            throw new ServicioException("El cliente tiene " + cs.size()
                    + " comprobantes cargados. Archivalos antes de darlo de baja.");
        }
        clientes.remove(cuit);
        comprobantes.remove(cuit);
        percepciones.remove(cuit);
        clientesDeCliente.remove(cuit);
        proveedores.remove(cuit);
    }

    /* ================================================================
     *  ComprobanteService
     * ================================================================ */
    @Override
    public List<Comprobante> listarComprobantes(String cuitCliente, Operacion operacion, YearMonth periodo, String filtro) {
        List<Comprobante> out = new ArrayList<>();
        List<Comprobante> fuente = comprobantes.get(cuitCliente);
        if (fuente == null) return out;
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        for (Comprobante c : fuente) {
            if (operacion != null && c.operacion != operacion) continue;
            if (periodo != null && !YearMonth.from(c.fecha).equals(periodo)) continue;
            if (!f.isEmpty()
                    && !c.razonSocial.toLowerCase().contains(f)
                    && !c.cuitContraparte.toLowerCase().contains(f)
                    && !c.comprobanteCompleto().toLowerCase().contains(f)) continue;
            out.add(c);
        }
        out.sort(Comparator.comparing((Comprobante c) -> c.fecha).thenComparing(c -> c.numero));
        return out;
    }

    @Override
    public void guardarComprobante(String cuitCliente, Comprobante comprobante) {
        List<Comprobante> lista = comprobantes.computeIfAbsent(cuitCliente, k -> new ArrayList<>());
        if (comprobante.id == null) {
            comprobante.id = secuencia++;
            lista.add(comprobante);
        } else {
            for (int i = 0; i < lista.size(); i++) {
                if (comprobante.id.equals(lista.get(i).id)) {
                    lista.set(i, comprobante);
                    return;
                }
            }
            lista.add(comprobante);
        }
    }

    @Override
    public void eliminarComprobante(String cuitCliente, Long id) {
        List<Comprobante> lista = comprobantes.get(cuitCliente);
        if (lista != null) lista.removeIf(c -> id.equals(c.id));
    }

    @Override
    public List<Comprobante> previsualizarComprobantes(File archivo, Operacion operacion) {
        String nombre = archivo.getName().toLowerCase();
        if (nombre.endsWith(".csv")) return leerCsv(archivo, operacion);
        // Excel y PDF: aca iria tu parser (Apache POI / PDFBox). Devuelvo filas de ejemplo.
        List<Comprobante> out = new ArrayList<>();
        String[][] contrapartes = operacion == Operacion.VENTA
                ? new String[][]{{"30-65478912-3", "Supermercados Andinos S.A."}, {"30-71122334-5", "Almacen Don Pedro S.R.L."}}
                : new String[][]{{"30-50001122-7", "Papelera del Centro S.A."}, {"30-71998877-4", "Combustibles Cuyanos S.R.L."}};
        for (int i = 0; i < 6; i++) {
            Comprobante c = base(YearMonth.now(), operacion, contrapartes);
            c.id = null;
            double neto = 90_000 + rnd.nextInt(500_000);
            c.neto21 = redondear(neto);
            c.iva21 = redondear(neto * 0.21);
            c.origen = nombre.endsWith(".pdf") ? "PDF AFIP" : "Excel";
            out.add(c);
        }
        return out;
    }

    /** CSV esperado: fecha;tipo;ptoVenta;numero;cuit;razonSocial;neto;iva;percepciones */
    private List<Comprobante> leerCsv(File archivo, Operacion operacion) {
        List<Comprobante> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primera = true;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                if (primera) {
                    primera = false;
                    if (linea.toLowerCase().contains("fecha")) continue;
                }
                String[] p = linea.split(linea.contains(";") ? ";" : ",");
                if (p.length < 8) continue;
                Comprobante c = new Comprobante();
                c.operacion = operacion;
                c.fecha = LocalDate.parse(p[0].trim());
                c.tipo = p[1].trim();
                c.puntoVenta = p[2].trim();
                c.numero = p[3].trim();
                c.cuitContraparte = p[4].trim();
                c.razonSocial = p[5].trim();
                c.neto21 = Theme.parsear(p[6]);
                c.iva21 = Theme.parsear(p[7]);
                if (p.length > 8) c.percepciones = Theme.parsear(p[8]);
                c.origen = "CSV";
                out.add(c);
            }
        } catch (Exception e) {
            throw new ServicioException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
        return out;
    }

    @Override
    public int importarComprobantes(String cuitCliente, List<Comprobante> lista) {
        int n = 0;
        for (Comprobante c : lista) {
            c.id = null;
            guardarComprobante(cuitCliente, c);
            n++;
        }
        return n;
    }

    /* ================================================================
     *  PercepcionService
     * ================================================================ */
    @Override
    public List<PercepcionRetencion> listarPercepciones(String cuitCliente, YearMonth periodo, Clase clase, Tributo tributo) {
        List<PercepcionRetencion> out = new ArrayList<>();
        List<PercepcionRetencion> fuente = percepciones.get(cuitCliente);
        if (fuente == null) return out;
        for (PercepcionRetencion p : fuente) {
            if (periodo != null && !YearMonth.from(p.fecha).equals(periodo)) continue;
            if (clase != null && p.clase != clase) continue;
            if (tributo != null && p.tributo != tributo) continue;
            out.add(p);
        }
        out.sort(Comparator.comparing(p -> p.fecha));
        return out;
    }

    @Override
    public void guardarPercepcion(String cuitCliente, PercepcionRetencion item) {
        List<PercepcionRetencion> lista = percepciones.computeIfAbsent(cuitCliente, k -> new ArrayList<>());
        if (item.id == null) {
            item.id = secuencia++;
            lista.add(item);
        } else {
            for (int i = 0; i < lista.size(); i++) {
                if (item.id.equals(lista.get(i).id)) {
                    lista.set(i, item);
                    return;
                }
            }
            lista.add(item);
        }
    }

    @Override
    public void eliminarPercepcion(String cuitCliente, Long id) {
        List<PercepcionRetencion> lista = percepciones.get(cuitCliente);
        if (lista != null) lista.removeIf(p -> id.equals(p.id));
    }

    @Override
    public List<PercepcionRetencion> previsualizarPercepciones(File archivo) {
        List<PercepcionRetencion> out = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PercepcionRetencion p = new PercepcionRetencion();
            p.clase = i % 2 == 0 ? Clase.PERCEPCION : Clase.RETENCION;
            p.tributo = i % 3 == 0 ? Tributo.IVA : i % 3 == 1 ? Tributo.GANANCIAS : Tributo.IIBB;
            p.fecha = LocalDate.now().withDayOfMonth(Math.min(5 + i * 3, 28));
            p.regimen = p.tributo == Tributo.GANANCIAS ? "RG 830" : "RG 2408";
            p.jurisdiccion = p.tributo == Tributo.IIBB ? "San Luis" : "Nacional";
            p.cuitAgente = "30-50001122-7";
            p.razonSocialAgente = "Papelera del Centro S.A.";
            p.comprobante = "FA 0001-000" + (12345 + i);
            p.importe = redondear(8_000 + rnd.nextInt(60_000));
            p.origen = archivo.getName().toLowerCase().endsWith(".pdf") ? "PDF AFIP" : "CSV";
            out.add(p);
        }
        return out;
    }

    @Override
    public int importarPercepciones(String cuitCliente, List<PercepcionRetencion> items) {
        int n = 0;
        for (PercepcionRetencion p : items) {
            p.id = null;
            guardarPercepcion(cuitCliente, p);
            n++;
        }
        return n;
    }

    /* ================================================================
     *  ImpuestoService
     * ================================================================ */
    @Override
    public PosicionIva determinarIva(String cuitCliente, YearMonth periodo) {
        PosicionIva pos = new PosicionIva();
        pos.periodo = periodo;

        for (Comprobante c : fuenteComprobantes.listarComprobantes(cuitCliente, Operacion.VENTA, periodo, null)) {
            BigDecimal signo = BigDecimal.valueOf(c.signo());
            pos.cantidadVentas++;
            pos.netoVentas = pos.netoVentas.add(c.netoGravado().multiply(signo));
            pos.debitoFiscal = pos.debitoFiscal.add(c.ivaTotal().multiply(signo));
        }
        for (Comprobante c : fuenteComprobantes.listarComprobantes(cuitCliente, Operacion.COMPRA, periodo, null)) {
            BigDecimal signo = BigDecimal.valueOf(c.signo());
            pos.cantidadCompras++;
            pos.netoCompras = pos.netoCompras.add(c.netoGravado().multiply(signo));
            pos.creditoFiscal = pos.creditoFiscal.add(c.ivaTotal().multiply(signo));
        }
        for (PercepcionRetencion p : listarPercepciones(cuitCliente, periodo, null, Tributo.IVA)) {
            if (p.clase == Clase.PERCEPCION) {
                pos.percepcionesSufridas = pos.percepcionesSufridas.add(Modelos.nz(p.importe));
            } else {
                pos.retencionesSufridas = pos.retencionesSufridas.add(Modelos.nz(p.importe));
            }
        }
        pos.saldoPeriodoAnterior = BigDecimal.ZERO;
        return pos;
    }

    @Override
    public List<ResumenTributo> determinar(String cuitCliente, YearMonth periodo, Tributo tributo) {
        List<ResumenTributo> out = new ArrayList<>();
        PosicionIva iva = determinarIva(cuitCliente, periodo);

        if (tributo == Tributo.GANANCIAS) {
            ResumenTributo r = new ResumenTributo();
            r.tributo = tributo;
            r.concepto = "Anticipo del periodo";
            r.detalle = "Estimado sobre el resultado bruto del mes";
            r.baseImponible = Modelos.dosDecimales(iva.netoVentas.subtract(iva.netoCompras));
            r.alicuota = new BigDecimal("35.00");
            r.determinado = Modelos.dosDecimales(r.baseImponible.multiply(new BigDecimal("0.35")));
            for (PercepcionRetencion p : listarPercepciones(cuitCliente, periodo, null, Tributo.GANANCIAS)) {
                r.ingresosACuenta = r.ingresosACuenta.add(Modelos.nz(p.importe));
            }
            out.add(r);
        } else if (tributo == Tributo.SUSS) {
            Cliente c = fuenteClientes.buscarPorCuit(cuitCliente);
            if (c == null || !c.isTieneEmpleados()) return out;
            ResumenTributo contribuciones = new ResumenTributo();
            contribuciones.tributo = tributo;
            contribuciones.concepto = "Contribuciones patronales";
            contribuciones.detalle = "F.931 · nomina del periodo";
            contribuciones.baseImponible = Modelos.dosDecimales(iva.netoVentas.multiply(new BigDecimal("0.12")));
            contribuciones.alicuota = new BigDecimal("24.00");
            contribuciones.determinado = Modelos.dosDecimales(
                    contribuciones.baseImponible.multiply(new BigDecimal("0.24")));
            out.add(contribuciones);

            ResumenTributo aportes = new ResumenTributo();
            aportes.tributo = tributo;
            aportes.concepto = "Aportes retenidos al personal";
            aportes.detalle = "F.931 · retenido a empleados";
            aportes.baseImponible = contribuciones.baseImponible;
            aportes.alicuota = new BigDecimal("17.00");
            aportes.determinado = Modelos.dosDecimales(
                    aportes.baseImponible.multiply(new BigDecimal("0.17")));
            out.add(aportes);
        }
        return out;
    }

    /* ================================================================
     *  ReporteService
     * ================================================================ */
    @Override
    public List<TotalPeriodo> totalesPorPeriodo(String cuitCliente, YearMonth desde, YearMonth hasta) {
        List<TotalPeriodo> out = new ArrayList<>();
        YearMonth actual = desde;
        while (!actual.isAfter(hasta)) {
            TotalPeriodo t = new TotalPeriodo(actual);
            for (Comprobante c : fuenteComprobantes.listarComprobantes(cuitCliente, Operacion.VENTA, actual, null)) {
                BigDecimal signo = BigDecimal.valueOf(c.signo());
                t.cantidadVentas++;
                t.netoVentas = t.netoVentas.add(c.netoGravado().multiply(signo));
                t.ivaVentas = t.ivaVentas.add(c.ivaTotal().multiply(signo));
            }
            for (Comprobante c : fuenteComprobantes.listarComprobantes(cuitCliente, Operacion.COMPRA, actual, null)) {
                BigDecimal signo = BigDecimal.valueOf(c.signo());
                t.cantidadCompras++;
                t.netoCompras = t.netoCompras.add(c.netoGravado().multiply(signo));
                t.ivaCompras = t.ivaCompras.add(c.ivaTotal().multiply(signo));
            }
            out.add(t);
            actual = actual.plusMonths(1);
        }
        return out;
    }

    /* ================================================================
     *  ContraparteService
     * ================================================================ */
    @Override
    public List<Contraparte> listarClientesDeCliente(String cuitCliente, String filtro) {
        return filtrar(clientesDeCliente.get(cuitCliente), filtro);
    }

    @Override
    public List<Contraparte> listarProveedores(String cuitCliente, String filtro) {
        return filtrar(proveedores.get(cuitCliente), filtro);
    }

    private List<Contraparte> filtrar(List<Contraparte> fuente, String filtro) {
        List<Contraparte> out = new ArrayList<>();
        if (fuente == null) return out;
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        for (Contraparte c : fuente) {
            if (f.isEmpty() || c.razonSocial.toLowerCase().contains(f) || c.cuit.contains(f)) out.add(c);
        }
        return out;
    }

    @Override
    public void guardarClienteDeCliente(String cuitCliente, Contraparte c, String cuitOriginal) {
        guardarContraparte(clientesDeCliente.computeIfAbsent(cuitCliente, k -> new ArrayList<>()), c, cuitOriginal);
    }

    @Override
    public void guardarProveedor(String cuitCliente, Contraparte c, String cuitOriginal) {
        guardarContraparte(proveedores.computeIfAbsent(cuitCliente, k -> new ArrayList<>()), c, cuitOriginal);
    }

    private void guardarContraparte(List<Contraparte> lista, Contraparte c, String cuitOriginal) {
        if (cuitOriginal != null) {
            for (int i = 0; i < lista.size(); i++) {
                if (lista.get(i).cuit.equals(cuitOriginal)) {
                    lista.set(i, c);
                    return;
                }
            }
        }
        for (Contraparte existente : lista) {
            if (existente.cuit.equals(c.cuit)) {
                throw new ServicioException("Ese CUIT ya esta cargado.");
            }
        }
        lista.add(c);
    }

    @Override
    public void eliminarClienteDeCliente(String cuitCliente, String cuit) {
        List<Contraparte> l = clientesDeCliente.get(cuitCliente);
        if (l != null) l.removeIf(c -> c.cuit.equals(cuit));
    }

    @Override
    public void eliminarProveedor(String cuitCliente, String cuit) {
        List<Contraparte> l = proveedores.get(cuitCliente);
        if (l != null) l.removeIf(c -> c.cuit.equals(cuit));
    }
}