package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.HendelseId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Paragraf
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Paragraf.PARAGRAF_11_9
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period

class AktivitetspliktRegelTest {
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
        assertEquals(listOf(PARAGRAF_11_9), vurdering.muligeSanksjoner)
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
        assertEquals(listOf(PARAGRAF_11_9), vurdering.muligeSanksjoner)
    }

    @Test
    fun `andre dag i meldeperiode kan føre til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 2)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(listOf(PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 1))!!.verdi.muligeSanksjoner)
        assertEquals(listOf(PARAGRAF_11_8, PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 2))!!.verdi.muligeSanksjoner)
    }

    @Test
    fun `to fravær i hver sin meldeperiode fører ikke til § 11-8 stans`() {
        val vurdering = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
            ),
            brudd(
                aktivitetsType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 15), dato(2020, 1, 15)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(listOf(PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 1))!!.verdi.muligeSanksjoner)
        assertEquals(listOf(PARAGRAF_11_9), vurdering.segment(dato(2020, 1, 15))!!.verdi.muligeSanksjoner)
    }

    private fun dato(år: Int, mnd: Int, dag: Int) = LocalDate.of(år, mnd, dag)

    private fun vurder(
        rettighetsperiode: Periode,
        vararg bruddAktivitetsplikt: BruddAktivitetsplikt,
    ): Tidslinje<AktivitetspliktVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            bruddAktivitetsplikt = bruddAktivitetsplikt.toSet(),
        )
        val vurdering = AktivitetspliktRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.aktivitetspliktVurdering!! }
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
        bruddAktivitetsplikt = bruddAktivitetsplikt,
        etAnnetSted = listOf(),
    )

    private fun brudd(
        aktivitetsType: AktivitetType,
        paragraf: Paragraf,
        periode: Periode,
        opprettet: LocalDate = periode.tom.plusMonths(4),
    ) = BruddAktivitetsplikt(
        id = BruddAktivitetspliktId(0),
        hendelseId = HendelseId.ny(),
        innsendingId = InnsendingId.ny(),
        navIdent = NavIdent(""),
        sakId = SakId(1),
        brudd = aktivitetsType,
        paragraf = paragraf,
        begrunnelse = "Informasjon fra tiltaksarrangør",
        periode = periode,
        opprettetTid = opprettet.atStartOfDay(),
    )
}