package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AktivitetspliktVurdering.Vilkårsvurdering.AKTIVT_BIDRAG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.STANS_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.FORELDET
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.UNNTAK_RIMELIG_GRUNN
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_FOR_REDUKSJON_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.HendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class VurderingTest {
    val dokument = AktivitetspliktRegistrering(
        metadata = AktivitetspliktDokument.Metadata(
            id = BruddAktivitetspliktId(0),
            hendelseId = HendelseId.ny(),
            innsendingId = InnsendingId.ny(),
            innsender = Bruker("Z000000"),
            opprettetTid = Instant.now(),
        ),
        brudd = Brudd(
            periode = Periode(LocalDate.EPOCH, LocalDate.EPOCH),
            bruddType = BruddType.IKKE_AKTIVT_BIDRAG,
            paragraf = Brudd.Paragraf.PARAGRAF_11_7,
        ),
        begrunnelse = "",
        grunn = Grunn.INGEN_GYLDIG_GRUNN,
    )

    @Test
    fun `Paragraf 11-7 har høyere prioritet enn 11-8 og 11-9`() {
        val vurdering = Vurdering(
            fårAapEtter = RettighetsType.BISTANDSBEHOV,
            aktivitetspliktVurdering = AktivitetspliktVurdering(
                dokument = dokument,
                vilkårsvurdering = AKTIVT_BIDRAG_IKKE_OPPFYLT
            ),
            fraværFastsattAktivitetVurdering = FraværFastsattAktivitetVurdering(
                dokument = dokument,
                vilkårsvurdering = STANS_ANDRE_DAG,
            ),
            reduksjonAktivitetspliktVurdering = ReduksjonAktivitetspliktVurdering(
                dokument = dokument,
                vilkårsvurdering = VILKÅR_FOR_REDUKSJON_OPPFYLT,
            ),
        )

        assertFalse(vurdering.harRett())
        assertEquals(BRUDD_PÅ_AKTIVITETSPLIKT, vurdering.avslagsårsak())
        assertFalse(vurdering.skalReduseresDagsatser())
    }

    @Test
    fun `Paragraf 11-8 har høyere prioritet enn 11-9`() {
        for (vilkårsvurdering in listOf(STANS_ANDRE_DAG, STANS_TI_DAGER_BRUKT_OPP)) {
            val vurdering = Vurdering(
                fårAapEtter = RettighetsType.BISTANDSBEHOV,
                fraværFastsattAktivitetVurdering = FraværFastsattAktivitetVurdering(
                    dokument = dokument,
                    vilkårsvurdering = STANS_ANDRE_DAG,
                ),
                reduksjonAktivitetspliktVurdering = ReduksjonAktivitetspliktVurdering(
                    dokument = dokument,
                    vilkårsvurdering = VILKÅR_FOR_REDUKSJON_OPPFYLT,
                ),
            )

            assertFalse(vurdering.harRett())
            assertEquals(UnderveisÅrsak.FRAVÆR_FASTSATT_AKTIVITET, vurdering.avslagsårsak())
            assertFalse(vurdering.skalReduseresDagsatser())
        }
    }

    @Test
    fun `Paragraf 11-8 har høyere prioritet enn 11-9 også ved unntak`() {
        for (vilkårsvurdering in listOf(
            UNNTAK_INNTIL_EN_DAG,
            UNNTAK_STERKE_VELFERDSGRUNNER,
            UNNTAK_SYKDOM_ELLER_SKADE
        )) {
            val vurdering = Vurdering(
                fårAapEtter = RettighetsType.BISTANDSBEHOV,
                fraværFastsattAktivitetVurdering = FraværFastsattAktivitetVurdering(
                    dokument = dokument,
                    vilkårsvurdering = vilkårsvurdering,
                ),
                reduksjonAktivitetspliktVurdering = ReduksjonAktivitetspliktVurdering(
                    dokument = dokument,
                    vilkårsvurdering = VILKÅR_FOR_REDUKSJON_OPPFYLT,
                ),
            )

            assertTrue(vurdering.harRett())
            assertNull(vurdering.avslagsårsak())
            assertFalse(vurdering.skalReduseresDagsatser())
        }
    }

    @Test
    fun `Paragraf 11-9 kan slå til`() {
        val vurdering = Vurdering(
            fårAapEtter = RettighetsType.BISTANDSBEHOV,
            reduksjonAktivitetspliktVurdering = ReduksjonAktivitetspliktVurdering(
                dokument = dokument,
                vilkårsvurdering = VILKÅR_FOR_REDUKSJON_OPPFYLT,
            ),
        )

        assertTrue(vurdering.harRett())
        assertNull(vurdering.avslagsårsak())
        assertTrue(vurdering.skalReduseresDagsatser())
    }

    @Test
    fun `Paragraf 11-9 gir ikke reduksjon hvis vilkår ikke er oppfylt `() {
        for (vilkårsvurdering in listOf(FORELDET, UNNTAK_RIMELIG_GRUNN)) {
            val vurdering = Vurdering(
                fårAapEtter = RettighetsType.BISTANDSBEHOV,
                reduksjonAktivitetspliktVurdering = ReduksjonAktivitetspliktVurdering(
                    dokument = dokument,
                    vilkårsvurdering = vilkårsvurdering,
                ),
            )

            assertTrue(vurdering.harRett())
            assertNull(vurdering.avslagsårsak())
            assertFalse(vurdering.skalReduseresDagsatser())
        }
    }
}