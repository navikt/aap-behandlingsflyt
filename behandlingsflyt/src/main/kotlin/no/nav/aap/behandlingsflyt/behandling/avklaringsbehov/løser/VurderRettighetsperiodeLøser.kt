package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.DatoFraDokumentUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class VurderRettighetsperiodeLøser(
    private val unleashGateway: UnleashGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val mottattDokumentRepository: MottattDokumentRepository,
) : AvklaringsbehovsLøser<VurderRettighetsperiodeLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        unleashGateway = gatewayProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        rettighetsperiodeRepository = repositoryProvider.provide(),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderRettighetsperiodeLøsning): LøsningsResultat {

        if (!unleashGateway.isEnabled(BehandlingsflytFeature.OverstyrStarttidspunkt, kontekst.bruker.ident)) {
            throw UgyldigForespørselException("Funksjonsbryter for overstyr starttidspunkt er skrudd av")
        }
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        if (behandling.status().erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at behandlingen er avsluttet")
        }
        if (sak.status() == Status.AVSLUTTET) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at saken er avsluttet")
        }
        if(innskrenkerTilEtterSøknadstidspunkt(løsning.rettighetsperiodeVurdering.startDato, sak.id)) {
            throw UgyldigForespørselException("Kan ikke endre starttidspunkt til å gjelde ETTER søknadstidspunkt")
        }

        rettighetsperiodeRepository.lagreVurdering(
            behandlingId = behandling.id,
            vurdering =
                RettighetsperiodeVurdering(
                    begrunnelse = løsning.rettighetsperiodeVurdering.begrunnelse,
                    startDato = løsning.rettighetsperiodeVurdering.startDato,
                    harRettUtoverSøknadsdato = løsning.rettighetsperiodeVurdering.harRettUtoverSøknadsdato,
                    harKravPåRenter = løsning.rettighetsperiodeVurdering.harKravPåRenter,
                    vurdertAv = kontekst.bruker.ident
                )
        )

        if (løsning.rettighetsperiodeVurdering.harRettUtoverSøknadsdato && løsning.rettighetsperiodeVurdering.startDato != null) {
            val sluttDato = utledNySluttdato(behandling, løsning.rettighetsperiodeVurdering.startDato, sak)

            sakOgBehandlingService.overstyrRettighetsperioden(
                sakId = sak.id,
                startDato = løsning.rettighetsperiodeVurdering.startDato,
                sluttDato = sluttDato
            )
        }

        return LøsningsResultat("Vurdert rettighetsperiode")
    }

    private fun innskrenkerTilEtterSøknadstidspunkt(startDato: LocalDate?, sakId: SakId): Boolean {
        val søknadsdato = DatoFraDokumentUtleder(mottattDokumentRepository).utledSøknadsdatoForSak(sakId)?.toLocalDate()
        return startDato != null && søknadsdato != null && startDato.isAfter(søknadsdato)
    }

    private fun utledNySluttdato(
        behandling: Behandling,
        startDato: LocalDate,
        sak: Sak
    ): LocalDate = if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
        startDato.plusYears(1).minusDays(1)
    } else {
        sak.rettighetsperiode.tom
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_RETTIGHETSPERIODE
    }
}