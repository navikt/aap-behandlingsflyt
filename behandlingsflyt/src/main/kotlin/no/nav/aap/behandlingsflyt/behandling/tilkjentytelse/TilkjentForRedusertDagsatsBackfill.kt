package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent

data class TilkjentForRedusertDagsatsBackfill(
    val tilkjentPeriodeId: Long,
    val dagsats: Beløp,
    val barnetillegg: Beløp,
    val barnepensjonDagsats: Beløp,
    val gradering: Prosent,
    var redusertDagsats: Beløp?
) {
    /**
     * Hent ut full dagsats etter reduksjon.
     */
    fun beregnRedusertDagsats(): Beløp {

        /* I historiske behandlinger, så er ikke `redusertDagsats` lagret
         * ned. Regner derfor ut her. Burde vurdere å backfille, slik at
         * vi ikke trenger denne kodesnutten.
         */
        return BeregnTilkjentYtelseService.redusertDagsats(
            dagsats = dagsats,
            barnetillegg = barnetillegg,
            barnepensjonDagsats = barnepensjonDagsats,
            gradering = gradering,
        )
    }

}