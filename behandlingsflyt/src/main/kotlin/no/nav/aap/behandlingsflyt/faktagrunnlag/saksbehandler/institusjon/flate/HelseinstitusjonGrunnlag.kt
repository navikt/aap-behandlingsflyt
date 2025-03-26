package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

data class HelseinstitusjonGrunnlag(
    val harTilgangTilÅSaksbehandle: Boolean,
    val opphold: List<InstitusjonsoppholdDto>, val vurderinger: List<Helseopphold>
)
