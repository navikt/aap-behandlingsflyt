package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.tidslinje.Segment

data class Oppholdene(internal val id: Long?, val opphold: List<Segment<Institusjon>>)