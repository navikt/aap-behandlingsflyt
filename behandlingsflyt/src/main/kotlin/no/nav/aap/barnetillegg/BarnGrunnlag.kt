package no.nav.aap.barnetillegg

data class BarnGrunnlag(
    val registerbarn: RegisterBarn?,
    val oppgitteBarn: OppgitteBarn?,
    val saksbehandlerOppgitteBarn: SaksbehandlerOppgitteBarn?,
    val vurderteBarn: VurderteBarn?
)