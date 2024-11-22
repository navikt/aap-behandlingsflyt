package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.DefinisjonDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(BehandlingHendelseServiceImpl::class.java)

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakService: SakService
) : BehandlingHendelseService {

    override fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {
        val sak = sakService.hent(behandling.sakId)

        // TODO: Utvide med flere parametere for prioritering
        val hendelse = BehandlingFlytStoppetHendelse(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                AvklaringsbehovHendelseDto(
                    definisjon = DefinisjonDTO(
                        type = avklaringsbehov.definisjon.kode,
                        behovType = avklaringsbehov.definisjon.type,
                        løsesISteg = avklaringsbehov.løsesISteg()
                    ), status = avklaringsbehov.status(),
                    endringer = avklaringsbehov.historikk.map { endring ->
                        EndringDTO(
                            status = endring.status,
                            tidsstempel = endring.tidsstempel,
                            endretAv = endring.endretAv,
                            frist = endring.frist,
                            årsakTilSattPåVent = endring.grunn?.oversettTilKontrakt()
                        )
                    })
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
        )

        val payload = DefaultJsonMapper.toJson(hendelse)

        log.info("Legger til flytjobber til statistikk og stoppethendelse for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = StatistikkJobbUtfører).medPayload(payload)
        )
    }

    fun no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.oversettTilKontrakt(): ÅrsakTilSettPåVent {
        return when (this) {
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL -> ÅrsakTilSettPåVent.VENTER_PÅ_VURDERING_AV_ROL
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER -> ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
            no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING -> ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING
        }
    }
}
