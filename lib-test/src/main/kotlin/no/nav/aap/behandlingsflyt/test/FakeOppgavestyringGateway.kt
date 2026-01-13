package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.komponenter.gateway.Factory

class FakeOppgavestyringGateway : OppgavestyringGateway {
    override fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse) {
        /**
         * Noop
         */
    }

    override fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingHendelseKafkaMelding) {
        /**
         * Noop
         */
    }

    override fun finnNayEnhetForPerson(
        personIdent: String,
        relevanteIdenter: List<String>
    ): EnhetNrDto {
        return EnhetNrDto("1234")
    }


    companion object : Factory<OppgavestyringGateway> {
        override fun konstruer(): OppgavestyringGateway = FakeOppgavestyringGateway()
    }

}