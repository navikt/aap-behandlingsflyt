package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveRequest
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.Prioritet
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Omgjøres
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraBeslutter
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val trekkKlageService: TrekkKlageService,
    private val gosysService: GosysService,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        gosysService = GosysService(gatewayProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type()) || trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
            if (klageresultat is Omgjøres) {
                avklaringsbehov.avbrytForSteg(type())
                return Fullført
            }
        }

        if (avklaringsbehov.skalTilbakeføresEtterTotrinnsVurdering()) {
            return TilbakeføresFraBeslutter
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomHarKrevdToTrinn()) {
            return FantAvklaringsbehov(Definisjon.FATTE_VEDTAK)
        }

        val oppgaveRequest = OpprettOppgaveRequest(
            oppgavetype = "FATTE",
            tema = "AAP",
            prioritet = Prioritet.NORM,
            aktivDato = LocalDate.now().toString(),
            personident = null,
            orgnr = null,
            tildeltEnhetsnr = null,
            opprettetAvEnhetsnr = null,
            journalpostId = "1",
            behandlesAvApplikasjon = null,
            tilordnetRessurs = null,
            beskrivelse = null,
            behandlingstema = "AAP",
            behandlingstype = "AAP",
            fristFerdigstillelse = LocalDate.now()
        )
        gosysService.opprettOppgaveHvisIkkeEksisterer(oppgaveRequest, kontekst.behandlingId.toString(), kontekst.behandlingId)
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return FatteVedtakSteg(repositoryProvider, GatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}
