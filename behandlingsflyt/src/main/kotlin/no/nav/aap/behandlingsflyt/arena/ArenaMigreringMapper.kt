package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
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
fun ArenaSykdomsvurdering.erOrdinærAap(): Boolean {
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
     * Kun ordinær AAP støttes for migreringsgruppe 1.
     */
    fun mapMigrertKrav(
        fraArena: ArenaKrav,
        behandlingId: BehandlingId,
    ): MigrertKrav {
        return MigrertKrav(
            referanse = Kravreferanse.ny(),
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Migrering av sak ${fraArena.arenaSaksnummer} fra Arena",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            virkningstidspunktArena = fraArena.søknadsdato,
            muligRettFra = fraArena.migreringsdato,
            arenaSaksnummer = fraArena.arenaSaksnummer,
            rettighetstype = MigrertRettighetstype.ORDINÆR,
            resterendeKvoteOrdinær = fraArena.gjenståendeKvoteOrdinær ?: 0,
        )
    }

    /**
     * Vurderingen antas alltid å gjelde ordinær AAP. Kalleren må ha sjekket
     * [erOrdinærAap] først (migreringsgruppe 1).
     */
    fun mapOppfyltOrdinærSykdomsvurdering(
        fraArena: ArenaSykdomsvurdering,
        behandlingId: BehandlingId,
        vurderingenGjelderFra: LocalDate,
    ): Sykdomsvurdering {
        // TODO denne mappingen er ikke landet
        val hoveddiagnose = requireNotNull(
            fraArena.diagnoser.sortedBy { it.opprettet }.lastOrNull { it.type == "HOVED" }
        ) { "Fant ingen hoveddiagnose i sykdomsvurdering fra Arena" }

        require(hoveddiagnose.kodeverk in listOf("ICPC-2", "ICD-10")) {
            "Hoveddiagnose har ikke støttet kodeverk i sykdomsvurdering fra Arena, støtter kun ICPC-2 og ICD-10"
        }

        val bidiagnoser = fraArena.diagnoser.filter { it.type == "BI" }

        require(bidiagnoser.all { it.kodeverk == hoveddiagnose.kodeverk }) {
            "Bidiagnoser har ikke samme kodeverk som hoveddiagnose i sykdomsvurdering fra Arena, dette er ikke støttet enda"
        }

        // Kodeverk i Kelvin har ikke bindestrek, men Arena har det. F.eks. "ICPC-2" vs "ICPC2"
        val kodeverk = hoveddiagnose.kodeverk.replace("-", "")

        val begrunnelse = fraArena.begrunnelse ?: "Automatisk migrert fra Arena"

        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            vurderingenGjelderFra = vurderingenGjelderFra,
            vurderingenGjelderTil = null,
            diagnose = Diagnose(
                kodeverk = kodeverk,
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
