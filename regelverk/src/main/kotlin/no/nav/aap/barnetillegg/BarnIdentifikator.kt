package no.nav.aap.barnetillegg

import no.nav.aap.misc.Ident
import no.nav.aap.personopplysninger.Fødselsdato

sealed class BarnIdentifikator : Comparable<BarnIdentifikator> {
    abstract fun hentIdent(): Ident?

    data class BarnIdent(
        val ident: Ident,
        val navn: String? = null,
        val fødselsdato: Fødselsdato? = null,
    ) : BarnIdentifikator() {
        constructor(ident: String) : this(Ident(ident))

        override fun hentIdent(): Ident = ident

        override fun compareTo(other: BarnIdentifikator): Int {
            return when (other) {
                is BarnIdent -> ident.identifikator.compareTo(other.ident.identifikator)
                is NavnOgFødselsdato -> -1 // Forskjellige typer, ikke sammenlignbare
            }
        }
    }

    data class NavnOgFødselsdato(val navn: String, val fødselsdato: Fødselsdato) : BarnIdentifikator() {
        override fun hentIdent(): Ident? = null

        override fun compareTo(other: BarnIdentifikator): Int {
            return when (other) {
                is BarnIdent -> 1 // Forskjellige typer, ikke sammenlignbare
                is NavnOgFødselsdato -> {
                    val navnComparison = navn.trim().compareTo(other.navn.trim(), ignoreCase = true)
                    if (navnComparison != 0) navnComparison else fødselsdato.toLocalDate()
                        .compareTo(other.fødselsdato.toLocalDate())
                }
            }
        }
    }

    fun er(other: BarnIdentifikator) = this == other || this.compareTo(other) == 0 || other.compareTo(this) == 0
}