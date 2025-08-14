package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.OVERGANGUFØREVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                    avklaringsbehovene.avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.OVERGANGUFØREVILKÅRET,
                        kontekst.rettighetsperiode,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                } else {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_OVERGANG_UFORE)
                }
            }

            VurderingType.REVURDERING -> {
                return FantAvklaringsbehov(Definisjon.AVKLAR_OVERGANG_UFORE)
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                // Skal ikke gjøre noe
            }
        }
        if (kontekst.harNoeTilBehandling()) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        postcondition(vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET))

        return Fullført
    }

    private fun postcondition(vilkår: Vilkår) {
        if (vilkår.harPerioderSomIkkeErVurdert(vilkår.vilkårsperioder().map { it.periode }.toSet())) {
            throw IllegalStateException("Det finnes perioder som ikke er vurdert")
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OvergangUføreSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_UFORE
        }
    }
}
