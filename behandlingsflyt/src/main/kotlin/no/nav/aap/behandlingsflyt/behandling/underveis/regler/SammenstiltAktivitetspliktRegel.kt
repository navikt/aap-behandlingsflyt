package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.Tidslinje

class SammenstiltAktivitetspliktRegel : UnderveisRegel {
    companion object {
        /**
         * Vurdering av 11-8 forutsetter at 11-7 allerede er vurdert [no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktRegel]
         * Vurdering av 11-9 fortsetter at 11-8 allerede er vurdert [no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetRegel]
         */
        private val aktivitetspliktRegler = listOf(
            AktivitetspliktRegel(),
            FraværFastsattAktivitetRegel(), /* Vurdering av 11-8 forutsetter at 11-7 allerede er vurdert [no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktRegel] */
            ReduksjonAktivitetspliktRegel(), /* Vurdering av 11-9 fortsetter at 11-8 allerede er vurdert [FraværFastsattAktivitetRegel] */
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        return aktivitetspliktRegler.fold(resultat) { foreløpigResultat, regel ->
            regel.vurder(input, foreløpigResultat)
        }
    }
}