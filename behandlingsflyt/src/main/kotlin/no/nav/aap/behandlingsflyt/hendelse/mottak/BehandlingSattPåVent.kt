package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate

class BehandlingSattPåVent(
    val frist: LocalDate?,
    val begrunnelse: String,
    val grunn: ÅrsakTilSettPåVent,
    val bruker: Bruker,
    val behandlingVersjon: Long
)