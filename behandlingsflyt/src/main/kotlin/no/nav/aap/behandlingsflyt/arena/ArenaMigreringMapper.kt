package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.ARENA_MIGRERING_BRUKER
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravPeriode
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ÅrsakBeregningstidspunkt
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.komponenter.type.Periode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

private const val VILKÅRKODE_REFUSJONSKRAV_SOSIALHJELP = "REFKRAVSOS"
private const val VILKÅRKODE_OPPHOLD_I_NORGE = "AOINORGE3"
private const val VILKÅRKODE_FORUTGÅENDE_MEDLEMSKAP = "MEDLSKAP5"
private const val VILKÅRKODE_INNTEKT_NEDSATT = "INNTNEDS"
private const val VILKÅRKODE_SYKDOM_SKADE_LYTE = "SYKSKADLYT"
private const val VILKÅRKODE_ARBEIDSEVNE = "AAARBEVNE"

private val PÅKREVDE_VILKÅR_FOR_ORDINÆR_AAP = setOf(
    VILKÅRKODE_INNTEKT_NEDSATT,
    VILKÅRKODE_SYKDOM_SKADE_LYTE,
    VILKÅRKODE_ARBEIDSEVNE
)

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

fun ArenaOppholdskrav.erOppfyltEllerIkkeVurdert(): Boolean {
    return vilkar?.kode == VILKÅRKODE_OPPHOLD_I_NORGE && vilkar.status in setOf("J", "V")
}

fun ArenaForutgåendeMedlemskap.erOppfylt(): Boolean {
    return vilkar?.kode == VILKÅRKODE_FORUTGÅENDE_MEDLEMSKAP && vilkar.status == "J"
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
            vurdertAv = ARENA_MIGRERING_BRUKER,
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
            vurdertAv = ARENA_MIGRERING_BRUKER,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )
    }

    /**
     * Oppholdskravet er enten oppfylt eller ikke vurdert i Arena. I migrering gjør vi antagelse om at det oppfylles da
     * det er et krav for å få innvilget AAP i Arena.
     */
    fun mapOppfyltOppholdskravVurdering(
        fraArena: ArenaOppholdskrav,
        behandlingId: BehandlingId,
        fom: LocalDate,
    ): OppholdskravVurdering {
        require(fraArena.erOppfyltEllerIkkeVurdert()) {
            "Kan ikke mappe oppholdskrav med vilkårkode ${fraArena.vilkar?.kode} og status ${fraArena.vilkar?.status}"
        }
        val status = fraArena.vilkar?.status
        val begrunnelse = when (status) {
            "J" -> "Oppholdskravet er oppfylt i Arena"
            "V" -> "Oppholdskravet er ikke vurdert i Arena, " +
                    "men oppfylles automatisk fordi det må være oppfylt for at vedtaket skal være innvilget i Arena"
            else -> error("Uventet status etter validering: $status")
        }

        return OppholdskravVurdering(
            vurdertAv = ARENA_MIGRERING_BRUKER,
            vurdertIBehandling = behandlingId,
            perioder = listOf(
                OppholdskravPeriode(
                    fom = fom,
                    tom = null,
                    land = null,
                    oppfylt = true,
                    begrunnelse = begrunnelse,
                )
            ),
        )
    }

    /**
     * Kun oppfylt forutgående medlemskap (status J) kan migreres. Status V (ikke vurdert) betyr at vilkåret ikke er
     * oppfylt, i motsetning til oppholdskrav.
     */
    fun mapOppfyltForutgåendeMedlemskapVurdering(
        fraArena: ArenaForutgåendeMedlemskap,
        behandlingId: BehandlingId,
        fom: LocalDate,
    ): ManuellVurderingForForutgåendeMedlemskap {
        require(fraArena.erOppfylt()) {
            "Kan ikke mappe forutgående medlemskap med vilkårkode ${fraArena.vilkar?.kode} og status ${fraArena.vilkar?.status}"
        }
        return ManuellVurderingForForutgåendeMedlemskap(
            begrunnelse = "Forutgående medlemskap er oppfylt i Arena",
            harForutgåendeMedlemskap = true,
            varMedlemMedNedsattArbeidsevne = null,
            medlemMedUnntakAvMaksFemAar = null,
            vurdertAv = ARENA_MIGRERING_BRUKER,
            vurdertTidspunkt = LocalDateTime.now(),
            vurdertIBehandling = behandlingId,
            fom = fom,
            tom = null,
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
            vurdertAv = ARENA_MIGRERING_BRUKER,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )
    }

    /**
     * Refusjonskrav av typen REFKRAVSOS støttes ikke i migreringsgruppe 1 og gir feil. Hvis det ikke finnes
     * refusjonskrav, eller kravet har en annen årsak, svares "nei" på om det finnes refusjonskrav.
     */
    fun mapRefusjonskravVurdering(arenaRefusjonskrav: ArenaRefusjonskrav?): RefusjonkravVurdering {
        require(arenaRefusjonskrav?.aarsak != VILKÅRKODE_REFUSJONSKRAV_SOSIALHJELP) {
            "Kan ikke migrere refusjonskrav fra Arena fordi det finnes refusjonskrav av typen $VILKÅRKODE_REFUSJONSKRAV_SOSIALHJELP"
        }
        return RefusjonkravVurdering(
            harKrav = false,
            navKontor = null,
            vurdertAv = ARENA_MIGRERING_BRUKER,
            opprettetTid = LocalDateTime.now()
        )
    }

    fun mapBeregningstidspunktVurdering(fraArena: ArenaBeregningstidspunkt): BeregningstidspunktVurdering {
        return BeregningstidspunktVurdering(
            begrunnelse = "Migrert fra Arena",
            nedsattArbeidsevneEllerStudieevneDato = fraArena.beregningstidspunkt,
            ytterligereNedsattBegrunnelse = null,
            ytterligereNedsattArbeidsevneDato = null,
            vurdertAv = ARENA_MIGRERING_BRUKER,
            vurdertTidspunkt = LocalDateTime.now(),
            årsak = ÅrsakBeregningstidspunkt.ANNET,
            ytterligereNedsattÅrsak = null,
        )
    }

    fun mapVedtakslengdeVurdering(
        fraArena: ArenaVedtaksperiode,
        behandlingId: BehandlingId,
    ): VedtakslengdeVurdering {
        val tilDato = requireNotNull(fraArena.tilDato) {
            "Kan ikke migrere vedtakslengde fra Arena fordi vedtaksperioden mangler tilDato"
        }

        return VedtakslengdeVurdering(
            sluttdato = tilDato,
            utvidetMed = ÅrMedHverdager.FØRSTE_ÅR, // Gyldig for migreringsgruppe 1
            årsaker = listOf(VedtakslengdeÅrsak.MAKS_ETT_ÅR),  // Gyldig for migreringsgruppe 1
            vurdertAv = ARENA_MIGRERING_BRUKER,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            begrunnelse = "Sluttdato for vedtaket er migrert fra Arena",
        )
    }
}
