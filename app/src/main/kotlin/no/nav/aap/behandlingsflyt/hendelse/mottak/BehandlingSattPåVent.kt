package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.auth.Bruker
import java.time.LocalDate

class BehandlingSattPåVent(val frist: LocalDate?, val begrunnelse: String, val bruker: Bruker, val behandlingVersjon: Long) : BehandlingHendelse