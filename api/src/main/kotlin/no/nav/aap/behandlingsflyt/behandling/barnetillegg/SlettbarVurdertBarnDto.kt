package no.nav.aap.behandlingsflyt.behandling.barnetillegg

data class SlettbarVurdertBarnDto(
    val vurdertBarn: ExtendedVurdertBarnDto,
    val erSlettbar: Boolean
)