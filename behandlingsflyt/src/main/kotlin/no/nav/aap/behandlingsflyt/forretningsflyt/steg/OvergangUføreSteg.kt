package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
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

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val sykdomRepository: SykdomRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val bistandRepository: BistandRepository,
    private val behandlingRepository: BehandlingRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
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
            definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD, Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { overgangUføreRepository.hentHvisEksisterer(it) }
                    ?.vurderinger.orEmpty()
                val aktiveVurderinger = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.vurderinger.orEmpty()
                if (vedtatteVurderinger.toSet() != aktiveVurderinger.toSet()) {
                    overgangUføreRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                }
            },
        )

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_UFORE)
                ?.status() in listOf(Status.AVSLUTTET, Status.AVBRUTT)
        ) {
            val grunnlag = OvergangUføreFaktagrunnlag(
                rettighetsperiode = kontekst.rettighetsperiode,
                vurderinger = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty(),
            )
            OvergangUføreVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
        } else {
            vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
        }
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        return Fullført
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje(kontekst.rettighetsperiode.fom) ?: tidslinjeOf()
        val bistandsvurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somBistandsvurderingstidslinje(startDato = kontekst.rettighetsperiode.fom) ?: tidslinjeOf()

        return Tidslinje.zip3(utfall, sykdomsvurderinger, bistandsvurderinger)
            .mapValue { (utfall, sykdomsvurdering, bistandsvurdering) ->
                when (utfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        sykdomsvurdering?.erOppfylt(kontekst.rettighetsperiode.fom) == true && bistandsvurdering != null && !bistandsvurdering.erBehovForBistand()
                    }
                }
            }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OvergangUføreSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_UFORE
        }
    }
}
