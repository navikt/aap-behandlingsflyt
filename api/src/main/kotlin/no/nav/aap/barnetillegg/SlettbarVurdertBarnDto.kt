package no.nav.aap.barnetillegg

data class SlettbarVurdertBarnDto(
    val vurdertBarn: ExtendedVurdertBarnDto,
    val erSlettbar: Boolean
)