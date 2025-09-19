package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class PersonopplysningInformasjonskrav private constructor(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val personopplysningRepository: PersonopplysningRepository,
    private val personopplysningGateway: PersonopplysningGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav, KanTriggeRevurdering {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSisteKalenderdag() &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val personopplysninger = hentPersonopplysninger(kontekst.behandlingId)
        val eksisterendeData =
            personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(kontekst.behandlingId)

        if (personopplysninger != eksisterendeData) {
            personopplysningRepository.lagre(kontekst.behandlingId, personopplysninger)
            return ENDRET
        }
        return IKKE_ENDRET
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val eksisterendeData =
            personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandlingId)
        val personopplysninger = hentPersonopplysninger(behandlingId)
        return if (personopplysninger != eksisterendeData) {
            listOf(
                VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_LOVVALG)
            )
        } else {
            emptyList()
        }
    }

    private fun hentPersonopplysninger(behandlingId: BehandlingId): Personopplysning {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        return personopplysningGateway.innhent(sak.person)
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.PERSONOPPLYSNING

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): PersonopplysningInformasjonskrav {
            val personopplysningRepository =
                repositoryProvider.provide<PersonopplysningRepository>()
            return PersonopplysningInformasjonskrav(
                SakOgBehandlingService(repositoryProvider, gatewayProvider),
                personopplysningRepository,
                gatewayProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
