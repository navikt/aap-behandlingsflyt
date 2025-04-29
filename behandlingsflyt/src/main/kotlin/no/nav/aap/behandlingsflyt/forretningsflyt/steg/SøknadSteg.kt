package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.VURDER_TREKK_AV_SØKNAD
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling.SØKNAD_TRUKKET
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

/** Formål: Hvilke søknader skal vi saksbehandle?
 *
 * Pr. nå ett tilfelle: Hvis en søknad trekkes av bruker før vi
 * har fattet vedtak.
 *
 * Om det er andre grunner til at man skal se bort fra en søknad, så
 * kan det kanskje løses her.
 */
class SøknadSteg(
    private val trukketSøknadRepository: TrukketSøknadRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (!erRelevant(kontekst)) {
            return Fullført
        }

        return if (trukketSøknadRepository.hentTrukketSøknadVurderinger(kontekst.behandlingId).isEmpty())
            FantAvklaringsbehov(VURDER_TREKK_AV_SØKNAD)
        else
            Fullført
    }

    private fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
        return (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling)
                && (SØKNAD_TRUKKET in kontekst.vurdering.årsakerTilBehandling)
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return SøknadSteg(
                RepositoryRegistry.provider(connection).provide()
            )
        }

        override fun type(): StegType {
            return StegType.SØKNAD
        }
    }
}