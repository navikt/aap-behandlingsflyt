package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.komponenter.tidslinje.Segment

data class MedlemskapUnntakGrunnlag(val unntak: List<Segment<Unntak>>)