package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FREMTIDIG_IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.FØR_VEDTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus.IKKE_MELDT_SEG
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType.BISTANDSBEHOV
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class MeldekortTilApiInternJobbUtførerTest {

    private val underveisperioder = listOf<Underveisperiode>(
        underveisperiode("2025-03-31,2025-04-14", "2025-03-31,2025-04-14", IKKE_GRUNNLEGGENDE_RETT, null, FØR_VEDTAK),
        underveisperiode("2025-04-14,2025-04-28", "2025-04-14,2025-04-28", IKKE_GRUNNLEGGENDE_RETT, null, IKKE_MELDT_SEG),
        underveisperiode("2025-04-28,2025-05-12", "2025-04-28,2025-05-12", IKKE_GRUNNLEGGENDE_RETT, null, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-05-12,2025-05-13", "2025-05-12,2025-05-26", IKKE_GRUNNLEGGENDE_RETT, null, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-05-13,2025-05-26", "2025-05-12,2025-05-26", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-05-26,2025-06-09", "2025-05-26,2025-06-09", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-06-09,2025-06-23", "2025-06-09,2025-06-23", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-06-23,2025-07-07", "2025-06-23,2025-07-07", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-07-07,2025-07-21", "2025-07-07,2025-07-21", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-07-21,2025-08-04", "2025-07-21,2025-08-04", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-08-04,2025-08-18", "2025-08-04,2025-08-18", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-08-18,2025-09-01", "2025-08-18,2025-09-01", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-09-01,2025-09-15", "2025-09-01,2025-09-15", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-09-15,2025-09-29", "2025-09-15,2025-09-29", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-09-29,2025-10-13", "2025-09-29,2025-10-13", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-10-13,2025-10-27", "2025-10-13,2025-10-27", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-10-27,2025-11-10", "2025-10-27,2025-11-10", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-11-10,2025-11-24", "2025-11-10,2025-11-24", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-11-24,2025-12-08", "2025-11-24,2025-12-08", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-12-08,2025-12-22", "2025-12-08,2025-12-22", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2025-12-22,2026-01-05", "2025-12-22,2026-01-05", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-01-05,2026-01-19", "2026-01-05,2026-01-19", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-01-19,2026-02-02", "2026-01-19,2026-02-02", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-02-02,2026-02-16", "2026-02-02,2026-02-16", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-02-16,2026-03-02", "2026-02-16,2026-03-02", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-03-02,2026-03-16", "2026-03-02,2026-03-16", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-03-16,2026-03-30", "2026-03-16,2026-03-30", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
        underveisperiode("2026-03-30,2026-03-31", "2026-03-30,2026-04-13", MELDEPLIKT_FRIST_IKKE_PASSERT, BISTANDSBEHOV, FREMTIDIG_IKKE_OPPFYLT),
    )

    @Test
    fun `send et meldekort-detalj`() {
        val opplysninger = MeldeperiodeTilMeldekortBackendJobbUtfører.opplysningerVedVedtak(
            sak = Sak(
                id = SakId(0),
                saksnummer = Saksnummer("s1"),
                person = Person(
                    UUID.randomUUID(), listOf(
                        Ident("1".repeat(11), aktivIdent = false),
                        Ident("2".repeat(11), aktivIdent = true)
                    )
                ),
                rettighetsperiode = Periode(LocalDate.parse("2025-03-31"), LocalDate.parse("2026-03-30")),
                status = Status.UTREDES,
            ),
            meldeperioder = underveisperioder.map { it.meldePeriode }.toSet().sorted(),
            vedtak = Vedtak(
                behandlingId = BehandlingId(0),
                vedtakstidspunkt = LocalDateTime.parse("2025-05-05T10:43:44.561"),
                virkningstidspunkt = LocalDate.parse("2025-05-13"),
            ),
            meldepliktGrunnlag = MeldepliktGrunnlag(emptyList()),
            underveisGrunnlag = UnderveisGrunnlag(
                id = 0,
                perioder = underveisperioder,
            )
        )

        assertEquals(opplysninger.identer.toSet(), setOf("1".repeat(11), "2".repeat(11)))
        assertEquals(opplysninger.opplysningsbehov.single().fom, 13 mai 2025)
        assertEquals(opplysninger.opplysningsbehov.single().tom, 30 mars 2026)
        assertEquals(opplysninger.meldeperioder.map { Periode(it.fom, it.tom) }.toSet(), setOf(
            Periode(LocalDate.parse("2025-03-31"), LocalDate.parse("2025-04-13")),
            Periode(LocalDate.parse("2025-04-14"), LocalDate.parse("2025-04-27")),
            Periode(LocalDate.parse("2025-04-28"), LocalDate.parse("2025-05-11")),
            Periode(LocalDate.parse("2025-05-12"), LocalDate.parse("2025-05-25")),
            Periode(LocalDate.parse("2025-05-12"), LocalDate.parse("2025-05-25")),
            Periode(LocalDate.parse("2025-05-26"), LocalDate.parse("2025-06-08")),
            Periode(LocalDate.parse("2025-06-09"), LocalDate.parse("2025-06-22")),
            Periode(LocalDate.parse("2025-06-23"), LocalDate.parse("2025-07-06")),
            Periode(LocalDate.parse("2025-07-07"), LocalDate.parse("2025-07-20")),
            Periode(LocalDate.parse("2025-07-21"), LocalDate.parse("2025-08-03")),
            Periode(LocalDate.parse("2025-08-04"), LocalDate.parse("2025-08-17")),
            Periode(LocalDate.parse("2025-08-18"), LocalDate.parse("2025-08-31")),
            Periode(LocalDate.parse("2025-09-01"), LocalDate.parse("2025-09-14")),
            Periode(LocalDate.parse("2025-09-15"), LocalDate.parse("2025-09-28")),
            Periode(LocalDate.parse("2025-09-29"), LocalDate.parse("2025-10-12")),
            Periode(LocalDate.parse("2025-10-13"), LocalDate.parse("2025-10-26")),
            Periode(LocalDate.parse("2025-10-27"), LocalDate.parse("2025-11-09")),
            Periode(LocalDate.parse("2025-11-10"), LocalDate.parse("2025-11-23")),
            Periode(LocalDate.parse("2025-11-24"), LocalDate.parse("2025-12-07")),
            Periode(LocalDate.parse("2025-12-08"), LocalDate.parse("2025-12-21")),
            Periode(LocalDate.parse("2025-12-22"), LocalDate.parse("2026-01-04")),
            Periode(LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-18")),
            Periode(LocalDate.parse("2026-01-19"), LocalDate.parse("2026-02-01")),
            Periode(LocalDate.parse("2026-02-02"), LocalDate.parse("2026-02-15")),
            Periode(LocalDate.parse("2026-02-16"), LocalDate.parse("2026-03-01")),
            Periode(LocalDate.parse("2026-03-02"), LocalDate.parse("2026-03-15")),
            Periode(LocalDate.parse("2026-03-16"), LocalDate.parse("2026-03-29")),
            Periode(LocalDate.parse("2026-03-30"), LocalDate.parse("2026-04-12")),
        ))
        assertEquals(opplysninger.meldeplikt.map { Periode(it.fom, it.tom) }.toSet(), setOf<Periode>(
            LocalDate.parse("2025-05-26").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-06-09").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-06-23").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-07-07").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-07-21").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-08-04").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-08-18").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-09-01").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-09-15").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-09-29").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-10-13").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-10-27").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-11-10").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-11-24").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-12-08").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2025-12-22").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-01-05").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-01-19").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-02-02").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-02-16").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-03-02").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-03-16").let { Periode(it, it.plusDays(7))},
            LocalDate.parse("2026-03-30").let { Periode(it, it.plusDays(7))},

            ))
    }

    private var id: Long = 0

    private fun underveisperiode(
        periode: String,
        meldeperiode: String,
        avslagsårsak: UnderveisÅrsak?,
        rettighetstype: RettighetsType?,
        meldepliktStatus: MeldepliktStatus,
    ) = Underveisperiode(
        periode = parse(periode),
        meldePeriode = parse(meldeperiode),
        utfall = IKKE_OPPFYLT,
        rettighetsType = rettighetstype,
        avslagsårsak = avslagsårsak,
        grenseverdi = Prosent(60),
        institusjonsoppholdReduksjon = Prosent(0),
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(0)),
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`0_PROSENT`,
            gradering = Prosent.`0_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        bruddAktivitetspliktId = null,
        meldepliktStatus = meldepliktStatus,
        id = UnderveisperiodeId(id.also { id += 1 }),
    )

    private fun parse(periode: String): Periode {
        val (start, slutt) = periode.split(",")
        return Periode(LocalDate.parse(start), LocalDate.parse(slutt).minusDays(1))
    }

}