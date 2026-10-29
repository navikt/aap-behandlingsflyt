package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaBeregningstidspunktResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaForutgaaendeMedlemskapResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVedtaksperiodeResponse
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
    val vilkår: List<ArenaVilkår>,
    val diagnoser: List<ArenaDiagnose>,
)

data class ArenaOppholdskrav(
    val vedtakId: Int,
    val vilkår: ArenaVilkår?,
)

data class ArenaForutgåendeMedlemskap(
    val vedtakId: Int,
    val vilkår: ArenaVilkår?,
)

data class ArenaVedtaksperiode(
    val vedtakId: Int,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)

data class ArenaVilkår(
    val id: Long,
    val kode: String,
    val status: String,
    val begrunnelse: String?,
)

data class ArenaBeregningstidspunkt(
    val beregningstidspunkt: LocalDate,
)

data class ArenaDiagnose(
    val kodeverk: String,
    val kode: String,
    val type: String,
    val opprettet: LocalDate
)

data class ArenaRefusjonskrav(
    val aarsak: String,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
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
    vilkår = vilkar.map { vilkar ->
        ArenaVilkår(
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

fun ArenaOppholdskravResponse.tilDomene(): ArenaOppholdskrav = ArenaOppholdskrav(
    vedtakId = vedtakId,
    vilkår = vilkar?.let {
        ArenaVilkår(
            id = it.id,
            kode = it.kode,
            status = it.status,
            begrunnelse = it.begrunnelse,
        )
    },
)

fun ArenaForutgaaendeMedlemskapResponse.tilDomene(): ArenaForutgåendeMedlemskap = ArenaForutgåendeMedlemskap(
    vedtakId = vedtakId,
    vilkår = vilkar?.let {
        ArenaVilkår(
            id = it.id,
            kode = it.kode,
            status = it.status,
            begrunnelse = it.begrunnelse,
        )
    },
)

fun ArenaVedtaksperiodeResponse.tilDomene(): ArenaVedtaksperiode = ArenaVedtaksperiode(
    vedtakId = vedtakId,
    fraDato = fraDato,
    tilDato = tilDato,
)

fun ArenaRefusjonskravResponse.tilDomene(): ArenaRefusjonskrav? =
    refusjonskrav?.let {
        ArenaRefusjonskrav(
            aarsak = it.aarsak,
            fraDato = it.fraDato,
            tilDato = it.tilDato
        )
    }

fun ArenaBeregningstidspunktResponse.tilDomene(): ArenaBeregningstidspunkt? =
    beregningstidspunkt?.let { ArenaBeregningstidspunkt(beregningstidspunkt = it) }

fun HarHistorikkResponse.tilDomene(): ArenaHistorikk = ArenaHistorikk(harHistorikk = harHistorikk)