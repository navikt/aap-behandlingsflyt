package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.Tidslinje

class SammenstiltAktivitetspliktRegel : UnderveisRegel {
    companion object {
        private val aktivitetspliktRegler = listOf(
            Aktivitetsplikt11_7Regel(),
        )
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        return aktivitetspliktRegler.fold(resultat) { foreløpigResultat, regel ->
            regel.vurder(input, foreløpigResultat)
        }
    }
}