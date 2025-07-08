package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

data class PersonopplysningGrunnlag(
    val brukerPersonopplysning: Personopplysning,
    val relatertePersonopplysninger: RelatertePersonopplysninger?
)
