package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
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

data class ArenaSykdomsvurdering(
    val vedtakId: Int,
    val begrunnelse: String?,
    val vilkar: List<ArenaVilkar>,
    val diagnoser: List<ArenaDiagnose>,
)

data class ArenaVilkar(
    val id: Long,
    val kode: String,
    val status: String,
    val begrunnelse: String?,
)

data class ArenaDiagnose(
    val kodeverk: String,
    val kode: String,
    val type: String,
    val opprettet: LocalDate
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

fun ArenaSykdomsvurderingResponse.tilDomene(): ArenaSykdomsvurdering = ArenaSykdomsvurdering(
    vedtakId = vedtakId,
    begrunnelse = begrunnelse,
    vilkar = vilkar.map { vilkar ->
        ArenaVilkar(
            id = vilkar.id,
            kode = vilkar.kode,
            status = vilkar.status,
            begrunnelse = vilkar.begrunnelse,
        )
    },
    diagnoser = diagnoser.map { diagnose ->
        ArenaDiagnose(
            kodeverk = diagnose.kodeverk,
            kode = diagnose.kode,
            type = diagnose.type,
            opprettet = diagnose.opprettet,
        )
    },
)

fun HarHistorikkResponse.tilDomene(): ArenaHistorikk = ArenaHistorikk(harHistorikk = harHistorikk)
