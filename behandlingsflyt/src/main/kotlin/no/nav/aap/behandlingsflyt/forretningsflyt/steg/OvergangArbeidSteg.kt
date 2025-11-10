package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
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
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val studentRepository: StudentRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        bistandRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        studentRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.OvergangArbeid)) {
            return Fullført
        }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                Vurderingsbehov.HELHETLIG_VURDERING,
            ),
            nårVurderingErRelevant = ::perioderVurderingErRelevant,
            kontekst = kontekst,
            erTilstrekkeligVurdert = { true },
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

    private fun perioderVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje(kontekst.rettighetsperiode.fom)
            .orEmpty()

        val bistandsvurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somBistandsvurderingstidslinje(kontekst.rettighetsperiode.fom)
            .orEmpty()

        val studentVurderinger = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somTidslinje(kontekst.rettighetsperiode)
            .orEmpty()

        val overgangUføreVurderinger = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somOvergangUforevurderingstidslinje(kontekst.rettighetsperiode.fom)
            .orEmpty()

        val forutgåendeOrdinærAap =
            Tidslinje.map2(sykdomsvurderinger, bistandsvurderinger) { sykdomsvurdering, bistandsvurdering ->
                sykdomsvurdering?.erOppfylt(kontekst.rettighetsperiode.fom) == true &&
                        bistandsvurdering?.erBehovForBistand() == true
            }
                .fold(
                    Tidslinje(
                        kontekst.rettighetsperiode,
                        false
                    )
                ) { forutgåendeOrdinærAap, periode, rettTilOrdinærAap ->
                    forutgåendeOrdinærAap.or(
                        tidslinjeOf(
                            Periode(
                                periode.fom.plusDays(1),
                                Tid.MAKS
                            ) to rettTilOrdinærAap
                        )
                    )
                }

        return Tidslinje.map6(
            utfall,
            sykdomsvurderinger,
            bistandsvurderinger,
            studentVurderinger,
            overgangUføreVurderinger,
            forutgåendeOrdinærAap,
        ) { utfall, sykdomsvurdering, bistandsvurdering, studentvurdering, uførevurdering, forutgåendeOrdinærAap ->
            when (utfall) {
                null -> false
                TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                    if (studentvurdering?.erOppfylt() == true) {
                        return@map6 false
                    }

                    if (uførevurdering?.harRettPåAAPMedOvergangUføre() == true) {
                        return@map6 false
                    }

                    if (forutgåendeOrdinærAap != true) {
                        return@map6 false
                    }

                    sykdomsvurdering?.erOppfylt(kontekst.rettighetsperiode.fom) != true ||
                            bistandsvurdering?.erBehovForBistand() != true
                }
            }
        }
    }

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

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
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