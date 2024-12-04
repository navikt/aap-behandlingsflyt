package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.komponenter.tidslinje.Segment

data class Oppholdene(internal val id: Long?, val opphold: List<Segment<Institusjon>>)