package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider

class OvergangArbeidSteg internal constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val sykdomRepository: SykdomRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val bistandRepository: BistandRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        bistandRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

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

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklarOvergangArbeid = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_ARBEID)
        if (avklarOvergangArbeid?.status() == Status.AVSLUTTET) {
            val grunnlag = OvergangArbeidFaktagrunnlag(
                rettighetsperiode = kontekst.rettighetsperiode,
                overgangArbeidGrunnlag = requireNotNull(overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId)) {
                    "Grunnlag må eksistere når avklaringsbehov har status AVSLUTTET"
                },
            )
            OvergangArbeidVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
        } else {
            vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGARBEIDVILKÅRET)
        }
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        return Fullført
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje()
            .orEmpty()

        val bistandsvurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somBistandsvurderingstidslinje()
            .orEmpty()

        val forutgåendeOrdinærAapEllerMedYrkesskade =
            Tidslinje.map2(sykdomsvurderinger, bistandsvurderinger) { sykdomsvurdering, bistandsvurdering ->
                sykdomsvurdering?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter() == true &&
                        bistandsvurdering?.erBehovForBistand() == true
            }
                .fold(
                    Tidslinje(
                        kontekst.rettighetsperiode,
                        false
                    )
                ) { forutgåendeOrdinærAapEllerMedYrkesskade, periode, rettTilOrdinærAapEllerMedYrkesskade ->
                    forutgåendeOrdinærAapEllerMedYrkesskade.or(
                        tidslinjeOf(
                            Periode(
                                periode.fom.plusDays(1),
                                Tid.MAKS
                            ) to rettTilOrdinærAapEllerMedYrkesskade
                        )
                    )
                }

        return Tidslinje.map4(
            utfall,
            sykdomsvurderinger,
            bistandsvurderinger,
            forutgåendeOrdinærAapEllerMedYrkesskade,
        ) { utfall, sykdomsvurdering, bistandsvurdering, forutgåendeOrdinærAap ->
            when (utfall) {
                null -> false
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    when (utfall.rettighetstype) {
                        null -> {
                            if (forutgåendeOrdinærAap != true) {
                                return@map4 false
                            }

                            sykdomsvurdering?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter() != true ||
                                    erDelvisUfør(bistandsvurdering)
                        }

                        else -> false
                    }
                }
            }
        }
    }

    private fun erDelvisUfør(bistandsvurdering: Bistandsvurdering?) =
        // TODO: Finn ut om bruker er delvis ufør. Bruker bistand nei i mellomtiden (løfter behovet for ofte)
        bistandsvurdering?.erBehovForBistand() != true

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

        val forrigeVilkårsvurderinger =
            kontekst.forrigeBehandlingId
                ?.let { vilkårsresultatRepository.hent(it).optionalVilkår(Vilkårtype.OVERGANGARBEIDVILKÅRET) }
                ?.tidslinje()
                .orEmpty()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.OVERGANGARBEIDVILKÅRET)
        if (vilkår != null) {
            vilkår.nullstillTidslinje()
            vilkår.leggTilVurderinger(forrigeVilkårsvurderinger)
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
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