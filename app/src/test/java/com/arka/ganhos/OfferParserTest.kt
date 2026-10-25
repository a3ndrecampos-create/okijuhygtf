package com.arka.ganhos

import com.arka.ganhos.core.OfferParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferParserTest {
    @Test
    fun parsesTypicalOffer() {
        val texts = listOf("UberX", "R$ 18,50", "4 min (1,2 km) de distância", "14 min (6,3 km) de viagem", "4,85", "Aceitar")
        val o = OfferParser.parse(texts, "Uber")
        assertNotNull(o)
        assertEquals(18.5, o!!.price, 0.001)
        assertEquals(1.2, o.pickupKm, 0.001)
        assertEquals(4, o.pickupMin)
        assertEquals(6.3, o.tripKm, 0.001)
        assertEquals(14, o.tripMin)
        assertEquals(4.85, o.rating!!, 0.001)
    }

    @Test
    fun ignoresScreensWithoutAcceptButton() {
        val texts = listOf("Ganhos de hoje", "R$ 120,00", "2 min (1 km)")
        assertNull(OfferParser.parse(texts, "Uber"))
    }

    @Test
    fun readsThousandsSeparator() {
        val texts = listOf("R$ 1.234,56", "30 min (20 km)", "Aceitar")
        assertEquals(1234.56, OfferParser.parse(texts, "99")!!.price, 0.001)
    }

    @Test
    fun readsMetersAndHours() {
        val texts = listOf("R$ 25,00", "3 min (800 m)", "1 h 5 min (40 km)", "Aceitar")
        val o = OfferParser.parse(texts, "99")!!
        assertEquals(0.8, o.pickupKm, 0.001)
        assertEquals(65, o.tripMin)
        assertEquals(40.0, o.tripKm, 0.001)
    }

    @Test
    fun singleDistanceIsTreatedAsTrip() {
        val o = OfferParser.parse(listOf("R$ 12,00", "15 min (8 km)", "Aceitar"), "99")!!
        assertEquals(0, o.pickupMin)
        assertEquals(8.0, o.tripKm, 0.001)
    }

    @Test
    fun parsesRealUberPriorityCard() {
        val texts = listOf(
            "Priority", "Exclusivo", "R$ 10,32", "Est. de R$3,44/km", "+R$ 3,00 incluído",
            "+R$ 1,70 incluída para retirada", "3 min • 1.0 km", "Rua Carlos Drumond de Andrade, Taboão da Serra",
            "4,91 (944)", "Verificado", "8 minutos • 2.0 km", "Estr. Benedito Cesário de Oliveira, 202", "Aceitar"
        )
        val o = OfferParser.parse(texts, "Uber")!!
        assertEquals(10.32, o.price, 0.001)
        assertEquals(1.0, o.pickupKm, 0.001)
        assertEquals(3, o.pickupMin)
        assertEquals(2.0, o.tripKm, 0.001)
        assertEquals(8, o.tripMin)
        assertEquals(4.91, o.rating!!, 0.001)
    }

    @Test
    fun parsesNonBreakingSpacesAndInvisibleMarks() {
        val texts = listOf(
            "R$\u00A010,32", "3\u00A0min\u00A0\u2022\u00A01.0\u00A0km",
            "\u200E4,91\u00A0(944)", "8\u202Fminutos\u00A0•\u00A02.0\u00A0km", "Aceitar"
        )
        val o = OfferParser.parse(texts, "Uber")!!
        assertEquals(10.32, o.price, 0.001)
        assertEquals(1.0, o.pickupKm, 0.001)
        assertEquals(2.0, o.tripKm, 0.001)
        assertEquals(4.91, o.rating!!, 0.001)
    }

    @Test
    fun parsesWithoutAcceptButtonWhenBothRoutesPresent() {
        val o = OfferParser.parse(listOf("R$ 10,32", "3 min • 1.0 km", "8 minutos • 2.0 km"), "Uber")
        assertNotNull(o)
    }

    @Test
    fun parsesLongTripWithHoursAndViagemButton() {
        val o = OfferParser.parse(listOf(
            "UberX", "R$ 100,72", "Est. de R$1,92/km", "5 min • 1.7 km", "4,90",
            "1 h e 2 min • 50.7 km", "Viagem longa (mais de 45 min)", "Viagem"
        ), "Uber")!!
        assertEquals(100.72, o.price, 0.001)
        assertEquals(62, o.tripMin)
        assertEquals(50.7, o.tripKm, 0.001)
        assertEquals(4.9, o.rating!!, 0.001)
    }

    @Test
    fun parses99CardWithParenthesesAndNoSpaceAfterReal() {
        val texts = listOf(
            "Dinheiro", "R$35,20", "1,4x", "R$1,88/km", "Toque abaixo para aceitar",
            "R$1,95 por espera longa incl.", "4,98 • 124 corridas", "Perfil Premium",
            "7 min (2,3 km)", "Avenida Cid Nelson Jordano, 274, Jardim Record",
            "30 min (16,4 km)", "Rua Leopoldo Couto Magalhães Júnior, 356, Itaim Bibi"
        )
        val o = OfferParser.parse(texts, "99")!!
        assertEquals(35.20, o.price, 0.001)
        assertEquals(2.3, o.pickupKm, 0.001)
        assertEquals(7, o.pickupMin)
        assertEquals(16.4, o.tripKm, 0.001)
        assertEquals(30, o.tripMin)
        assertEquals(4.98, o.rating!!, 0.001)
    }

    @Test
    fun parses99PriorityCard() {
        val texts = listOf(
            "Pgto. no app", "Prioritário", "R$18,06", "1,1x", "R$1,82/km", "Toque abaixo para aceitar",
            "R$1,95 por corrida noturna incl.", "4,94", "651 corridas", "6 min (2,1 km)",
            "Rua Alfredo Schultz, 149, Nucleo Res. Isabela", "12 min (7,8 km)", "Mercado Livre - Embu das Artes"
        )
        val o = OfferParser.parse(texts, "99")!!
        assertEquals(18.06, o.price, 0.001)
        assertEquals(2.1, o.pickupKm, 0.001)
        assertEquals(7.8, o.tripKm, 0.001)
        assertEquals(12, o.tripMin)
    }

    @Test
    fun recoversMissingPickupFromShownPerKm() {
        // OCR leu só a viagem; 35,20 ÷ 1,88 = 18,7 km no total
        val texts = listOf("Dinheiro", "R$35,20", "R$1,88/km", "Toque abaixo para aceitar", "30 min (16,4 km)")
        val o = OfferParser.parse(texts, "99", strict = true)!!
        assertEquals(16.4, o.tripKm, 0.001)
        assertEquals(2.3, o.pickupKm, 0.05)
    }

    @Test
    fun strictModeWaitsWhenTotalsDoNotMatch() {
        // "2,3" lido como "23": total 39,4 km não fecha com 18,7 km
        val texts = listOf("R$35,20", "R$1,88/km", "Toque abaixo para aceitar", "7 min (23 km)", "30 min (16,4 km)")
        assertNull(OfferParser.parse(texts, "99", strict = true))
        assertNotNull(OfferParser.parse(texts, "99"))
    }

    @Test
    fun toleratesOcrTypos() {
        val texts = listOf("R$35,20", "Toque abaixo para aceitar", "7 rnin (2,3 kn)", "30 mín (16,4 km)")
        val o = OfferParser.parse(texts, "99")!!
        assertEquals(2.3, o.pickupKm, 0.001)
        assertEquals(16.4, o.tripKm, 0.001)
    }

    @Test
    fun parses99NegociaCardIgnoringCounterOffers() {
        val texts = listOf(
            "Negocia • Dinheiro", "R$17,76", "R$1,66/km", "4,91 • +999 corridas • Perfil Premium",
            "7 min (2,2 km)", "Rua José Micheloni Filho, 83, Jardim Salete",
            "21 min (8,4 km)", "Estação de Metrô Vila Sônia, Rua Heitor dos Prazeres - Ferreira",
            "Aceitar por R$17,76", "R$18,65", "R$19,54", "R$20,42"
        )
        val o = OfferParser.parse(texts, "99", strict = true)!!
        assertEquals(17.76, o.price, 0.001)
        assertEquals(2.2, o.pickupKm, 0.001)
        assertEquals(8.4, o.tripKm, 0.001)
        assertEquals(21, o.tripMin)
        assertEquals(4.91, o.rating!!, 0.001)
    }

    @Test
    fun picksPriceThatMatchesPerKmWhenNoAcceptPriceButton() {
        // contrapropostas maiores que o valor da oferta, sem o botão "Aceitar por"
        val texts = listOf(
            "R$17,76", "R$1,66/km", "Toque abaixo para aceitar", "7 min (2,2 km)", "21 min (8,4 km)",
            "R$18,65", "R$19,54", "R$20,42"
        )
        assertEquals(17.76, OfferParser.parse(texts, "99", strict = true)!!.price, 0.001)
    }

    @Test
    fun ignoresBalanceLabelledNextToOffer() {
        val texts = listOf(
            "Saldo total", "R$ 845,20", "UberX", "R$ 10,32", "Est. de R$3,44/km",
            "3 min • 1.0 km", "8 minutos • 2.0 km", "Aceitar"
        )
        assertEquals(10.32, OfferParser.parse(texts, "Uber")!!.price, 0.001)
    }

    @Test
    fun ignoresBalanceValueWhenLabelIsAfterIt() {
        val texts = listOf("R$ 845,20", "Saldo disponível", "R$ 10,32", "3 min • 1.0 km", "8 min • 2.0 km", "Aceitar")
        assertEquals(10.32, OfferParser.parse(texts, "99")!!.price, 0.001)
    }

    @Test
    fun ignoresUnlabelledBalanceFarFromTheCard() {
        // valor solto no topo da tela (sem rótulo) e longe do card da oferta
        val filler = List(14) { "Mapa $it" }
        val texts = listOf("R$ 845,20") + filler + listOf("R$ 10,32", "3 min • 1.0 km", "8 min • 2.0 km", "Aceitar")
        assertEquals(10.32, OfferParser.parse(texts, "Uber")!!.price, 0.001)
    }

    @Test
    fun balanceScreenWithoutAcceptButtonIsNotAnOffer() {
        val texts = listOf("Saldo total", "R$ 845,20", "12 min (5 km)", "20 min (9 km)")
        assertNull(OfferParser.parse(texts, "99"))
    }

    @Test
    fun ignoresEarningsHeaderOnOcrToo() {
        val texts = listOf("Ganhos de hoje", "R$ 312,40", "R$ 10,32", "3 min • 1.0 km", "8 min • 2.0 km", "Aceitar")
        assertEquals(10.32, OfferParser.parse(texts, "Uber", strict = true)!!.price, 0.001)
    }

    @Test
    fun inDriveCardReadsPriceAndPickupOnly() {
        val texts = listOf(
            "Pedido de corrida", "7 min.", "3,3 km", "Guilherme", "5 (5)",
            "Rua 6 número 375 portao verde estou de camisa vermelha !",
            "Rua Francisco Gentil de Guzzi 46 (Parque Santa Felicia Jardim)", "R$12",
            "Aceitar por R$12", "Ofereça a sua tarifa para a viagem", "R$13", "R$14", "R$16", "Fechar"
        )
        val o = OfferParser.parse(texts, "inDrive")!!
        assertEquals(12.0, o.price, 0.001)
        assertEquals(3.3, o.pickupKm, 0.001)
        assertEquals(7, o.pickupMin)
        assertEquals(5.0, o.rating!!, 0.001)
        assertEquals(false, o.tripKnown)
    }

    @Test
    fun inDriveCardWithBothRoutes() {
        val texts = listOf(
            "Pedido de corrida", "9 min.", "3,7 km", "8 min.", "3,6 km", "Matheus", "★5 (217)",
            "Rua Barão do Rio Branco 965 (Parque Anhanguera)", "R$ 10",
            "Aceitar por R$ 10", "Ofereça sua tarifa:", "R$ 12", "R$ 13", "R$ 15"
        )
        val o = OfferParser.parse(texts, "inDrive")!!
        assertEquals(10.0, o.price, 0.001)
        assertEquals(7.3, o.totalKm, 0.001)
        assertEquals(17, o.totalMin)
        assertEquals(true, o.tripKnown)
        assertEquals(5.0, o.rating!!, 0.001)
    }

    @Test
    fun ifoodDeliveryCard() {
        val texts = listOf(
            "Coleta 1 • Restaurante", "Viva! Marmitaria - Santa Lúcia", "Entrega 1", "Buritis",
            "R$ 13,83", "Rota para Moto", "Distância total", "7,08 km", "Tempo aproximado de rota", "40 min",
            "Possibilidade de devolução", "Sim", "Rejeitar", "Aceitar", "52"
        )
        val o = OfferParser.parse(texts, "iFood")!!
        assertEquals(13.83, o.price, 0.001)
        assertEquals(7.08, o.totalKm, 0.001)
        assertEquals(40, o.totalMin)
    }

    @Test
    fun keetaCardReadsPriceAndKmOnly() {
        val texts = listOf(
            "R$ 18,48", "rider", "5,4 km total",
            "R. S. Domingos Donatão, 569 - Jardim Apura, São Paulo - SP, 04470-175",
            "Rua Maria Aparecida de Jesus, 185, São Paulo", "Pegar(40s)"
        )
        val o = OfferParser.parse(texts, "Keeta")!!
        assertEquals(18.48, o.price, 0.001)
        assertEquals(5.4, o.totalKm, 0.001)
        assertEquals(false, o.timeKnown)
    }
}
