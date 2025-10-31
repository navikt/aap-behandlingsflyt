package no.nav.aap.behandlingsflyt.integrasjon.pdl

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Factory
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class PdlBarnGateway : BarnGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<BarnGateway> {
        override fun konstruer(): BarnGateway {
            return PdlBarnGateway()
        }
    }

    override fun hentBarn(person: Person, oppgitteBarnIdenter: List<Ident>): BarnInnhentingRespons {
        val barnRelasjoner = hentBarnRelasjoner(person)

        val barnIdenter = barnRelasjoner.filterIsInstance<BarnIdentifikator.BarnIdent>().map { it.ident }
        val barnUtenIdent = barnRelasjoner.filterIsInstance<BarnIdentifikator.NavnOgFødselsdato>().map {
            Barn(
                ident = it,
                fødselsdato = it.fødselsdato,
                dødsdato = null,
                navn = it.navn
            )
        }

        val registerBarn = hentBarn(barnIdenter)
        val oppgitteBarn = hentBarn(oppgitteBarnIdenter)
        return BarnInnhentingRespons(registerBarn + barnUtenIdent, oppgitteBarn)
    }

    private fun hentBarnRelasjoner(person: Person): List<BarnIdentifikator> {
        val request = PdlRequest(BARN_RELASJON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response = PdlGateway.query<PdlRelasjonDataResponse>(request)

        val relasjoner = response.data?.hentPerson?.forelderBarnRelasjon.orEmpty()
            .filter { it.relatertPersonsRolle == ForelderBarnRelasjonRolle.BARN }

        return relasjoner.map { relasjon ->
            if (relasjon.relatertPersonsIdent == null) {
                val harNavnOgFodselsdato =
                    relasjon.relatertPersonUtenFolkeregisteridentifikator?.foedselsdato != null && relasjon.relatertPersonUtenFolkeregisteridentifikator.navn != null
                log.info("Barn har ingen ident. Har navn og fodselsdato: $harNavnOgFodselsdato")
                if (harNavnOgFodselsdato) {
                    BarnIdentifikator.NavnOgFødselsdato(
                        relasjon.relatertPersonUtenFolkeregisteridentifikator.navn.let { it.fornavn + " " + it.etternavn },
                        Fødselsdato(relasjon.relatertPersonUtenFolkeregisteridentifikator.foedselsdato)
                    )
                } else {
                    error("Barn mangler både ident og kombinasjonen navn+fødselsdato.")
                }
            } else {
                BarnIdentifikator.BarnIdent(
                    requireNotNull(relasjon.relatertPersonsIdent) { "Vi støtter ikke per nå at denne er null fra PDL " }
                )
            }
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
                        ident = BarnIdentifikator.BarnIdent(res.ident),
                        fødselsdato = requireNotNull(fødselsdato) { "Barn i PDL manglet fødselsdato. " },
                        dødsdato = person.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) },
                        navn = person.navn?.firstOrNull()?.let { "${it.fornavn} ${it.etternavn}" })
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
                relatertPersonsRolle
                relatertPersonsIdent
                relatertPersonUtenFolkeregisteridentifikator {
                  foedselsdato
                  navn {
                    etternavn
                    fornavn
                    mellomnavn
                  }
                 statsborgerskap
               }
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
                },
                navn {
                    fornavn
                    mellomnavn
                    etternavn
                }
            }
            code
        }
    }
""".trimIndent()
