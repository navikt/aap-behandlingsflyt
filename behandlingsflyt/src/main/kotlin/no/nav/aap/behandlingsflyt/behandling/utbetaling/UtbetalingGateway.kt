package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto

interface UtbetalingGateway: Gateway {

    fun utbetal(tilkjentYtelseDto: TilkjentYtelseDto): Unit?

}