package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Grunn.STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.HendelseId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period
import no.nav.aap.tidslinje.Segment

class ReduksjonFraværFastsattAktivitetRegelTest {
    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31))
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `ett fravær fra tiltak, kun 11-9 kan brukes`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 1, 2)
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)
        val vurdering = vurderinger.segment(dato(2020, 1, 1))!!.verdi
        /* Kan ikke sanksjonere med 11-8 på grunn av regelen om én dags fravær i meldeperioden. */
        assertTrue(vurdering.kanReduseres)
    }

    @Test
    fun `ett fravær fra tiltak uten grunn, kan ikke sanksjoneres etter tre måneder`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)

        /* Kan sanksjonere med 11-9 fordi 2020-04-01 ikke er senere enn tre månder fra 2020-01-01, men
         * er nøyaktigt 3 måneder etter. */
        /* Kan ikke sanksjonere med 11-8 på grunn av regelen om én dags fravær i meldeperioden. */
        val vurdering = vurderinger.segment(dato(2020, 1, 1))!!.verdi
        assertTrue(vurdering.kanReduseres)
    }

    private fun dato(år: Int, mnd: Int, dag: Int) = LocalDate.of(år, mnd, dag)

    private fun vurder(
        rettighetsperiode: Periode,
        vararg bruddAktivitetsplikt: BruddAktivitetsplikt,
    ): Tidslinje<ReduksjonAktivitetspliktVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            bruddAktivitetsplikt = bruddAktivitetsplikt.toSet(),
        )
        val vurdering = ReduksjonAktivitetspliktRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.reduksjonAktivitetspliktVurdering!! }
    }

    private fun underveisInput(
        rettighetsperiode: Periode,
        bruddAktivitetsplikt: Set<BruddAktivitetsplikt> = setOf(),
    ) = UnderveisInput(
        rettighetsperiode = rettighetsperiode,
        relevanteVilkår = listOf(),
        opptrappingPerioder = listOf(),
        pliktkort = listOf(),
        innsendingsTidspunkt = mapOf(),
        dødsdato = null,
        kvote = Kvote(Period.ofYears(365 * 3)),
        bruddAktivitetsplikt = Tidslinje(bruddAktivitetsplikt.sortedBy { it.periode.fom }.map { Segment(it.periode, it) }),
        etAnnetSted = listOf(),
        barnetillegg = BarnetilleggGrunnlag(1, listOf()),
    )

    private fun brudd(
        aktivitetsType: BruddAktivitetsplikt.Type,
        paragraf: BruddAktivitetsplikt.Paragraf,
        periode: Periode,
        opprettet: LocalDate = periode.tom.plusMonths(4),
        grunn: BruddAktivitetsplikt.Grunn = INGEN_GYLDIG_GRUNN,
    ) = BruddAktivitetsplikt(
        id = BruddAktivitetspliktId(0),
        hendelseId = HendelseId.ny(),
        innsendingId = InnsendingId.ny(),
        navIdent = NavIdent(""),
        sakId = SakId(1),
        type = aktivitetsType,
        paragraf = paragraf,
        begrunnelse = "Informasjon fra tiltaksarrangør",
        periode = periode,
        opprettetTid = opprettet.atStartOfDay(),
        grunn = grunn,
    )
}