package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import java.time.LocalDate

data class ArenaKrav(
    val arenaSaksnummer: String,
    val søknadsdato: LocalDate,
    val migreringsdato: LocalDate,
    val gjenståendeKvoteOrdinær: Int?,
)

data class ArenaSak(
    val saksnummer: String,
    val statuskode: String,
)

data class ArenaHistorikk(
    val harHistorikk: Boolean,
)

fun KravResponse.tilDomene(): ArenaKrav {
    val arenaSaksnummer = "$aar-$lopenr"
    return ArenaKrav(
        arenaSaksnummer = arenaSaksnummer,
        søknadsdato = requireNotNull(soknadsdato) {
            "Kan ikke migrere krav for Arena-sak $arenaSaksnummer. Mangler søknadsdato fra Arena."
        },
        migreringsdato = requireNotNull(migreringsdato) {
            "Kan ikke migrere krav for Arena-sak $arenaSaksnummer. Mangler migreringsdato fra Arena."
        },
        gjenståendeKvoteOrdinær = gjenstaaendeKvote.ordinaer,
    )
}

fun ArenaSakOppsummeringKontrakt.tilDomene(): ArenaSak = ArenaSak(
    saksnummer = "$aar-$lopenummer",
    statuskode = statuskode,
)

fun HarHistorikkResponse.tilDomene(): ArenaHistorikk = ArenaHistorikk(harHistorikk = harHistorikk)
