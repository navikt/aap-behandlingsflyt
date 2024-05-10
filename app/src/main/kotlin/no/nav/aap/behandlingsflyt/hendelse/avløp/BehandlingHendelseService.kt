package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService

class BehandlingHendelseService(private val sakService: SakService) {

    private val oppgavestyringGateway = OppgavestyringGateway

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {
        // TODO: Slippe ut event om at behandlingen har stoppet opp
        val sak = sakService.hent(behandling.sakId)

        // TODO: Se på hvordan hendelsen ser ut ved retur fra beslutter på mer enn et behov og adferden der
        val hendelse = BehandlingFlytStoppetHendelse(
            personident = sak.person.aktivIdent().identifikator,
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
                            endretAv = endring.endretAv,
                            frist = endring.frist
                        )
                    }
                )
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt
        )

        // TODO: Utvide med flere parametere for prioritering

        oppgavestyringGateway.varsleHendelse(hendelse)
    }
}