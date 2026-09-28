package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

private val PÅKREVDE_VILKÅR_FOR_ORDINÆR_AAP = setOf("INNTNEDS", "SYKSKADLYT", "AAARBEVNE")

/**
 * Migreringsgruppe 1 støtter kun ordinær AAP. Dette er oppfylt når alle disse
 * vilkårene er oppfylt i Arena: INNTNEDS (inntekt/nedsatt), SYKSKADLYT (sykdom/
 * skade/lyte) og AAARBEVNE (nedsatt arbeidsevne).
 */
fun ArenaSykdomsvurderingResponse.erOrdinærAap(): Boolean {
    return PÅKREVDE_VILKÅR_FOR_ORDINÆR_AAP.all { påkrevdKode ->
        vilkar.any { it.kode == påkrevdKode && it.status == "J" }
    }
}

/**
 * Valgene som gjøres her er dokumentert på confluence:
 * https://confluence.adeo.no/spaces/PAAP/pages/837399601/Avklaringer+og+veivalg+i+migrering
 */
object ArenaMigreringMapper {
    /**
     * Vurderingen antas alltid å gjelde ordinær AAP. Kalleren må ha sjekket
     * [erOrdinærAap] først (migreringsgruppe 1).
     */
    fun mapOppfyltOrdinærSykdomsvurdering(
        fraArena: ArenaSykdomsvurderingResponse,
        behandlingId: BehandlingId,
        vurderingenGjelderFra: LocalDate,
    ): Sykdomsvurdering {
        // TODO denne mappingen er ikke landet
        val hoveddiagnose = requireNotNull(
            fraArena.diagnoser.sortedBy { it.opprettet }.lastOrNull { it.type == ArenaDiagnoseType.HOVEDDIAGNOSE }
        ) { "Fant ingen hoveddiagnose i sykdomsvurdering fra Arena" }
        val bidiagnoser = fraArena.diagnoser.filter { it.type == ArenaDiagnoseType.BIDIAGNOSE }

        val begrunnelse = "Automatisk migrert fra Arena\n\n${fraArena.begrunnelse}"

        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            vurderingenGjelderFra = vurderingenGjelderFra,
            vurderingenGjelderTil = null,
            diagnose = Diagnose(
                kodeverk = hoveddiagnose.kodeverk,
                hoveddiagnose = hoveddiagnose.kode,
                bidiagnoser = bidiagnoser.map { it.kode }
            ),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            yrkesskadeBegrunnelse = null,
            vurdertAv = SYSTEMBRUKER,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )
    }

    /**
     * Vurderingen av bistandsbehov finnes ikke i Arena. Ved ordinær AAP skal dette
     * vilkåret være oppfylt. I migrering gjør vi antagelse om at det oppfylles ved
     * bokstav a (behov for aktiv behandling) og b (behov for arbeidsrettet tiltak).
     */
    fun mapOppfyltBistandsvurdering(
        behandlingId: BehandlingId,
        fom: LocalDate,
    ): Bistandsvurdering {
        val begrunnelse = "Ikke vurdert i Arena, men oppfylles automatisk ved at bruker har ordinær AAP i Arena på migreringstidspunktet"

        return Bistandsvurdering(
            begrunnelse = begrunnelse,
            fom = fom,
            tom = null,
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = true,
            erBehovForAnnenOppfølging = null,
            overgangBegrunnelse = null,
            skalVurdereAapIOvergangTilArbeid = null,
            vurdertAv = SYSTEMBRUKER,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )
    }
}
