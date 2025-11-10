package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

data class BarnGrunnlag(
    val registerbarn: RegisterBarn?,
    val oppgitteBarn: OppgitteBarn?,
    val saksbehandlerOppgitteBarn: SaksbehandlerOppgitteBarn?,
    val vurderteBarn: VurderteBarn?
)