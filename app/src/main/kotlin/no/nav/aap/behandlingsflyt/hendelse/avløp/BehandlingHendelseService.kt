package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
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
                AvklaringsbehovHendelseDto(
                    definisjon = DefinisjonDTO(
                        type = avklaringsbehov.definisjon.kode,
                        behovType = avklaringsbehov.definisjon.type,
                        løsesISteg = avklaringsbehov.løsesISteg()
                    ),
                    status = avklaringsbehov.status(),
                    endringer = avklaringsbehov.historikk.filter {
                        it.status in listOf(
                            Status.OPPRETTET,
                            Status.SENDT_TILBAKE_FRA_BESLUTTER,
                            Status.AVSLUTTET
                        )
                    }.map { endring ->
                        EndringDTO(
                            status = endring.status,
                            tidsstempel = endring.tidsstempel,
                            endretAv = endring.endretAv
                        )
                    }
                )
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt
        )
        OppgavestyringGateway.varsleHendelse(hendelse)
        log.info(DefaultJsonMapper.toJson(hendelse))
    }
}