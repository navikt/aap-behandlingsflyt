package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.dokument.KlagedokumentInformasjonUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.MeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class StatistikkMetoder(
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val pipService: PipService,
    private val dokumentRepository: MottattDokumentRepository,
    private val meldekortRepository: MeldekortRepository,
    private val påklagetBehandlingRepository: PåklagetBehandlingRepository,
    private val klagedokumentInformasjonUtleder: KlagedokumentInformasjonUtleder,
    private val avsluttetBehandlingTilStatistikk: AvsluttetBehandlingTilStatistikk,
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider.provide(), repositoryProvider.provide()),
        pipService = PipService(repositoryProvider),
        dokumentRepository = repositoryProvider.provide(),
        meldekortRepository = repositoryProvider.provide(),
        påklagetBehandlingRepository = repositoryProvider.provide(),
        klagedokumentInformasjonUtleder = KlagedokumentInformasjonUtleder(repositoryProvider),
        avsluttetBehandlingTilStatistikk = AvsluttetBehandlingTilStatistikk(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun oversettHendelseTilKontrakt(hendelse: BehandlingFlytStoppetHendelseTilStatistikk): StoppetBehandling {
        log.info("Oversetter hendelse for behandling ${hendelse.referanse} og saksnr ${hendelse.saksnummer}")
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sisteEndring = behandlingRepository.hentStegHistorikk(behandling.id).lastOrNull()?.tidspunkt()
        val relevanteDokumenter = hentRelevanteDokumenterForBehandling(behandling)
        val mottattTidspunkt = utledMottattTidspunkt(behandling, relevanteDokumenter)
        val søknadIder = relevanteDokumenter
            .filter { it.type == InnsendingType.SØKNAD }
            .map { it.referanse.asJournalpostId }

        val kanal = hentSøknadsKanal(behandling, relevanteDokumenter)

        val sak = sakService.hent(hendelse.saksnummer)

        val meldekort = meldekortRepository.hentHvisEksisterer(behandling.id)
        val forrigeBehandlingMeldekort =
            behandling.forrigeBehandlingId?.let { meldekortRepository.hentHvisEksisterer(it) }

        val nyeMeldekort =
            meldekort?.meldekort().orEmpty().toSet().minus(forrigeBehandlingMeldekort?.meldekort().orEmpty().toSet())
                .toList()

        val vurderingsbehovForBehandling = utledVurderingsbehovForBehandling(behandling)
        return StoppetBehandling(
            saksnummer = hendelse.saksnummer.toString(),
            behandlingType = hendelse.behandlingType,
            behandlingStatus = hendelse.status,
            ident = hendelse.personIdent,
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingReferanse = hendelse.referanse.referanse,
            relatertBehandling = relatertBehandling(behandling),
            behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
            tidspunktSisteEndring = sisteEndring ?: hendelse.hendelsesTidspunkt,
            soknadsFormat = kanal,
            versjon = hendelse.versjon,
            mottattTid = mottattTidspunkt,
            sakStatus = sak.status(),
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
            avsluttetBehandling = if (hendelse.status == AVSLUTTET) avsluttetBehandlingTilStatistikk.hentAvsluttetBehandlingDTO(
                hendelse
            ) else null,
            identerForSak = hentIdenterPåSak(sak.saksnummer),
            vurderingsbehov = vurderingsbehovForBehandling,
            årsakTilOpprettelse = behandling.årsakTilOpprettelse.tilKontrakt(),
            opprettetAv = hendelse.opprettetAv,
            nyeMeldekort = nyeMeldekort.map { meldekort ->
                MeldekortDTO(
                    meldekort.journalpostId.identifikator,
                    meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(it.periode.fom, it.periode.tom, it.timerArbeid.antallTimer)
                    }
                )
            },
            søknadIder = søknadIder
        )
    }

    private fun relatertBehandling(behandling: Behandling): UUID? {
        return when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> null
            TypeBehandling.Revurdering ->
                behandling.forrigeBehandlingId
                    ?.let { behandlingRepository.hent(it).referanse.referanse }

            TypeBehandling.Tilbakekreving -> TODO()
            TypeBehandling.Klage -> {
                val påklagetBehandling =
                    påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(behandling.referanse)

                check(påklagetBehandling == null || påklagetBehandling.påklagetVedtakType == PåklagetVedtakType.KELVIN_BEHANDLING) {
                    "Hvis det klages på en behandling utenfor Kelvin, må dette være synlig i statistikk med referanse til eksternt system."
                }

                påklagetBehandling?.referanse?.referanse
            }

            TypeBehandling.SvarFraAndreinstans -> {
                klagedokumentInformasjonUtleder.utledKlagebehandlingForSvar(behandling.id).referanse
            }

            TypeBehandling.OppfølgingsBehandling -> null
            TypeBehandling.Aktivitetsplikt -> null
            TypeBehandling.Aktivitetsplikt11_9 -> null
        }
    }

    private fun utledVurderingsbehovForBehandling(behandling: Behandling): List<Vurderingsbehov> =
        behandling.vurderingsbehov().map { it.tilKontraktVurderingsbehov() }.distinct()

    private fun hentIdenterPåSak(saksnummer: Saksnummer): List<String> {
        return pipService.finnIdenterPåSak(saksnummer).map { it.ident }
    }

    private fun hentSøknadsKanal(behandling: Behandling, hentDokumenterAvType: Set<MottattDokument>): Kanal {
        val kanaler = hentDokumenterAvType.filter { it.behandlingId == behandling.id }.map { it.kanal }

        // Om minst én av søknadene er papir, regn med at hele behandlingen er papir
        return kanaler.reduceOrNull { acc, curr ->
            when (acc) {
                Kanal.DIGITAL -> curr
                Kanal.PAPIR -> Kanal.PAPIR
            }
        } ?: Kanal.DIGITAL
    }

    private fun utledMottattTidspunkt(
        behandling: Behandling, mottatteDokumenter: Set<MottattDokument>
    ): LocalDateTime {
        val mottattTidspunkt =
            mottatteDokumenter.filter { it.behandlingId == behandling.id }
                .minByOrNull { it.opprettetTid }?.mottattTidspunkt

        if (mottattTidspunkt == null) {
            log.info("Ingen søknader funnet for behandling ${behandling.referanse} av type ${behandling.typeBehandling()}.")
            return behandling.opprettetTidspunkt
        }
        return minOf(mottattTidspunkt, behandling.opprettetTidspunkt)
    }

    private fun hentRelevanteDokumenterForBehandling(behandling: Behandling): Set<MottattDokument> {
        val hentDokumenterAvType = dokumentRepository.hentDokumenterAvType(
            behandling.id,
            InnsendingType.entries
        )
        return hentDokumenterAvType
    }
}
