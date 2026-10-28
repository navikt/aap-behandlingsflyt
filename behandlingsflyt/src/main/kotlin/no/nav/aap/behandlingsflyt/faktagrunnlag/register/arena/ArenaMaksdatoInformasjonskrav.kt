package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.arena.tilDomene
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdagForBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

/** Slår opp maksdato fra Arena når §12-steget passeres, maks én gang per kalenderdag per behandling. */
class ArenaMaksdatoInformasjonskrav(
    private val sakRepository: SakRepository,
    private val arenaMaksdatoRepository: ArenaMaksdatoRepository,
    private val arenaOppslagGateway: ArenaOppslagGateway,
    private val unleashGateway: UnleashGateway,
) : Informasjonskrav<ArenaMaksdatoInformasjonskrav.Input, ArenaMaksdatoInformasjonskrav.Registerdata> {
    data class Input(val ident: Ident) : InformasjonskravInput

    data class Registerdata(val arenaMaksdato: ArenaMaksdato?) : InformasjonskravRegisterdata

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?,
    ): Boolean = unleashGateway.isEnabled(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)
            && kontekst.erFørstegangsbehandlingEllerRevurdering()
            && oppdatert.ikkeKjørtSisteKalenderdagForBehandling(kontekst.behandlingId)

    override fun klargjør(kontekst: FlytKontekstMedPerioder): Input {
        val sak = sakRepository.hent(kontekst.sakId)
        return Input(sak.person.aktivIdent())
    }

    override fun hentData(input: Input): Registerdata =
        Registerdata(arenaOppslagGateway.hentMaksdato(input.ident).tilDomene())

    override fun oppdater(
        input: Input,
        registerdata: Registerdata,
        kontekst: FlytKontekstMedPerioder,
    ): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val eksisterende = arenaMaksdatoRepository.hentHvisEksisterer(behandlingId)
        if (eksisterende != null && eksisterende.arenaMaksdato == registerdata.arenaMaksdato) {
            return Informasjonskrav.Endret.IKKE_ENDRET
        }

        arenaMaksdatoRepository.lagre(behandlingId, registerdata.arenaMaksdato)
        return Informasjonskrav.Endret.ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.ARENA_MAKSDATO

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ): Informasjonskrav<Input, Registerdata> = ArenaMaksdatoInformasjonskrav(
            sakRepository = repositoryProvider.provide(),
            arenaMaksdatoRepository = repositoryProvider.provide(),
            arenaOppslagGateway = gatewayProvider.provide(),
            unleashGateway = gatewayProvider.provide(),
        )
    }
}
