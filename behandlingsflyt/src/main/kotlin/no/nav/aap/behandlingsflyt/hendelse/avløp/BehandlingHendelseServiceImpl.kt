package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_FUNKSJONALITET
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_KLAGE_IMPLEMENTASJON
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.FORVALTNINGSMELDING
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.KLAGE_AVVIST
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.KLAGE_OPPRETTHOLDELSE
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.KLAGE_TRUKKET
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VARSEL_OM_BESTILLING
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VEDTAK_AVSLAG
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VEDTAK_ENDRING
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VEDTAK_INNVILGELSE
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering.Markering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering.MarkeringRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.MarkeringDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.MottattDokumentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.prosessering.DatadelingMeldePerioderJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.DatadelingSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.MeldeperiodeTilMeldekortBackendJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur as DomeneÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilReturKode as ÅrsakTilReturKodeKontrakt

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val sakService: SakService,
    private val dokumentRepository: MottattDokumentRepository,
    private val markeringRepository: MarkeringRepository
) : BehandlingHendelseService {
    constructor(repositoryProvider: RepositoryProvider) : this(
        flytJobbRepository = repositoryProvider.provide(),
        brevbestillingRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider),
        dokumentRepository = repositoryProvider.provide(),
        markeringRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val sak = sakService.hent(behandling.sakId)
        val erPåVent = avklaringsbehovene.hentÅpneVentebehov().isNotEmpty()
        val årsaker = behandling.årsaker()
        val mottattDokumenter = hentMottattDokumenter(årsaker, behandling)
        val aktiveMarkeringer = markeringRepository.hentAktiveMarkeringerForBehandling(behandling.id).map { it.tilBehandlingMarkeringDto() }

        val hendelse = BehandlingFlytStoppetHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            aktivtSteg = behandling.aktivtSteg(),
            status = behandling.status(),
            årsakerTilBehandling = årsaker.map { it.type.name },
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                val brevbestilling = if (avklaringsbehov.definisjon == Definisjon.SKRIV_BREV) {
                    brevbestillingRepository.hent(behandling.id)
                        .firstOrNull { it.status == Status.FORHÅNDSVISNING_KLAR }
                } else {
                    null
                }
                AvklaringsbehovHendelseDto(
                    avklaringsbehovDefinisjon = avklaringsbehov.definisjon,
                    status = avklaringsbehov.status(),
                    endringer = avklaringsbehov.historikk.map { endring ->
                        EndringDTO(
                            status = endring.status,
                            tidsstempel = endring.tidsstempel,
                            endretAv = endring.endretAv,
                            frist = endring.frist,
                            årsakTilSattPåVent = endring.grunn?.oversettTilKontrakt(),
                            begrunnelse = endring.begrunnelse,
                            årsakTilRetur = endring.årsakTilRetur.map {
                                ÅrsakTilRetur(it.oversettTilKontrakt())
                            })
                    },
                    typeBrev = brevbestilling?.typeBrev?.oversettTilKontrakt()
                )
            },
            erPåVent = erPåVent,
            mottattDokumenter = mottattDokumenter,
            reserverTil = hentReservertTil(behandling.id),
            markeringer = aktiveMarkeringer,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
        )

        log.info("Legger til flytjobber til statistikk og stoppethendelse for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = StatistikkJobbUtfører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingMeldePerioderJobbUtfører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )

        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingSakStatusJobbUtfører).medPayload(hendelse)
                .forBehandling(sak.id.id, behandling.id.id)
        )

        if (behandling.typeBehandling() in listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)) {
            flytJobbRepository.leggTil(MeldeperiodeTilMeldekortBackendJobbUtfører.nyJobb(sak.id, behandling.id))
        }
    }

    private fun hentReservertTil(behandlingId: BehandlingId): String? {
        val oppfølgingsoppgavedokument =
            MottaDokumentService(dokumentRepository).hentOppfølgingsBehandlingDokument(behandlingId)

        if (oppfølgingsoppgavedokument == null) {
            return null
        }

        return oppfølgingsoppgavedokument.reserverTilBruker
    }

    private fun hentMottattDokumenter(
        årsaker: List<Årsak>,
        behandling: Behandling
    ): List<MottattDokumentDto> {
        // Sender kun med dokumenter ved følgende behandlingsårsaker
        val gyldigeÅrsaker = listOf(
            ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
            ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING,
            ÅrsakTilBehandling.MOTTATT_DIALOGMELDING
        )

        return if (årsaker.any { it.type in gyldigeÅrsaker }) {
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

    private fun DomeneÅrsakTilRetur.oversettTilKontrakt(): ÅrsakTilReturKodeKontrakt {
        return when (this.årsak) {
            ÅrsakTilReturKode.MANGELFULL_BEGRUNNELSE -> ÅrsakTilReturKodeKontrakt.MANGELFULL_BEGRUNNELSE
            ÅrsakTilReturKode.MANGLENDE_UTREDNING -> ÅrsakTilReturKodeKontrakt.MANGLENDE_UTREDNING
            ÅrsakTilReturKode.FEIL_LOVANVENDELSE -> ÅrsakTilReturKodeKontrakt.FEIL_LOVANVENDELSE
            ÅrsakTilReturKode.ANNET -> ÅrsakTilReturKodeKontrakt.ANNET
        }
    }

    private fun no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.oversettTilKontrakt(): ÅrsakTilSettPåVent {
        return when (this) {
            VENTER_PÅ_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
            VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
            VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
            VENTER_PÅ_VURDERING_AV_ROL -> ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL
            VENTER_PÅ_SVAR_FRA_BRUKER -> ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
            VENTER_PÅ_MASKINELL_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
            VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
            VENTER_PÅ_KLAGE_IMPLEMENTASJON -> ÅrsakTilSettPåVent.VENTER_PÅ_KLAGE_IMPLEMENTASJON
            VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL -> ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL
            VENTER_PÅ_FUNKSJONALITET -> ÅrsakTilSettPåVent.VENTER_PÅ_FUNKSJONALITET
        }
    }

    private fun no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.oversettTilKontrakt(): TypeBrev {
        return when (this) {
            VEDTAK_AVSLAG -> TypeBrev.VEDTAK_AVSLAG
            VEDTAK_INNVILGELSE -> TypeBrev.VEDTAK_INNVILGELSE
            VEDTAK_ENDRING -> TypeBrev.VEDTAK_ENDRING
            VARSEL_OM_BESTILLING -> TypeBrev.VARSEL_OM_BESTILLING
            FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT
            KLAGE_AVVIST -> TypeBrev.KLAGE_AVVIST
            KLAGE_OPPRETTHOLDELSE -> TypeBrev.KLAGE_OPPRETTHOLDELSE
            KLAGE_TRUKKET -> TypeBrev.KLAGE_TRUKKET
            FORHÅNDSVARSEL_KLAGE_FORMKRAV -> TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV
            FORVALTNINGSMELDING -> TypeBrev.FORVALTNINGSMELDING
        }
    }

    private fun MottattDokument.tilMottattDokumentDto(): MottattDokumentDto =
        MottattDokumentDto(
            type = this.type,
            referanse = this.referanse
        )

    private fun Markering.tilBehandlingMarkeringDto(): MarkeringDto =
        MarkeringDto(
            markeringType = this.markeringType,
            begrunnelse = this.begrunnelse,
            opprettetAv = AnsattInfoService().hentAnsattNavn(this.opprettetAv) ?: "Ukjent",
        )
}
