package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class SamordningSteg(
    private val samordningVurderingRepository: SamordningVurderingRepository,
    private val samordningYtelseRepository: SamordningYtelseRepository,
    private val samordningRepository: SamordningRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningVurderingRepository = repositoryProvider.provide(),
        samordningYtelseRepository = repositoryProvider.provide(),
        samordningRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurderinger = samordningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        val ytelser = samordningYtelseRepository.hentHvisEksisterer(kontekst.behandlingId)
        val grunnlag = SamordningYtelseVurderingGrunnlag(ytelser, vurderinger)
        val perioderSomIkkeHarBlittVurdert = grunnlag.perioderSomIkkeHarBlittVurdert()

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SAMORDNING_OG_AVREGNING,
                Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER
            ),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeHarBlittVurdert.perioder().toSet() },
            tilbakestillGrunnlag = { tilbakestillVurderinger(kontekst.behandlingId, kontekst.forrigeBehandlingId) }
        )

        if (perioderSomIkkeHarBlittVurdert.isEmpty()) {
            val samordningTidslinje = SamordningYtelseVurderingGrunnlag(ytelser, vurderinger).tilTidslinje()

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
        if (Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER in kontekst.vurderingsbehovRelevanteForSteg) {
            // FIXME: Stygg hack for å tvinge manuell revurdering
            return Tidslinje(kontekst.rettighetsperiode, true)
        }

        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val samordningYtelseGrunnlag =
            samordningYtelseRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
        val samordningsvurderinger = samordningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        val grunnlag = SamordningYtelseVurderingGrunnlag(samordningYtelseGrunnlag, samordningsvurderinger)
        val ytelser = grunnlag.tidslinjeMedSamordningYtelser()

        // Vi sjekker om det har blitt gjort en manuell vurdering her for å klare å sende tilbake hit
        // hvis f.eks beslutter underkjenner vurderingen.
        val vurderingtidslinje = samordningsvurderinger?.tilTidslinje().orEmpty()

        return Tidslinje.map3(
            tidligereVurderingsutfall,
            ytelser,
            vurderingtidslinje
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

    fun tilbakestillVurderinger(behandlingId: BehandlingId, forrigeBehandlingId: BehandlingId?) {
        val vurderinger = samordningVurderingRepository.hentHvisEksisterer(behandlingId)
        val forrigeVurderinger =
            forrigeBehandlingId?.let { samordningVurderingRepository.hentHvisEksisterer(it) }

        if (forrigeVurderinger != vurderinger) {
            if (forrigeBehandlingId == null || forrigeVurderinger == null) {
                // Er ingen forrige behandlingId, så vi deaktiverer det eksisterende grunnlaget.
                samordningVurderingRepository.deaktiverGrunnlag(behandlingId)
            } else {
                samordningVurderingRepository.lagreVurderinger(
                    behandlingId, forrigeVurderinger
                )
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
