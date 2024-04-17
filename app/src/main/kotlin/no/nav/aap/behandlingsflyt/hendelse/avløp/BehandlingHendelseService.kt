package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.flyt.flate.AvklaringsbehovDTO
import no.nav.aap.behandlingsflyt.flyt.flate.EndringDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.json.DefaultJsonMapper
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BehandlingHendelseService::class.java)

class BehandlingHendelseService(private val sakService: SakService) {

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {
        // TODO: Slippe ut event om at behandlingen har stoppet opp
        val sak = sakService.hent(behandling.sakId)
        val hendelse = BehandlingFlytStoppetHendelse(
            saksnummer = sak.saksnummer,
            referanse = BehandlingReferanse(behandling.referanse.toString()),
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                AvklaringsbehovDTO(
                    definisjon = avklaringsbehov.definisjon,
                    status = avklaringsbehov.status(),
                    endringer = avklaringsbehov.historikk.map { endring ->
                        EndringDTO(
                            status = endring.status,
                            tidsstempel = endring.tidsstempel,
                            begrunnelse = endring.begrunnelse + "Noe",
                            endretAv = endring.endretAv
                        )
                    }
                )
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt
        )
        // TODO: Antar at det trenges flere felter for god prioritering av oppgaver
        log.info(DefaultJsonMapper.toJson(hendelse))
    }
}