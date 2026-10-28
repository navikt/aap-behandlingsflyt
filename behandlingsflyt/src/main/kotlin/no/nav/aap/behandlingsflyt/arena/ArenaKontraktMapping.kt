package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoMedVedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaBeregningstidspunktResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVedtaksperiodeResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
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

data class ArenaOppholdskrav(
    val vedtakId: Int,
    val vilkar: ArenaVilkar?,
)

data class ArenaVedtaksperiode(
    val vedtakId: Int,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)

data class ArenaVilkar(
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

fun ArenaOppholdskravResponse.tilDomene(): ArenaOppholdskrav = ArenaOppholdskrav(
    vedtakId = vedtakId,
    vilkar = vilkar?.let {
        ArenaVilkar(
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

// Samme valg av maksdato som MaksdatoSamordningResponse.from, men status STANSET gir ikke null her; status lagres separat.
fun MaksdatoMedVedtakResponse.tilDomene(): ArenaMaksdato? = sak?.let {
    ArenaMaksdato(
        arenaSaksnummer = it.saknummer,
        status = it.sakStatus,
        maksdato = it.sisteVedtak.maxdatoUnntak ?: it.sisteVedtak.maxdatoOrdinaer,
        vedtakTil = it.sisteVedtak.til,
        vedtakId = it.sisteVedtak.vedtakId,
    )
}
