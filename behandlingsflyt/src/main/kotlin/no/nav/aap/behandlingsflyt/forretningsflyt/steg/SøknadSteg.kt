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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling.SØKNAD_TRUKKET
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

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
    private val repositoryProvider: RepositoryProvider,
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (!erRelevant(kontekst)) {
            return Fullført
        }

        if (trukketSøknadRepository.hentTrukketSøknadVurderinger(kontekst.behandlingId).isNotEmpty()) {
            slettVurderingerOgRegisterdata(kontekst.behandlingId)
            return Fullført
        } else {
            return FantAvklaringsbehov(VURDER_TREKK_AV_SØKNAD)
        }
    }

    private fun slettVurderingerOgRegisterdata(behandlingId: BehandlingId) {
        log.info("sletter vurderinger og registerdata i alle repositories for {}", behandlingId)
        repositoryProvider.provideAlle().forEach { repository ->
            if (repository is no.nav.aap.lookup.repository.Repository) {
                //repository.slett(behandlingId)
            }
        }
    }

    private fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
        return (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling)
                && (SØKNAD_TRUKKET in kontekst.vurdering.årsakerTilBehandling)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return SøknadSteg(
                trukketSøknadRepository = repositoryProvider.provide(),
                repositoryProvider = repositoryProvider,
            )
        }

        override fun type(): StegType {
            return StegType.SØKNAD
        }
    }
}