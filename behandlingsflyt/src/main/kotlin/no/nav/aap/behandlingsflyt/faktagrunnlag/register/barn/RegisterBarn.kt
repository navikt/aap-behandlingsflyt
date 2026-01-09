package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

data class RegisterBarn(val id: Long, val barn: List<Barn>)

fun List<Barn>.filtrerBortMigrerteBarn(): List<Barn> {
    return this.filterNot { barn ->
        barn.navn?.startsWith("migrert fra dsf") == true &&
                this.any { annetBarn ->
                    annetBarn != barn &&
                            annetBarn.fødselsdato == barn.fødselsdato &&
                            annetBarn.ident.hentIdent() != null
                }
    }
}