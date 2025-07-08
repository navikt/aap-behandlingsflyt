package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AdresseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.UtenlandsAdresse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IdentVariables
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfo
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

object PdlPersonopplysningGateway : PersonopplysningGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.pdl.scope"),
        additionalHeaders = listOf(Header("Behandlingsnummer", "B287"))
    )
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = PdlResponseHandler(),
        prometheus = prometheus
    )

    private fun query(request: PdlRequest): PdlPersoninfoDataResponse {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun innhent(person: Person): Personopplysning? {
        val request = PdlRequest(PERSON_QUERY, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = query(request)

        val foedselsdato = PdlParser.utledFødselsdato(response.data?.hentPerson?.foedselsdato)
            ?: return null

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
            status = status,
            utenlandsAddresser = mapUtenlandsAdresser(response.data.hentPerson)
        )
    }

    override fun innhentMedHistorikk(person: Person): PersonopplysningMedHistorikk? {
        val request = PdlRequest(PERSON_QUERY_HISTORIKK, IdentVariables(person.aktivIdent().identifikator))
        val response: PdlPersoninfoDataResponse = query(request)

        val foedselsdato = PdlParser.utledFødselsdato(response.data?.hentPerson?.foedselsdato)
            ?: return null

        val folkeregisterStatuser = requireNotNull(response.data?.hentPerson?.folkeregisterpersonstatus?.map {
            FolkeregisterStatus(it.status, it.folkeregistermetadata?.gyldighetstidspunkt?.toLocalDate(), it.folkeregistermetadata?.opphoerstidspunkt?.toLocalDate())
        })
        val statsborgerskap = requireNotNull(response.data?.hentPerson?.statsborgerskap?.map {
            Statsborgerskap(
                land = it.land,
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed
            )
        })

        return PersonopplysningMedHistorikk(
            id = 0, // Setter no bs her for å få det gjennom
            fødselsdato = foedselsdato,
            dødsdato = response.data?.hentPerson?.doedsfall?.firstOrNull()?.doedsdato?.let { Dødsdato.parse(it) },
            statsborgerskap = statsborgerskap,
            folkeregisterStatuser = folkeregisterStatuser,
            utenlandsAddresser = mapUtenlandsAdresser(response.data?.hentPerson)
        )
    }
}

private fun mapUtenlandsAdresser(personInfo: PdlPersoninfo?): List<UtenlandsAdresse>? {
    if (personInfo == null) return null

    val bostedsAdresser = personInfo.bostedsadresse.orEmpty().mapNotNull {
        it.utenlandskAdresse?.let { adresse ->
            UtenlandsAdresse(
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
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
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
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
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    gyldigTilOgMed = it.gyldigTilOgMed,
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
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
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


private const val ident = "\$ident"

val PERSON_QUERY = """
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

val PERSON_QUERY_HISTORIKK = """
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