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
import no.nav.aap.lookup.repository.RepositoryProvider

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

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {


        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER,
                Vurderingsbehov.FERIE_I_SYKEPENGEPERIODE
            ),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { emptySet() },
            tilbakestillGrunnlag = {
                samordningService.tilbakestillVurderinger(kontekst.behandlingId, kontekst.forrigeBehandlingId)
            }
        )

        val samordningYtelseVurderingGrunnlag = samordningService.samordningGrunnlag(behandlingId = kontekst.behandlingId)
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

        return Fullført
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val skalRevurdereSamordning = Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER in kontekst.vurderingsbehovRelevanteForSteg

        val mottarSykepengerOppgittISøknad = sykepengerOgFerieOppgittISøknadRepository
            .hentHvisEksisterer(kontekst.behandlingId)
            ?.mottarSykepenger == true

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
                    // Bruker kan ha oppgitt i søknaden at hen mottar sykepenger. Krev da vurdering
                    // av samordning selv om vi ennå ikke har mottatt vedtak om sykepenger fra registeret.
                    skalRevurdereSamordning || mottarSykepengerOppgittISøknad || !samordningYtelser.isNullOrEmpty() || !vurdering.isNullOrEmpty()
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
