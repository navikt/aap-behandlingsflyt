package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid.OvergangArbeidVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class OvergangArbeidSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val sykdomRepository: SykdomRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val bistandRepository: BistandRepository,
    private val behandlingRepository: BehandlingRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        bistandRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.MOTTATT_SØKNAD,
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                Vurderingsbehov.HELHETLIG_VURDERING,
            ),
            nårVurderingErRelevant = ::perioderVurderingErRelevant,
            kontekst = kontekst,
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId?.let {
                    overgangArbeidRepository.hentHvisEksisterer(it)?.vurderinger
                }.orEmpty()

                val gjeldendeVurderinger = overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.vurderinger
                    .orEmpty()
                if (gjeldendeVurderinger.toSet() != vedtatteVurderinger.toSet()) {
                    overgangArbeidRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                }
            },
        )

        val avklarOvergangArbeid = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_ARBEID)
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        if (avklarOvergangArbeid?.status() in listOf(Status.AVSLUTTET, Status.AVBRUTT)) {
            val grunnlag = OvergangArbeidFaktagrunnlag(
                kontekst.rettighetsperiode.fom,
                kontekst.rettighetsperiode.tom,
                overgangArbeidRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty(),
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
            ?: tidslinjeOf()
        val bistandsvurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somBistandsvurderingstidslinje(kontekst.rettighetsperiode.fom)
            ?: tidslinjeOf()

        return Tidslinje.zip3(utfall, sykdomsvurderinger, bistandsvurderinger)
            .mapValue { (utfall, sykdomsvurdering, bistandsvurdering) ->
                when (utfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {

                        /* Her tror jeg det er naturlig å se etter student også? Og kanskje overgang uføre? Noe annet? */
                        (sykdomsvurdering?.harSkadeSykdomEllerLyte == false ||
                                sykdomsvurdering?.erArbeidsevnenNedsatt == false ||
                                sykdomsvurdering?.erSkadeSykdomEllerLyteVesentligdel == false ||
                                bistandsvurdering?.erBehovForBistand() != true) && bistandsvurdering?.skalVurdereAapIOvergangTilArbeid == true
                    }
                }
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