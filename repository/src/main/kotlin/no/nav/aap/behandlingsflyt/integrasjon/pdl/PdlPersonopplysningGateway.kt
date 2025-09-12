package no.nav.aap.behandlingsflyt.integrasjon.pdl

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AdresseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.bosatt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.doed
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.foedselsregistrert
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.forsvunnet
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.ikkeBosatt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.inaktiv
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.midlertidig
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.opphort
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus.utflyttet
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.UtenlandsAdresse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.miljo.Miljø
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import kotlin.jvm.javaClass

object PdlPersonopplysningGateway : PersonopplysningGateway {

    private val log = LoggerFactory.getLogger(javaClass)
    override fun innhent(person: Person): Personopplysning {
        val request = PdlRequest(PERSON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = PdlGateway.query(request)

        if (Miljø.erDev()) {
            log.info("Henter personinfo fra PDL i dev ${response.data}")
        }
        val foedselsdato = PdlParser.utledFødselsdato(response.data?.hentPerson?.foedselsdato)
            ?: error("fødselsdato skal alltid eksistere i PDL")

        val status = requireNotNull(response.data?.hentPerson?.folkeregisterpersonstatus?.firstOrNull()?.status)

        val statsborgerskap = requireNotNull(response.data.hentPerson.statsborgerskap?.map {
            Statsborgerskap(
                land = it.land,
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed
            )
        })

        return Personopplysning(
            id = 0, // Setter no bs her for å få det gjennom
            fødselsdato = foedselsdato,
            dødsdato = response.data.hentPerson.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) },
            statsborgerskap = statsborgerskap,
            status = pdlStatusTilDomene(status),
            utenlandsAddresser = mapUtenlandsAdresser(response.data.hentPerson)
        )
    }

    private fun pdlStatusTilDomene(status: PersonStatus): no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus =
        when (status) {
            PersonStatus.bosatt -> bosatt
            PersonStatus.utflyttet -> utflyttet
            PersonStatus.forsvunnet -> forsvunnet
            PersonStatus.doed -> doed
            PersonStatus.opphort -> opphort
            PersonStatus.foedselsregistrert -> foedselsregistrert
            PersonStatus.ikkeBosatt -> ikkeBosatt
            PersonStatus.midlertidig -> midlertidig
            PersonStatus.inaktiv -> inaktiv
        }

    override fun innhentMedHistorikk(person: Person): PersonopplysningMedHistorikk {
        val request = PdlRequest(PERSON_QUERY_HISTORIKK, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = PdlGateway.query(request)

        val foedselsdato = PdlParser.utledFødselsdato(response.data?.hentPerson?.foedselsdato)
            ?: error("fødselsdato skal alltid eksistere i PDL")

        val folkeregisterStatuser = requireNotNull(response.data?.hentPerson?.folkeregisterpersonstatus?.map {
            FolkeregisterStatus(
                pdlStatusTilDomene(it.status),
                it.folkeregistermetadata?.gyldighetstidspunkt?.toLocalDate(),
                it.folkeregistermetadata?.opphoerstidspunkt?.toLocalDate()
            )
        })
        val statsborgerskap = requireNotNull(response.data.hentPerson.statsborgerskap?.map {
            Statsborgerskap(
                land = it.land,
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed
            )
        })

        return PersonopplysningMedHistorikk(
            id = 0, // Setter no bs her for å få det gjennom
            fødselsdato = foedselsdato,
            dødsdato = response.data.hentPerson.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) },
            statsborgerskap = statsborgerskap,
            folkeregisterStatuser = folkeregisterStatuser,
            utenlandsAddresser = mapUtenlandsAdresser(response.data.hentPerson)
        )
    }
}

private fun mapUtenlandsAdresser(personInfo: PdlPersoninfo?): List<UtenlandsAdresse>? {
    if (personInfo == null) return null

    val bostedsAdresser = personInfo.bostedsadresse.orEmpty().mapNotNull {
        it.utenlandskAdresse?.let { adresse ->
            UtenlandsAdresse(
                gyldigFraOgMed = it.gyldigFraOgMed?.toLocalDate(),
                gyldigTilOgMed = it.gyldigTilOgMed?.toLocalDate(),
                adresseNavn = adresse.adressenavnNummer,
                postkode = adresse.postkode,
                bySted = adresse.bySted,
                landkode = adresse.landkode,
                adresseType = AdresseType.BOSTEDS_ADRESSE
            )
        }
    }

    val kontaktAdresser = personInfo.kontaktadresse.orEmpty().mapNotNull {
        when {
            it.utenlandskAdresse != null -> {
                val adresse = it.utenlandskAdresse
                UtenlandsAdresse(
                    gyldigFraOgMed = it.gyldigFraOgMed?.toLocalDate(),
                    gyldigTilOgMed = it.gyldigTilOgMed?.toLocalDate(),
                    adresseNavn = adresse.adressenavnNummer,
                    postkode = adresse.postkode,
                    bySted = adresse.bySted,
                    landkode = adresse.landkode,
                    adresseType = AdresseType.KONTAKT_ADRESSE
                )
            }

            it.utenlandskAdresseIFrittFormat != null -> {
                val frittFormat = it.utenlandskAdresseIFrittFormat
                val adresseNavn = listOfNotNull(
                    frittFormat.adresselinje1,
                    frittFormat.adresselinje2,
                    frittFormat.adresselinje3
                ).joinToString(", ")

                UtenlandsAdresse(
                    gyldigFraOgMed = it.gyldigFraOgMed?.toLocalDate(),
                    gyldigTilOgMed = it.gyldigTilOgMed?.toLocalDate(),
                    adresseNavn = adresseNavn,
                    postkode = frittFormat.postkode,
                    bySted = frittFormat.byEllerStedsnavn,
                    landkode = frittFormat.landkode,
                    adresseType = AdresseType.KONTAKT_ADRESSE
                )
            }

            else -> null
        }
    }

    val oppholdsAdresser = personInfo.oppholdsadresse.orEmpty().mapNotNull {
        it.utenlandskAdresse?.let { adresse ->
            UtenlandsAdresse(
                gyldigFraOgMed = it.gyldigFraOgMed?.toLocalDate(),
                gyldigTilOgMed = it.gyldigTilOgMed?.toLocalDate(),
                adresseNavn = adresse.adressenavnNummer,
                postkode = adresse.postkode,
                bySted = adresse.bySted,
                landkode = adresse.landkode,
                adresseType = AdresseType.OPPHOLDS_ADRESSE
            )
        }
    }

    return oppholdsAdresser + kontaktAdresser + bostedsAdresser
}

@Suppress("GraphQLUnresolvedReference")
@Language("GraphQL")
val PERSON_QUERY = $$"""
    query($ident: ID!){
      hentPerson(ident: $ident) {
        oppholdsadresse {
            gyldigFraOgMed
            gyldigTilOgMed
            utenlandskAdresse {
              adressenavnNummer
              bygningEtasjeLeilighet
              postboksNummerNavn
              postkode
              bySted
              regionDistriktOmraade
              landkode
            }
        },
        bostedsadresse {
            gyldigFraOgMed
            gyldigTilOgMed
            utenlandskAdresse {
              adressenavnNummer
              bygningEtasjeLeilighet
              postboksNummerNavn
              postkode
              bySted
              regionDistriktOmraade
              landkode
            }
        },
        kontaktadresse {
            gyldigFraOgMed
            gyldigTilOgMed
            utenlandskAdresseIFrittFormat {
                adresselinje1
                adresselinje2
                adresselinje3
                postkode
                byEllerStedsnavn
                landkode
            },
            utenlandskAdresse {
              adressenavnNummer
              bygningEtasjeLeilighet
              postboksNummerNavn
              postkode
              bySted
              regionDistriktOmraade
              landkode
            }
        },
        doedsfall {
            doedsdato
        },
        foedselsdato {
            foedselsdato
        },
        statsborgerskap {
            land,
            gyldigFraOgMed, 
            gyldigTilOgMed
        },
        folkeregisterpersonstatus {
            status
        }
      }
    }
""".trimIndent()

@Suppress("GraphQLUnresolvedReference")
@Language("GraphQL")
val PERSON_QUERY_HISTORIKK = $$"""
    query($ident: ID!){
      hentPerson(ident: $ident) {
        oppholdsadresse(historikk: true) {
            gyldigFraOgMed
            gyldigTilOgMed
            utenlandskAdresse {
              adressenavnNummer
              postkode
              bySted
              landkode
            }
        },
        bostedsadresse(historikk: true) {
            gyldigFraOgMed
            gyldigTilOgMed
            utenlandskAdresse {
              adressenavnNummer
              postkode
              bySted
              landkode
            }
        },
        kontaktadresse(historikk: true) {
            gyldigFraOgMed
            gyldigTilOgMed
            utenlandskAdresseIFrittFormat {
                adresselinje1
                adresselinje2
                adresselinje3
                postkode
                byEllerStedsnavn
                landkode
            },
            utenlandskAdresse {
              adressenavnNummer
              postkode
              bySted
              landkode
            }
        },
        foedselsdato {
        foedselsdato
        },
        statsborgerskap(historikk: true) {
            land,
            gyldigFraOgMed,
            gyldigTilOgMed
        },
        folkeregisterpersonstatus(historikk: true) {
            status,
            folkeregistermetadata {
                gyldighetstidspunkt,
                opphoerstidspunkt
            }
        }
      }
    }
""".trimIndent()