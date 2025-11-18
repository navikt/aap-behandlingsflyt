package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class SamordningSteg(
    private val samordningService: SamordningService,
    private val samordningRepository: SamordningRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningService = SamordningService(repositoryProvider),
        samordningRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurderinger = samordningService.hentVurderinger(behandlingId = kontekst.behandlingId)
        val ytelser = samordningService.hentYtelser(behandlingId = kontekst.behandlingId)
        val tidligereVurderinger = samordningService.vurderingTidslinje(vurderinger)

        val perioderSomIkkeHarBlittVurdert = samordningService.perioderSomIkkeHarBlittVurdert(
            ytelser, tidligereVurderinger
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER
            ),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeHarBlittVurdert.perioder().toSet() },
            tilbakestillGrunnlag = {
                samordningService.tilbakestillVurderinger(kontekst.behandlingId, kontekst.forrigeBehandlingId)
            }
        )

        if (perioderSomIkkeHarBlittVurdert.isEmpty()) {
            val samordningTidslinje =
                samordningService.vurder(ytelser, tidligereVurderinger)

            samordningRepository.lagre(
                kontekst.behandlingId,
                samordningTidslinje.segmenter()
                    .map {
                        SamordningPeriode(
                            it.periode,
                            it.verdi.gradering
                        )
                    }.toSet(),
                SamordningYtelseVurderingGrunnlag(ytelser, vurderinger)
            )
        } else {
            log.info("Mangler vurdering på perioder, lagrer ingenting i SamordningRepository.")
        }

        return Fullført
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val grunnlag = samordningService.hentYtelser(behandlingId = kontekst.behandlingId)
        val ytelser = samordningService.tidslinjeMedSamordningYtelser(grunnlag)

        // Vi sjekker om det har blitt gjort en manuell vurdering her for å klare å sende tilbake hit
        // hvis f.eks beslutter underkjenner vurderingen.
        val vurderinger = samordningService.hentVurderinger(behandlingId = kontekst.behandlingId)
        val vurderingtidslinje = samordningService.vurderingTidslinje(vurderinger)

        return Tidslinje.map3(
            tidligereVurderingsutfall,
            ytelser,
            vurderingtidslinje
        ) { utfall, samordningYtelser, vurdering ->
            when (utfall) {
                TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                TidligereVurderinger.Behandlingsutfall.UKJENT -> {
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
            return SamordningSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}
