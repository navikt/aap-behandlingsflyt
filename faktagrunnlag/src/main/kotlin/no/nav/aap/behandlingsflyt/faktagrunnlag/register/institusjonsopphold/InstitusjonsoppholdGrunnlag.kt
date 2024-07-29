package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.tidslinje.Segment

data class InstitusjonsoppholdGrunnlag(val opphold: List<Segment<Institusjon>>)