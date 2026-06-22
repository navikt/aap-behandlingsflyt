package no.nav.aap.behandlingsflyt.forretningsflyt.steg

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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
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
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurderinger = trukketSøknadRepository.hentTrukketSøknadVurderinger(kontekst.behandlingId)
        val erTilstrekkeligVurdert = vurderinger.isNotEmpty()
        val harTrukketSøknad = vurderinger.any { it.skalTrekkes }

        avklaringsbehovService.oppdaterAvklaringsbehov(
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

        if (erTilstrekkeligVurdert && harTrukketSøknad) {
            /* Frem til [KravSteg] er ferdig utviklet, så kan personer ha flere saker.
             * Det er ikke uvanlig at saker blir trukket ved en feil, og at det så opprettes
             * en ny sak som ville ha overlappet med den trukne saken. Vi setter derfor
             * en fiktiv rettighetsperiode på den trukne saken. */
            sakRepository.oppdaterRettighetsperiode(kontekst.sakId, finnUgyldigRettighetsperiode(kontekst.sakId))
            slettVurderingerOgRegisterdata(kontekst.behandlingId)
        }

        return Fullført
    }

    private fun finnUgyldigRettighetsperiode(sakId: SakId): Periode {
        val førsteLedigeDato = sakRepository.finnSakerFor(sakRepository.finnPersonId(sakId))
            .somTidslinje({ it.rettighetsperiode }, { false }).komprimer()
            .mergePrioriterVenstre(tidslinjeOf(Periode(Tid.MIN, Tid.MAKS) to true))
            .filter { it.verdi }
            .perioder()
            .first()
            .fom
        return Periode(førsteLedigeDato, førsteLedigeDato)
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
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.SØKNAD
        }
    }
}