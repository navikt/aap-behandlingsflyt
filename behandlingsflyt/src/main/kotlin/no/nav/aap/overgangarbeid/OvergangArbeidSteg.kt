package no.nav.aap.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
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
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider

class OvergangArbeidSteg internal constructor(
    private val vilkårService: VilkårService,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val sykdomRepository: SykdomRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val uføreRepository: UføreRepository
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårService = VilkårService(repositoryProvider),
        overgangArbeidRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        uføreRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                Vurderingsbehov.HELHETLIG_VURDERING,
                Vurderingsbehov.OVERGANG_ARBEID,
            ),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            kontekst = kontekst,
            nårVurderingErGyldig = {
                overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId)?.gjeldendeVurderinger().orEmpty()
                    .mapValue { true }
            },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst) },
        )

        val grunnlag = OvergangArbeidFaktagrunnlag(
            rettighetsperiode = kontekst.rettighetsperiode,
            overgangArbeidGrunnlag = overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId),
        )
        vilkårService.vurderVilkår(kontekst.behandlingId, grunnlag, OvergangArbeidVilkår)

        return Fullført
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje()
            .orEmpty()

        val uføreTidslinje =
            uføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger?.tilTidslinje().orEmpty()

        val forutgåendeRettBistandsbehov =
            utfall.mapValue { it is TidligereVurderinger.PotensieltOppfylt && it.rettighetstype == RettighetsType.BISTANDSBEHOV }
                .fold(
                    Tidslinje(
                        kontekst.rettighetsperiode,
                        false
                    )
                ) { rettITidligerePeriode, periode, rettIDennePerioden ->
                    rettITidligerePeriode.or(
                        tidslinjeOf(
                            Periode(
                                periode.fom.plusDays(1),
                                Tid.MAKS
                            ) to rettIDennePerioden
                        )
                    )
                }

        return Tidslinje.map4(
            utfall,
            sykdomsvurderinger,
            uføreTidslinje,
            forutgåendeRettBistandsbehov,
        ) { utfall, sykdomsvurdering, uføregrad, forutgåendeOrdinærAap ->
            when (utfall) {
                null -> false
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                is TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    when (utfall.rettighetstype) {
                        null -> {
                            if (forutgåendeOrdinærAap != true) {
                                return@map4 false
                            }

                            sykdomsvurdering?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() != true
                                    || erDelvisUfør(uføregrad)
                        }

                        else -> false
                    }
                }
            }
        }
    }

    private fun erDelvisUfør(uføregrad: Prosent?) =
        uføregrad != null && uføregrad > Prosent(0) && uføregrad < Prosent(100)

    private fun tilbakestillGrunnlag(kontekst: FlytKontekstMedPerioder) {
        val vedtatteVurderinger = kontekst.forrigeBehandlingId
            ?.let { overgangArbeidRepository.hentHvisEksisterer(it)?.vurderinger }
            .orEmpty()

        val gjeldendeVurderinger = overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.vurderinger
            .orEmpty()
        if (gjeldendeVurderinger.toSet() != vedtatteVurderinger.toSet()) {
            overgangArbeidRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
        }
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): OvergangArbeidSteg {
            return OvergangArbeidSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_ARBEID
        }
    }
}

fun Tidslinje<Boolean>.or(other: Tidslinje<Boolean>): Tidslinje<Boolean> {
    return this.outerJoin(other) { thisBool, otherBool ->
        (thisBool ?: false) || (otherBool ?: false)
    }
}
