package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.DatoFraDokumentUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VurderRettighetsperiodeLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val mottattDokumentRepository: MottattDokumentRepository,
) : AvklaringsbehovsLøser<VurderRettighetsperiodeLøsning> {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        rettighetsperiodeRepository = repositoryProvider.provide(),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderRettighetsperiodeLøsning): LøsningsResultat {

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        if (behandling.status().erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at behandlingen er avsluttet")
        }
        if (sak.status() == Status.AVSLUTTET) {
            throw UgyldigForespørselException("Kan ikke oppdatere rettighetsperioden etter at saken er avsluttet")
        }
        val nyStartDato = løsning.rettighetsperiodeVurdering.startDato
        if (innskrenkerTilEtterSøknadstidspunkt(nyStartDato, sak.id)) {
            throw UgyldigForespørselException("Kan ikke endre starttidspunkt til å gjelde ETTER søknadstidspunkt")
        }

        rettighetsperiodeRepository.lagreVurdering(
            behandlingId = behandling.id,
            vurdering =
                RettighetsperiodeVurdering(
                    begrunnelse = løsning.rettighetsperiodeVurdering.begrunnelse,
                    startDato = nyStartDato,
                    harRettUtoverSøknadsdato = løsning.rettighetsperiodeVurdering.harRett,
                    vurdertAv = kontekst.bruker.ident
                )
        )

        if (løsning.rettighetsperiodeVurdering.harRett.toBoolean() && nyStartDato != null) {
            log.info("Oppdaterer rettighetsperioden til å gjelde fra $ for sak ${sak.id}")
            sakOgBehandlingService.overstyrRettighetsperioden(
                sakId = sak.id,
                startDato = nyStartDato,
                sluttDato = Tid.MAKS
            )
        } else if (!løsning.rettighetsperiodeVurdering.harRett.toBoolean()) {
            val søknadsdato = finnSøknadsdatoForSak(sak.id)
                ?: throw UgyldigForespørselException("Forsøker å tilbakestille rettighetsperioden, men finner ingen søknadsdato for saken")
            if (sak.rettighetsperiode.fom != søknadsdato) {
                log.info("Tilbakestiller rettighetsperioden til å gjelde fra søknadsdato $søknadsdato for sak ${sak.id}")
                sakOgBehandlingService.overstyrRettighetsperioden(
                    sakId = sak.id,
                    startDato = søknadsdato,
                    sluttDato = Tid.MAKS
                )
            }
        }

        return LøsningsResultat("Vurdert rettighetsperiode")
    }

    private fun innskrenkerTilEtterSøknadstidspunkt(startDato: LocalDate?, sakId: SakId): Boolean {
        val søknadsdato = finnSøknadsdatoForSak(sakId)
        return startDato != null && søknadsdato != null && startDato.isAfter(søknadsdato)
    }

    private fun finnSøknadsdatoForSak(sakId: SakId): LocalDate? =
        DatoFraDokumentUtleder(mottattDokumentRepository).utledSøknadsdatoForSak(sakId)?.toLocalDate()

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_RETTIGHETSPERIODE
    }
}