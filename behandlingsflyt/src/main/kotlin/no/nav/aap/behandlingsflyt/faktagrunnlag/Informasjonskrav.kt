package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

/**
 * Et _Informasjonskrav_ har ansvar for å hente inn nødvendig informasjon for et gitt [BehandlingSteg].
 */
interface Informasjonskrav {
    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }

    /**
     * Skal sjekke om det har kommet ny informasjon på dette kravet, og persistere ny informasjon.
     *
     * Om det ikke er noen endringer, returneres [Endret.IKKE_ENDRET].
     */
    fun oppdater(kontekst: FlytKontekstMedPerioder): Endret
}