package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto

interface UtbetalingGateway: Gateway {
    fun utbetal(tilkjentYtelseDto: TilkjentYtelseDto): Unit?
    fun simulering(tilkjentYtelseDto: TilkjentYtelseDto): List<UtbetalingOgSimuleringDto>
}