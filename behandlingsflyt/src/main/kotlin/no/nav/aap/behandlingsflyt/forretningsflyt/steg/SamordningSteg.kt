package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class SamordningSteg(
    private val samordningService: SamordningService,
    private val samordningRepository: SamordningRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val sykepengerOgFerieOppgittISøknadRepository: SykepengerOgFerieOppgittISøknadRepository
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningService = SamordningService(repositoryProvider),
        samordningRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        sykepengerOgFerieOppgittISøknadRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {


        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER
            ),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = ::perioderSomIkkeErTilstrekkeligVurdert,
            tilbakestillGrunnlag = {
                samordningService.tilbakestillVurderinger(kontekst.behandlingId, kontekst.forrigeBehandlingId)
            }
        )

        val samordningYtelseVurderingGrunnlag = samordningService.samordningGrunnlag(behandlingId = kontekst.behandlingId)
        val perioderSomIkkeHarBlittVurdert =
            samordningYtelseVurderingGrunnlag.perioderSomIkkeHarBlittVurdert()

        if (perioderSomIkkeHarBlittVurdert.isEmpty()) {
            val samordningTidslinje = samordningYtelseVurderingGrunnlag.vurder()

            samordningRepository.lagre(
                kontekst.behandlingId,
                samordningTidslinje.segmenter()
                    .map {
                        SamordningPeriode(
                            it.periode,
                            it.verdi.gradering
                        )
                    }.toSet(),
                samordningYtelseVurderingGrunnlag
            )
        } else {
            log.info("Mangler vurdering på perioder, lagrer ingenting i SamordningRepository.")
        }

        return Fullført
    }

    private fun perioderSomIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val samordningYtelseVurderingGrunnlag = samordningService.samordningGrunnlag(behandlingId = kontekst.behandlingId)
        val perioderSomIkkeHarBlittVurdert =
            samordningYtelseVurderingGrunnlag.perioderSomIkkeHarBlittVurdert()
        return perioderSomIkkeHarBlittVurdert.perioder().toSet()
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        if (Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER in kontekst.vurderingsbehovRelevanteForSteg) {
            // FIXME: Stygg hack for å tvinge manuell revurdering
            return Tidslinje(kontekst.rettighetsperiode, true)
        }

        val mottarSykepengerOppgittISøknad = sykepengerOgFerieOppgittISøknadRepository
            .hentHvisEksisterer(kontekst.behandlingId)
            ?.mottarSykepenger == true
        if (mottarSykepengerOppgittISøknad) {
            // Bruker har oppgitt i søknaden at hen mottar sykepenger. Krev vurdering av samordning
            // selv om vi ennå ikke har mottatt vedtak om sykepenger fra registeret.
            return Tidslinje(kontekst.rettighetsperiode, true)
        }

        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val grunnlag = samordningService.samordningGrunnlag(behandlingId = kontekst.behandlingId)
        val ytelser = grunnlag.ytelseGrunnlag?.tidslinjeMedSamordningYtelser().orEmpty()

        // Vi sjekker om det har blitt gjort en manuell vurdering her for å klare å sende tilbake hit
        // hvis f.eks beslutter underkjenner vurderingen.
        val vurderinger = grunnlag.vurderingGrunnlag?.vurderingTidslinje().orEmpty()

        return Tidslinje.map3(
            tidligereVurderingsutfall,
            ytelser,
            vurderinger
        ) { utfall, samordningYtelser, vurdering ->
            when (utfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    !samordningYtelser.isNullOrEmpty() || !vurdering.isNullOrEmpty()
                }

                null -> false
            }
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SamordningSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}
