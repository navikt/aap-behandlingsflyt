package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ReduksjonAktivitetspliktVurdering.Vilkårsvurdering.VILKÅR_FOR_REDUKSJON_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn.INGEN_GYLDIG_GRUNN
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_TILTAK
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.HendelseId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.InnsendingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class ReduksjonFraværFastsattAktivitetRegelTest {
    @Test
    fun `ingen brudd, ingen vurderinger`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31))
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `vurderinger fra paragraf 11_8 og 11_7 blir ikke med`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_8,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 1, 2)
            ),
            brudd(
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                periode = Periode(dato(2021, 1, 1), dato(2021, 1, 1)),
                opprettet = dato(2020, 1, 2)
            ),
        )
        assertEquals(0, vurderinger.segmenter().size)
    }

    @Test
    fun `brudd uten gyldig grunn blir sanksjonert`() {
        val vurderinger = vurder(
            rettighetsperiode = Periode(fom = dato(2020, 1, 1), tom = dato(2022, 12, 31)),
            brudd(
                bruddType = IKKE_MØTT_TIL_TILTAK,
                paragraf = PARAGRAF_11_9,
                periode = Periode(dato(2020, 1, 1), dato(2020, 1, 1)),
                opprettet = dato(2020, 4, 1),
            ),
        )
        assertEquals(1, vurderinger.segmenter().size)

        /* Kan sanksjonere med 11-9 fordi 2020-04-01 ikke er senere enn tre månder fra 2020-01-01, men
         * er nøyaktigt 3 måneder etter. */
        /* Kan ikke sanksjonere med 11-8 på grunn av regelen om én dags fravær i meldeperioden. */
        val vurdering = vurderinger.segment(dato(2020, 1, 1))!!.verdi
        assertEquals(VILKÅR_FOR_REDUKSJON_OPPFYLT ,vurdering.vilkårsvurdering)
    }

    private fun dato(år: Int, mnd: Int, dag: Int) = LocalDate.of(år, mnd, dag)

    private fun vurder(
        rettighetsperiode: Periode,
        vararg aktivitetspliktDokument: AktivitetspliktDokument,
    ): Tidslinje<ReduksjonAktivitetspliktVurdering> {
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            aktivitetspliktDokument = aktivitetspliktDokument.toSet(),
        )
        val vurdering = ReduksjonAktivitetspliktRegel().vurder(input, Tidslinje())
        return vurdering.mapValue { it.reduksjonAktivitetspliktVurdering!! }
    }

    private fun underveisInput(
        rettighetsperiode: Periode,
        aktivitetspliktDokument: Set<AktivitetspliktDokument> = setOf(),
    ) = tomUnderveisInput.copy(
        rettighetsperiode = rettighetsperiode,
        aktivitetspliktGrunnlag = AktivitetspliktGrunnlag(aktivitetspliktDokument),
    )

    private fun brudd(
        bruddType: BruddType,
        paragraf: Brudd.Paragraf,
        periode: Periode,
        opprettet: LocalDate = periode.tom.plusMonths(4),
        grunn: Grunn = INGEN_GYLDIG_GRUNN,
    ) = AktivitetspliktRegistrering(
        brudd = Brudd(
            sakId = SakId(1),
            bruddType = bruddType,
            paragraf = paragraf,
            periode = periode,
        ),
        metadata = AktivitetspliktDokument.Metadata(
            id = BruddAktivitetspliktId(0),
            hendelseId = HendelseId.ny(),
            innsendingId = InnsendingId.ny(),
            innsender = NavIdent(""),
            opprettetTid = opprettet.atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant(),
        ),
        begrunnelse = "Informasjon fra tiltaksarrangør",
        grunn = grunn,
    )
}