package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput

class BehandlingHendelseService(
    private val sakService: SakService, private val flytJobbRepository: FlytJobbRepository
) {

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {
        val sak = sakService.hent(behandling.sakId)

        // TODO: Utvide med flere parametere for prioritering
        val hendelse = BehandlingFlytStoppetHendelse(
            personident = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = BehandlingReferanse(behandling.referanse.toString()),
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                AvklaringsbehovHendelseDto(definisjon = DefinisjonDTO(
                    type = avklaringsbehov.definisjon.kode,
                    behovType = avklaringsbehov.definisjon.type,
                    løsesISteg = avklaringsbehov.løsesISteg()
                ), status = avklaringsbehov.status(), endringer = avklaringsbehov.historikk.filter {
                    it.status in listOf(
                        Status.OPPRETTET, Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.AVSLUTTET
                    )
                }.map { endring ->
                    EndringDTO(
                        status = endring.status,
                        tidsstempel = endring.tidsstempel,
                        endretAv = endring.endretAv,
                        frist = endring.frist
                    )
                })
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt
        )

        val payload = DefaultJsonMapper.toJson(hendelse)
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
        )
        flytJobbRepository.leggTil(
            JobbInput(jobb = StatistikkJobbUtfører).medPayload(payload)
        )
    }
}