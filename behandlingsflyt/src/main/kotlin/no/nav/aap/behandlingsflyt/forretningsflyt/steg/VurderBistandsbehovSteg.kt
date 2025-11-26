package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VurderBistandsbehovSteg(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val sykdomsRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        bistandRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        sykdomsRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            definisjon = Definisjon.AVKLAR_BISTANDSBEHOV,
            tvingerAvklaringsbehov = kontekst.vurderingsbehovRelevanteForSteg,
            nårVurderingErRelevant = { perioderHvorBistandsvilkåretErRelevant(kontekst) },
            nårVurderingErGyldig = {
                bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.somBistandsvurderingstidslinje()
                    .orEmpty()
                    .mapValue { true } // Alle vurderinger er gyldige hvis de finnes
            },
            tilbakestillGrunnlag = {
                val forrigeVurderinger = kontekst.forrigeBehandlingId
                    ?.let { bistandRepository.hentHvisEksisterer(it) }
                    ?.vurderinger
                    .orEmpty()

                val nåværendeVurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.vurderinger
                    .orEmpty()

                if (forrigeVurderinger.toSet() != nåværendeVurderinger.toSet()) {
                    bistandRepository.lagre(kontekst.behandlingId, forrigeVurderinger)
                }

                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                val nyttVilkår = vilkårsresultat.optionalVilkår(Vilkårtype.BISTANDSVILKÅRET)

                if (nyttVilkår != null) {
                    val forrigeVilkårTidslinje =
                        kontekst.forrigeBehandlingId?.let { vilkårsresultatRepository.hent(it) }
                            ?.optionalVilkår(Vilkårtype.BISTANDSVILKÅRET)
                            ?.tidslinje()
                            .orEmpty()

                    if (nyttVilkår.tidslinje() != forrigeVilkårTidslinje) {
                        nyttVilkår.nullstillTidslinje()
                            .leggTilVurderinger(forrigeVilkårTidslinje)

                        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
                    }
                } else {
                    log.info("Vilkår for bistandsbehov finnes ikke i vilkårsresultat for behandling ${kontekst.behandlingId}, ingen tilbakestilling utført.")
                }
            },
            kontekst = kontekst
        )

        if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)?.status()
                ?.erAvsluttet() == true
        ) {
            /* Dette skal på sikt ut av denne metoden, og samles i et eget fastsett-steg. */
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)

            val grunnlag = BistandFaktagrunnlag(
                kontekst.rettighetsperiode.tom,
                bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            )
            Bistandsvilkåret(vilkårsresultat).vurder(grunnlag = grunnlag)
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        return Fullført
    }

    private fun perioderHvorBistandsvilkåretErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val sykdomsvurderinger = sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje()
            ?.begrensetTil(kontekst.rettighetsperiode)
            .orEmpty()

        val studentvurderinger = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somTidslinje(kontekst.rettighetsperiode)
            .orEmpty()

        return Tidslinje.zip3(tidligereVurderingsutfall, sykdomsvurderinger, studentvurderinger)
            .mapValue { (behandlingsutfall, sykdomsvurdering, studentvurdering) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        studentvurdering?.erOppfylt() != true &&
                                sykdomsvurdering?.erOppfyltOrdinær(kravdato = kontekst.rettighetsperiode.fom) == true
                    }
                }
            }
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderBistandsbehovSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
