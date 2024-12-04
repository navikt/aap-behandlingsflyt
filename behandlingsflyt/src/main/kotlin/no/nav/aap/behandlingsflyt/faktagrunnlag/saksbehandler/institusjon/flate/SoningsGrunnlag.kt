package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

data class SoningsGrunnlag(val soningsforhold: List<InstitusjonsoppholdDto>, val vurderinger: List<Soningsforhold>)
