package com.sistemaventas.tax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaxTest {

    @Test
    void derivesNetAndContainedIvaFromAFinalPriceSoTheyAddUpExactly() {
        BigDecimal finalPrice = new BigDecimal("18900.00");

        BigDecimal net = IvaCalculator.netFromFinal(finalPrice, new BigDecimal("21"));
        BigDecimal iva = IvaCalculator.ivaContained(finalPrice, new BigDecimal("21"));

        assertEquals(new BigDecimal("15619.83"), net);
        assertEquals(new BigDecimal("3280.17"), iva);
        assertEquals(finalPrice, net.add(iva));
    }

    @Test
    void addsIvaToANetPriceForEveryArgentineRate() {
        BigDecimal net = new BigDecimal("1000.00");

        assertEquals(new BigDecimal("1210.00"), IvaCalculator.finalFromNet(net, AlicuotaIva.GENERAL.percent()));
        assertEquals(new BigDecimal("1105.00"), IvaCalculator.finalFromNet(net, AlicuotaIva.REDUCIDA.percent()));
        assertEquals(new BigDecimal("1270.00"), IvaCalculator.finalFromNet(net, AlicuotaIva.INCREMENTADA.percent()));
        assertEquals(new BigDecimal("1000.00"), IvaCalculator.finalFromNet(net, AlicuotaIva.EXENTO.percent()));
    }

    @Test
    void parsesRatesWrittenTheArgentineWayAndRejectsUnknownOnes() {
        assertEquals(Optional.of(AlicuotaIva.REDUCIDA), AlicuotaIva.parse("10,5"));
        assertEquals(Optional.of(AlicuotaIva.REDUCIDA), AlicuotaIva.parse("10.5%"));
        assertEquals(Optional.of(AlicuotaIva.GENERAL), AlicuotaIva.parse(""));
        assertEquals(Optional.empty(), AlicuotaIva.parse("19"));
        assertEquals("10,5%", AlicuotaIva.REDUCIDA.label());
    }

    @Test
    void validatesCuitCheckDigitAndFormatsIt() {
        assertEquals(Optional.of("20-12345678-6"), Cuit.normalize("20123456786"));
        assertEquals(Optional.of("30-71234567-1"), Cuit.normalize("30.71234567.1"));
        assertTrue(Cuit.isValid("20-12345678-6"));
        assertFalse(Cuit.isValid("20-12345678-5"));
        assertFalse(Cuit.isValid("2012345678"));
        assertFalse(Cuit.isValid(null));
    }

    @Test
    void choosesTheComprobanteLetterFromSellerAndBuyerConditions() {
        assertEquals("B", CondicionIva.comprobanteLetter(CondicionIva.RESPONSABLE_INSCRIPTO, CondicionIva.CONSUMIDOR_FINAL));
        assertEquals("A", CondicionIva.comprobanteLetter(CondicionIva.RESPONSABLE_INSCRIPTO, CondicionIva.RESPONSABLE_INSCRIPTO));
        assertEquals("A", CondicionIva.comprobanteLetter(CondicionIva.RESPONSABLE_INSCRIPTO, CondicionIva.MONOTRIBUTISTA));
        assertEquals("B", CondicionIva.comprobanteLetter(CondicionIva.RESPONSABLE_INSCRIPTO, CondicionIva.EXENTO));
        assertEquals("C", CondicionIva.comprobanteLetter(CondicionIva.MONOTRIBUTISTA, CondicionIva.RESPONSABLE_INSCRIPTO));
    }
}
