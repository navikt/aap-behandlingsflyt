package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.komponenter.gateway.Gateway

interface StatistikkGateway : Gateway {
    fun avgiStatistikk(hendelse: StoppetBehandling)

    fun resendBehandling(hendelse: StoppetBehandling)
}