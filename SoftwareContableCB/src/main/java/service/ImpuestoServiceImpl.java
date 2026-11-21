package service;

import view.Modelos;
import view.Modelos.*;
import view.Servicios.ClienteService;
import view.Servicios.ComprobanteService;
import view.Servicios.ImpuestoService;
import view.Servicios.PercepcionService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Determinacion de impuestos del periodo, calculada sobre los comprobantes y
 * percepciones/retenciones guardados en la base.
 *
 * <p><b>Estado de cada calculo</b> (ver README, "Pendientes con la contadora"):</p>
 * <ul>
 *   <li><b>IVA</b>: debito fiscal (IVA ventas) - credito fiscal (IVA compras), notas de credito restan;
 *       menos percepciones y retenciones de IVA sufridas. El saldo a favor del periodo anterior
 *       todavia NO se arrastra (queda en 0) hasta confirmar el criterio con la contadora.</li>
 *   <li><b>Ganancias</b> y <b>SUSS</b>: <b>formulas provisorias</b> heredadas del mock
 *       (35% sobre neto ventas - neto compras; SUSS sobre una nomina estimada en el 12% de las ventas).
 *       No usar para liquidar.</li>
 * </ul>
 */
public class ImpuestoServiceImpl implements ImpuestoService {

    /** Alicuota provisoria de Ganancias (mock). A definir con la contadora. */
    static final BigDecimal ALICUOTA_GANANCIAS = new BigDecimal("35.00");
    /** Nomina estimada como porcentaje de las ventas (mock). A reemplazar por la nomina real (F.931). */
    static final BigDecimal NOMINA_SOBRE_VENTAS = new BigDecimal("0.12");
    static final BigDecimal ALICUOTA_CONTRIBUCIONES = new BigDecimal("24.00");
    static final BigDecimal ALICUOTA_APORTES = new BigDecimal("17.00");

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ClienteService clientes;
    private final ComprobanteService comprobantes;
    private final PercepcionService percepciones;

    public ImpuestoServiceImpl(ClienteService clientes, ComprobanteService comprobantes,
                               PercepcionService percepciones) {
        this.clientes = clientes;
        this.comprobantes = comprobantes;
        this.percepciones = percepciones;
    }

    @Override
    public PosicionIva determinarIva(String cuitCliente, YearMonth periodo) {
        PosicionIva pos = new PosicionIva();
        pos.periodo = periodo;

        for (Comprobante c : comprobantes.listarComprobantes(cuitCliente, Operacion.VENTA, periodo, null)) {
            BigDecimal signo = BigDecimal.valueOf(c.signo());
            pos.cantidadVentas++;
            pos.netoVentas = pos.netoVentas.add(c.netoGravado().multiply(signo));
            pos.debitoFiscal = pos.debitoFiscal.add(c.ivaTotal().multiply(signo));
        }
        for (Comprobante c : comprobantes.listarComprobantes(cuitCliente, Operacion.COMPRA, periodo, null)) {
            BigDecimal signo = BigDecimal.valueOf(c.signo());
            pos.cantidadCompras++;
            pos.netoCompras = pos.netoCompras.add(c.netoGravado().multiply(signo));
            pos.creditoFiscal = pos.creditoFiscal.add(c.ivaTotal().multiply(signo));
        }
        for (PercepcionRetencion p : percepciones.listarPercepciones(cuitCliente, periodo, null, Tributo.IVA)) {
            if (p.clase == Clase.PERCEPCION) {
                pos.percepcionesSufridas = pos.percepcionesSufridas.add(Modelos.nz(p.importe));
            } else {
                pos.retencionesSufridas = pos.retencionesSufridas.add(Modelos.nz(p.importe));
            }
        }
        pos.saldoPeriodoAnterior = BigDecimal.ZERO; // pendiente: arrastre del saldo a favor
        return pos;
    }

    /** Ganancias y SUSS (provisorios). Para IVA usar {@link #determinarIva}; otros tributos devuelven vacio. */
    @Override
    public List<ResumenTributo> determinar(String cuitCliente, YearMonth periodo, Tributo tributo) {
        List<ResumenTributo> out = new ArrayList<>();
        if (tributo == Tributo.GANANCIAS) {
            out.add(ganancias(cuitCliente, periodo));
        } else if (tributo == Tributo.SUSS) {
            Cliente c = clientes.buscarPorCuit(cuitCliente);
            if (c != null && c.isTieneEmpleados()) out.addAll(suss(cuitCliente, periodo));
        }
        return out;
    }

    private ResumenTributo ganancias(String cuitCliente, YearMonth periodo) {
        PosicionIva iva = determinarIva(cuitCliente, periodo);
        ResumenTributo r = new ResumenTributo();
        r.tributo = Tributo.GANANCIAS;
        r.concepto = "Anticipo del periodo";
        r.detalle = "Estimado sobre el resultado bruto del mes (calculo provisorio)";
        // Si el mes dio perdida no hay impuesto (el mock calculaba un impuesto negativo).
        BigDecimal resultado = iva.netoVentas.subtract(iva.netoCompras);
        r.baseImponible = Modelos.dosDecimales(resultado.max(BigDecimal.ZERO));
        r.alicuota = ALICUOTA_GANANCIAS;
        r.determinado = porcentaje(r.baseImponible, r.alicuota);
        for (PercepcionRetencion p : percepciones.listarPercepciones(cuitCliente, periodo, null, Tributo.GANANCIAS)) {
            r.ingresosACuenta = r.ingresosACuenta.add(Modelos.nz(p.importe));
        }
        return r;
    }

    private List<ResumenTributo> suss(String cuitCliente, YearMonth periodo) {
        PosicionIva iva = determinarIva(cuitCliente, periodo);
        BigDecimal nomina = Modelos.dosDecimales(iva.netoVentas.max(BigDecimal.ZERO).multiply(NOMINA_SOBRE_VENTAS));

        ResumenTributo contribuciones = new ResumenTributo();
        contribuciones.tributo = Tributo.SUSS;
        contribuciones.concepto = "Contribuciones patronales";
        contribuciones.detalle = "F.931 · nomina estimada (calculo provisorio)";
        contribuciones.baseImponible = nomina;
        contribuciones.alicuota = ALICUOTA_CONTRIBUCIONES;
        contribuciones.determinado = porcentaje(nomina, ALICUOTA_CONTRIBUCIONES);

        ResumenTributo aportes = new ResumenTributo();
        aportes.tributo = Tributo.SUSS;
        aportes.concepto = "Aportes retenidos al personal";
        aportes.detalle = "F.931 · retenido a empleados (calculo provisorio)";
        aportes.baseImponible = nomina;
        aportes.alicuota = ALICUOTA_APORTES;
        aportes.determinado = porcentaje(nomina, ALICUOTA_APORTES);

        // Pendiente con la contadora: si las retenciones de SUSS sufridas se toman a cuenta.
        return List.of(contribuciones, aportes);
    }

    private static BigDecimal porcentaje(BigDecimal base, BigDecimal alicuota) {
        return Modelos.dosDecimales(base.multiply(alicuota).divide(CIEN));
    }
}
