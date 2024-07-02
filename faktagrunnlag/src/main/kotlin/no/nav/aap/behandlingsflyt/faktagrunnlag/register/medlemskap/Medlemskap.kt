package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

class Medlemskap(val unntakId: Number, val ident: String) {
    companion object {
        fun nyttMedlemskap(unntakId: Number, ident: String) = Medlemskap(unntakId, ident)
    }
}