package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository
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
    private val sakRepository: SakRepository,
    private val repositoryProvider: RepositoryProvider,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val erTilstrekkeligVurdert = trukketSøknadRepository.hentTrukketSøknadVurderinger(kontekst.behandlingId).isNotEmpty()

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.VURDER_TREKK_AV_SØKNAD,
            vedtakBehøverVurdering = {
                // Her gir det faktisk mening å sjekke på behandlingstype, siden man ikke kan trekke søknad
                // i en revurdering.
                (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling)
                        && (Vurderingsbehov.SØKNAD_TRUKKET in kontekst.vurderingsbehovRelevanteForSteg)
            },
            erTilstrekkeligVurdert = {
                erTilstrekkeligVurdert
            },
            tilbakestillGrunnlag = {},
            kontekst = kontekst,
        )

        if (erTilstrekkeligVurdert) {
            val rettighetsperiode = kontekst.rettighetsperiode
            // Setter ny rettighetsperiode til én dag lang
            val nyRettighetsPeriode = Periode(rettighetsperiode.fom, rettighetsperiode.fom)
            sakRepository.oppdaterRettighetsperiode(kontekst.sakId, nyRettighetsPeriode)
            slettVurderingerOgRegisterdata(kontekst.behandlingId)
        }

        return Fullført
    }

    private fun slettVurderingerOgRegisterdata(behandlingId: BehandlingId) {
        log.info("Sletter vurderinger og registerdata i alle repositories for {}", behandlingId)
        repositoryProvider.provideAlle().forEach { repository ->
            if (repository is Repository) {
                repository.slett(behandlingId)
            }
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SøknadSteg(
                trukketSøknadRepository = repositoryProvider.provide(),
                repositoryProvider = repositoryProvider,
                sakRepository = repositoryProvider.provide(),
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.SØKNAD
        }
    }
}