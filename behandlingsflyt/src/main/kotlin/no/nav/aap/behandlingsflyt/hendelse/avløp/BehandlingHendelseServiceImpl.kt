package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattDokumentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.prosessering.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.DatadelingMeldekortJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.MeldeperiodeTilMeldekortBackendJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.BehandlingFlytStoppetHendelseTilStatistikk
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakService: SakService,
    private val dokumentRepository: MottattDokumentRepository,
    private val pipRepository: PipRepository,
    private val meldekortRepository: MeldekortRepository
) : BehandlingHendelseService {
    constructor(repositoryProvider: RepositoryProvider) : this(
        flytJobbRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider),
        dokumentRepository = repositoryProvider.provide(),
        pipRepository = repositoryProvider.provide(),
        meldekortRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val sak = sakService.hent(behandling.sakId)
        val erPåVent = avklaringsbehovene.hentÅpneVentebehov().isNotEmpty()
        val vurderingsbehov = behandling.vurderingsbehov()
        val mottattDokumenter = hentMottattDokumenter(vurderingsbehov, behandling)

        val meldekort = meldekortRepository.hentHvisEksisterer(behandling.id)
        val forrigeBehandlingMeldekort = behandling.forrigeBehandlingId?.let { meldekortRepository.hentHvisEksisterer(it) }

        val nyeMeldekort = meldekort?.meldekort()?.filter { forrigeBehandlingMeldekort?.meldekort()?.contains(it) == false  }

        val hendelse = BehandlingFlytStoppetHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            aktivtSteg = behandling.aktivtSteg(),
            status = behandling.status(),
            årsakerTilBehandling = vurderingsbehov.map { it.type.name },
            vurderingsbehov = vurderingsbehov.map { it.type.name },
            årsakTilOpprettelse = behandling.årsakTilOpprettelse?.name ?: "Ukjent årsak",
            avklaringsbehov = sortererteAvklaringsbehov(behandling, avklaringsbehovene.alle()),
            relevanteIdenterPåBehandling = pipRepository.finnIdenterPåBehandling(behandling.referanse).map { it.ident },
            erPåVent = erPåVent,
            mottattDokumenter = mottattDokumenter,
            reserverTil = hentReservertTil(behandling.id),
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
        )

        log.info("Legger til flytjobber til statistikk og stoppethendelse for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = VarsleOppgaveOmHendelseJobbUtFører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = StatistikkJobbUtfører).medPayload(
                BehandlingFlytStoppetHendelseTilStatistikk(
                    personIdent = hendelse.personIdent,
                    saksnummer = hendelse.saksnummer,
                    referanse = hendelse.referanse,
                    behandlingType = hendelse.behandlingType,
                    status = hendelse.status,
                    avklaringsbehov = hendelse.avklaringsbehov,
                    opprettetTidspunkt = hendelse.opprettetTidspunkt,
                    hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
                    versjon = hendelse.versjon,
                    opprettetAv = hentBehandlingOpprettetAv(behandling.id),
                    nyeMeldekort = nyeMeldekort
                )
            )
                .forBehandling(sak.id.id, behandling.id.id)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingMeldePerioderOgSakStatusJobbUtfører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )

        // Sender meldekort til API-intern
        flytJobbRepository.leggTil(DatadelingMeldekortJobbUtfører.nyJobb(sak.id, behandling.id))

        if (behandling.typeBehandling().erYtelsesbehandling()) {
            flytJobbRepository.leggTil(MeldeperiodeTilMeldekortBackendJobbUtfører.nyJobb(sak.id, behandling.id))
        }
    }

    private fun hentReservertTil(behandlingId: BehandlingId): String? {
        val oppfølgingsoppgavedokument =
            MottaDokumentService(dokumentRepository).hentOppfølgingsBehandlingDokument(behandlingId)

        val reserverTilBrukerRevurderingAvbrutt = finnReserverTilBrukerGittVurderingsbehov(behandlingId, no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDERING_AVBRUTT)
        val reserverTilBrukerSøknadTrukket = finnReserverTilBrukerGittVurderingsbehov(behandlingId, no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)

        if (listOfNotNull(
                oppfølgingsoppgavedokument?.reserverTilBruker,
                reserverTilBrukerRevurderingAvbrutt,
                reserverTilBrukerSøknadTrukket
            ).size > 1) {
            log.warn("Fant mer enn én reserverTil-verdi i hendelse til oppgave")
        }

        return oppfølgingsoppgavedokument?.reserverTilBruker ?: reserverTilBrukerRevurderingAvbrutt ?: reserverTilBrukerSøknadTrukket
    }

    private fun hentBehandlingOpprettetAv(behandlingId: BehandlingId): String? {
        val eldsteManuellVurderingDokument = MottaDokumentService(dokumentRepository).hentMottattDokumenterAvType(
            behandlingId,
            InnsendingType.MANUELL_REVURDERING
        ).minByOrNull { it.mottattTidspunkt }

        val manuellVurdering = eldsteManuellVurderingDokument?.strukturerteData<ManuellRevurderingV0>()?.data
        return manuellVurdering?.opprettetAv
    }

    private fun finnReserverTilBrukerGittVurderingsbehov(behandlingId: BehandlingId, vurderingsbehov: no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov): String? {
        val nyÅrsakTilBehandlingDokumenter = MottaDokumentService(dokumentRepository).hentMottattDokumenterAvType(
            behandlingId,
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING
        )

        val dokument = nyÅrsakTilBehandlingDokumenter.find { dokument ->
            val melding = dokument.ustrukturerteData()?.let { DefaultJsonMapper.fromJson<Melding>(it) }
            melding is NyÅrsakTilBehandlingV0 &&
                    melding.årsakerTilBehandling.contains(
                        vurderingsbehov
                    )
        }

        return (dokument
            ?.ustrukturerteData()
            ?.let { DefaultJsonMapper.fromJson<Melding>(it) } as? NyÅrsakTilBehandlingV0)
            ?.reserverTilBruker
    }

    private fun hentMottattDokumenter(
        vurderingsbehov: List<VurderingsbehovMedPeriode>,
        behandling: Behandling
    ): List<MottattDokumentDto> {
        // Sender kun med dokumenter ved følgende behandlingsårsaker
        val gyldigeÅrsaker = listOf(
            Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
            Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING,
            Vurderingsbehov.MOTTATT_DIALOGMELDING
        )

        return if (vurderingsbehov.any { it.type in gyldigeÅrsaker }) {
            val gyldigeDokumenter = listOf(
                InnsendingType.LEGEERKLÆRING,
                InnsendingType.LEGEERKLÆRING_AVVIST,
                InnsendingType.DIALOGMELDING,
            )

            dokumentRepository
                .hentDokumenterAvType(behandling.id, gyldigeDokumenter)
                .map { it.tilMottattDokumentDto() }
                .toList()
        } else {
            emptyList()
        }
    }

    private fun MottattDokument.tilMottattDokumentDto(): MottattDokumentDto =
        MottattDokumentDto(
            type = this.type,
            referanse = this.referanse
        )
}
