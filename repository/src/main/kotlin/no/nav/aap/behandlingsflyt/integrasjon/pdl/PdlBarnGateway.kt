package no.nav.aap.behandlingsflyt.integrasjon.pdl

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Factory
import org.intellij.lang.annotations.Language

class PdlBarnGateway : BarnGateway {
    companion object : Factory<BarnGateway> {
        override fun konstruer(): BarnGateway {
            return PdlBarnGateway()
        }
    }

    override fun hentBarn(person: Person, oppgitteBarnIdenter: List<Ident>): BarnInnhentingRespons {
        val barnRelasjoner = hentBarnRelasjoner(person)
        val registerBarn = hentBarn(barnRelasjoner)
        val oppgitteBarn = hentBarn(oppgitteBarnIdenter)
        return BarnInnhentingRespons(registerBarn, oppgitteBarn)
    }

    private fun hentBarnRelasjoner(person: Person): List<Ident> {
        val request = PdlRequest(BARN_RELASJON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response = PdlGateway.query<PdlRelasjonDataResponse>(request)

        val relasjoner = (response.data?.hentPerson?.forelderBarnRelasjon ?: return emptyList())

        return relasjoner.map {
            Ident(
                requireNotNull(it.relatertPersonsIdent) { "Vi støtter ikke per nå at denne er null fra PDL " }
            )
        }
    }

    private fun hentBarn(identer: List<Ident>): List<Barn> {
        if (identer.isEmpty()) {
            return emptyList()
        }

        val request = PdlRequest(PERSON_BOLK_QUERY, IdentVariables(identer = identer.map { it.identifikator }))
        val response: PdlRelasjonDataResponse = PdlGateway.query(request)

        val bolk = response.data?.hentPersonBolk ?: return emptyList()

        return bolk.mapNotNull { res ->
            res.person?.let { person ->
                person.foedselsdato?.let { foedsel ->
                    val fødselsdato = PdlParser.utledFødselsdato(foedsel)
                    Barn(
                        ident = Ident(res.ident),
                        fødselsdato = requireNotNull(fødselsdato) { "Barn i PDL manglet fødselsdato. " },
                        dødsdato = person.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) })
                }
            }
        }
    }
}

@Language("GraphQL")
val BARN_RELASJON_QUERY = $$"""
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            forelderBarnRelasjon {
                relatertPersonsIdent
            }
        }
    }
""".trimIndent()

@Language("GraphQL")
val PERSON_BOLK_QUERY = $$"""
    query($identer: [ID!]!) {
        hentPersonBolk(identer: $identer) {
            ident,
            person {
                doedsfall {
                    doedsdato
                },
                foedselsdato {
                    foedselsdato
                }
            }
            code
        }
    }
""".trimIndent()
