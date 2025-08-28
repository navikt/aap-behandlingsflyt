package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sykdomRepository: SykdomRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for behandlingId ${kontekst.behandlingId}, avbryter steg ${type()}")
                    avklaringsbehovService.avbrytForSteg(kontekst.behandlingId, type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst,
                        Vilkårtype.BISTANDSVILKÅRET,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }

                vurder(kontekst)
            }

            VurderingType.REVURDERING -> {
                // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
                vurder(kontekst)
            }

            VurderingType.MELDEKORT, VurderingType.AKTIVITETSPLIKT, VurderingType.IKKE_RELEVANT -> {
                log.info("Vurderingtype ${kontekst.vurderingType} ikke relevant for steg ${type()} for behandlingId ${kontekst.behandlingId}, fullfører steg.")
                // Do nothing
                Fullført
            }
        }
    }

    private fun vurder(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Vurderer sykepengeerstatning for behandlingId ${kontekst.behandlingId}")
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger.orEmpty()

        val behandlingsType = kontekst.behandlingType
        val kravDato = kontekst.rettighetsperiode.fom

        val erRelevantÅVurdereSykepengererstatning = sykdomsvurderinger.any {
            it.erOppfyltSettBortIfraVissVarighet() && !it.erOppfylt(
                behandlingsType, kravDato
            ) || (!vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET).harPerioderSomErOppfylt() && it.erOppfylt(
                behandlingsType,
                kravDato
            ))
        }

        log.info("Relevant å vurdere sykepengeerstatning: $erRelevantÅVurdereSykepengererstatning for behandlingId ${kontekst.behandlingId}.")

        if (erRelevantÅVurdereSykepengererstatning) {
            val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

            if (grunnlag?.vurdering != null) {
                val rettighetsperiode = kontekst.rettighetsperiode
                val vurderingsdato = rettighetsperiode.fom
                val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                    vurderingsdato,
                    // TODO: Trenger å finne en god løsning for hvordan vi setter slutt på dette vilkåret ved tom kvote
                    rettighetsperiode.tom, grunnlag.vurdering
                )

                if (grunnlag.vurdering.harRettPå) {
                    // TODO her bør vi finne en bedre løsning på sikt
                    //      Vi bør sentralisere behandling av vilkår til én vurderer-klasse.
                    vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET).leggTilVurdering(
                        Vilkårsperiode(
                            periode = Periode(faktagrunnlag.vurderingsdato, faktagrunnlag.sisteDagMedMuligYtelse),
                            utfall = Utfall.IKKE_RELEVANT,
                            begrunnelse = null,
                            faktagrunnlag = faktagrunnlag,
                            versjon = ApplikasjonsVersjon.versjon
                        )
                    )
                }
                log.info("Merket bistand som ikke relevant pga innvilget sykepengevilkår.")
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            } else {
                return FantAvklaringsbehov(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
        } else {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val sykepengeerstatningsBehov =
                avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)

            if (sykepengeerstatningsBehov?.erÅpent() == true) {
                avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSykepengeErstatningSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_SYKEPENGEERSTATNING
        }
    }
}
