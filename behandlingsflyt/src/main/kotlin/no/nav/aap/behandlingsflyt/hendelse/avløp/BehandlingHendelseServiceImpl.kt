package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.prosessering.DatadelingMeldePerioderJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.DatadelingSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur as DomeneÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilReturKode as ÅrsakTilReturKodeKontrakt

private val log = LoggerFactory.getLogger(BehandlingHendelseServiceImpl::class.java)

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val sakService: SakService
) : BehandlingHendelseService {

    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        val sak = sakService.hent(behandling.sakId)

        // TODO: Utvide med flere parametere for prioritering
        val hendelse = BehandlingFlytStoppetHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
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
                            årsakTilRetur = endring.årsakTilRetur.map {
                                ÅrsakTilRetur(
                                    it.oversettTilKontrakt()
                                )
                            })
                    },
                    typeBrev = brevbestilling?.typeBrev?.oversettTilKontrakt()
                )
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
        )

        log.info("Legger til flytjobber til statistikk og stoppethendelse for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(hendelse)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = StatistikkJobbUtfører).medPayload(hendelse)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingMeldePerioderJobbUtfører).medPayload(hendelse)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingSakStatusJobbUtfører).medPayload(hendelse)
        )

    }

    private fun DomeneÅrsakTilRetur.oversettTilKontrakt(): ÅrsakTilReturKodeKontrakt {
        return when (this.årsak) {
            ÅrsakTilReturKode.MANGELFULL_BEGRUNNELSE -> ÅrsakTilReturKodeKontrakt.MANGELFULL_BEGRUNNELSE
            ÅrsakTilReturKode.MANGLENDE_UTREDNING -> ÅrsakTilReturKodeKontrakt.MANGLENDE_UTREDNING
            ÅrsakTilReturKode.MANGELFULL_VILKAARSVURDERING -> ÅrsakTilReturKodeKontrakt.MANGELFULL_VILKAARSVURDERING
            ÅrsakTilReturKode.FEIL_LOVANVENDELSE -> ÅrsakTilReturKodeKontrakt.FEIL_LOVANVENDELSE
            ÅrsakTilReturKode.ANNET -> ÅrsakTilReturKodeKontrakt.ANNET
        }
    }

    private fun no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.oversettTilKontrakt(): ÅrsakTilSettPåVent {
        return when (this) {
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL -> ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER -> ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
        }
    }

    private fun no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.oversettTilKontrakt(): TypeBrev {
        return when (this) {
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VEDTAK_AVSLAG -> TypeBrev.VEDTAK_AVSLAG
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VEDTAK_INNVILGELSE -> TypeBrev.VEDTAK_INNVILGELSE
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.VARSEL_OM_BESTILLING -> TypeBrev.VARSEL_OM_BESTILLING
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT
        }
    }
}
