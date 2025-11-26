package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.STUDENTKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VarighetRegelTest {
    private val regel = VarighetRegel()

    @Test
    fun `rett alle dager, innenfor en uke, men kvoten blir brukt opp`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(4, 0, 0)
            ),

            Tidslinje(
                rettighetsperiode,
                Vurdering(
                    fårAapEtter = RettighetsType.BISTANDSBEHOV,
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    @Test
    fun `Kvote blir brukt opp på fredag, stans skjer førstkommende mandag`() {
        val rettighetsperiode = Periode(18 november 2024, 25 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(5, 0, 0)
            ),

            Tidslinje(
                rettighetsperiode,
                Vurdering(
                    fårAapEtter = RettighetsType.BISTANDSBEHOV,
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 24 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(25 november 2024, 25 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    @Test
    fun `Helg teller ikke på kvote`() {
        val rettighetsperiode = Periode(22 november 2024, 26 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 0, 0)
            ),

            Tidslinje(
                rettighetsperiode,
                Vurdering(
                    fårAapEtter = RettighetsType.BISTANDSBEHOV,
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(22 november 2024, 25 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(26 november 2024, 26 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    @Test
    fun `Helger etter brukt opp kvote får stans`() {
        val rettighetsperiode = Periode(18 november 2024, 26 januar 2025)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 0, 0)
            ),

            Tidslinje(
                rettighetsperiode,
                Vurdering(
                    fårAapEtter = RettighetsType.BISTANDSBEHOV,
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 19 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(20 november 2024, 26 januar 2025)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }


    @Test
    fun `Stans teller ikke på kvote`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 0, 0)
            ),

            listOf(
                Segment(
                    Periode(18 november 2024, 18 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.BISTANDSBEHOV,
                    )
                ),
                Segment(
                    Periode(19 november 2024, 20 november 2024),
                    Vurdering()
                ),
                Segment(
                    Periode(21 november 2024, 22 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.BISTANDSBEHOV,
                    )
                ),
            ).let { Tidslinje(it) }
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 18 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(19 november 2024, 20 november 2024)) { vurdering ->
                assertEquals(setOf<Kvote>(), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, IKKE_GRUNNLEGGENDE_RETT)
            },
            Segment(Periode(21 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(
                    vurdering,
                    VARIGHETSKVOTE_BRUKT_OPP,
                    ORDINÆRKVOTE_BRUKT_OPP
                )
            },
        )
    }

    @Test
    fun `rett grunnet sykepengeerstatning, så ordinær-kvoten brukes ikke`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 0, 2)
            ),

            Tidslinje(
                listOf(
                    Segment(
                        Periode(18 november 2024, 19 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.SYKEPENGEERSTATNING,
                        )
                    ),
                    Segment(
                        Periode(20 november 2024, 22 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    )
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 19 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.SYKEPENGEERSTATNING), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(20 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(
                    vurdering,
                    VARIGHETSKVOTE_BRUKT_OPP,
                    ORDINÆRKVOTE_BRUKT_OPP
                )
            },
        )
    }

    @Test
    fun `stans grunnet sykepengeerstatning, så ordinær-kvoten brukes ikke`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 0, 0)
            ),

            Tidslinje(
                listOf(
                    Segment(
                        Periode(18 november 2024, 19 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.SYKEPENGEERSTATNING,
                        )
                    ),
                    Segment(
                        Periode(20 november 2024, 22 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    )
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 19 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.SYKEPENGEERSTATNING), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
            Segment(Periode(20 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(
                    vurdering,
                    VARIGHETSKVOTE_BRUKT_OPP,
                    ORDINÆRKVOTE_BRUKT_OPP
                )
            },
        )
    }

    @Test
    fun `kvote brukes ikke ved ingen grunnleggende rett`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 0, 0)
            ),
            Tidslinje(
                listOf(
                    Segment(
                        Periode(18 november 2024, 19 november 2024),
                        Vurdering(
                            fårAapEtter = null,
                        )
                    ),
                    Segment(
                        Periode(20 november 2024, 22 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    )
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 19 november 2024)) { vurdering ->
                assertEquals(setOf<Kvote>(), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(
                    vurdering,
                    IKKE_GRUNNLEGGENDE_RETT
                )
            },
            Segment(Periode(20 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    @Test
    fun `teller kvote riktig hvis input-vurderinger er segmentert opp og stans først dag i et segment`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(3, 0, 0)
            ),
            Tidslinje(
                listOf(
                    Segment(
                        Periode(18 november 2024, 19 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                    Segment(
                        Periode(20 november 2024, 20 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                    Segment(
                        Periode(21 november 2024, 22 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    )
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 20 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(21 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    @Test
    fun `teller kvote riktig hvis input-vurderinger er segmentert opp og stans siste dag i et segment`() {
        val rettighetsperiode = Periode(18 november 2024, 22 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(4, 0, 0)
            ),
            Tidslinje(
                listOf(
                    Segment(
                        Periode(18 november 2024, 19 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                    Segment(
                        Periode(20 november 2024, 20 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                    Segment(
                        Periode(21 november 2024, 22 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    )
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 22 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    /*
        November 2024
     Mo Tu We Th Fr Sa Su
                  1  2  3
      4  5  6  7  8  9 10
     11 12 13 14 15 16 17
     18 19 20 21 22 23 24
     25 26 27 28 29 30
     */
    @Test
    fun `stans grunnet student-kvote, får dermed vanlig 11-5 innvilget`() {
        val rettighetsperiode = Periode(18 november 2024, 26 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(4, 2, 0)
            ),

            listOf(
                Segment(
                    Periode(18 november 2024, 21 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.STUDENT,
                    )
                ),
                Segment(
                    Periode(22 november 2024, 26 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.BISTANDSBEHOV,
                    )
                ),
            ).let(::Tidslinje)
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 19 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR, Kvote.STUDENT), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(20 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR, Kvote.STUDENT), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, STUDENTKVOTE_BRUKT_OPP)
            },
            Segment(Periode(22 november 2024, 25 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(26 november 2024, 26 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    /*
        November 2024
     Mo Tu We Th Fr Sa Su
                  1  2  3
      4  5  6  7  8  9 10
     11 12 13 14 15 16 17
     18 19 20 21 22 23 24
     25 26 27 28 29 30
     */
    @Test
    fun `brukt opp ordinær kvote, kan ikke bruke student-kvote alene`() {
        val rettighetsperiode = Periode(18 november 2024, 26 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(2, 2, 0)
            ),

            listOf(
                Segment(
                    Periode(18 november 2024, 21 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.BISTANDSBEHOV,
                    )
                ),
                Segment(
                    Periode(22 november 2024, 26 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.STUDENT,
                    )
                ),
            ).let(::Tidslinje)
        )

        vurderinger.assertTidslinje(
            Segment(Periode(18 november 2024, 19 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(20 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
            Segment(Periode(22 november 2024, 26 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR, Kvote.STUDENT), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
        )
    }

    /*  November 2024
     * Mo Tu We Th Fr Sa Su
     *              8  9 10
     */
    @Test
    fun `ordinær kvote brukt opp på siste hverdag i uka med påfølgende helg i samme segment`() {
        val rettighetsperiode = Periode(8 november 2024, 10 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(ordinærkvote = 1, studentkvote = 0, 0)
            ),
            Tidslinje(
                Periode(8 november 2024, 10 november 2024),
                Vurdering(
                    fårAapEtter = RettighetsType.BISTANDSBEHOV,
                )
            )
        )

        vurderinger.assertTidslinje(
            Segment(Periode(8 november 2024, 10 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
        )
    }

    /*
        November 2024
     Mo Tu We Th Fr Sa Su
                  1  2  3
      4  5  6  7  8  9 10
     11 12 13 14 15 16 17
     18 19 20 21 22 23 24
     25 26 27 28 29 30
     */
    @Test
    fun `alle måter å dele opp periode i to`() {
        val rettighetsperiode = Periode(11 november 2024, 26 november 2024)
        Periode(rettighetsperiode.fom.plusDays(1), rettighetsperiode.tom).dager().forEach {
            val vurderinger = regel.vurder(
                tomUnderveisInput.copy(
                    periodeForVurdering = rettighetsperiode,
                    kvoter = Kvoter.create(4, 0, 0)
                ),

                listOf(
                    Segment(
                        Periode(11 november 2024, it.minusDays(1)),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                    Segment(
                        Periode(it, 26 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                ).let(::Tidslinje)
            )

            vurderinger.assertTidslinje(
                Segment(Periode(11 november 2024, 14 november 2024)) { vurdering ->
                    assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                    assertTrue(vurdering.harRett())
                },
                Segment(Periode(15 november 2024, 26 november 2024)) { vurdering ->
                    assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                    assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
                }
            )
        }
    }

    /*
        November 2024
     Mo Tu We Th Fr Sa Su
                  1  2  3
      4  5  6  7  8  9 10
     11 12 13 14 15 16 17
     18 19 20 21 22 23 24
     25 26 27 28 29 30
     */
    @Test
    fun `alle måter å dele opp periode i to hvor antall dager i kvote er lik hverdagene`() {
        val rettighetsperiode = Periode(11 november 2024, 26 november 2024)
        Periode(rettighetsperiode.fom.plusDays(1), rettighetsperiode.tom).dager().forEach {
            val vurderinger = regel.vurder(
                tomUnderveisInput.copy(
                    periodeForVurdering = rettighetsperiode,
                    kvoter = Kvoter.create(5, 0, 0)
                ),

                listOf(
                    Segment(
                        Periode(11 november 2024, it.minusDays(1)),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                    Segment(
                        Periode(it, 26 november 2024),
                        Vurdering(
                            fårAapEtter = RettighetsType.BISTANDSBEHOV,
                        )
                    ),
                ).let(::Tidslinje)
            )

            vurderinger.assertTidslinje(
                Segment(Periode(11 november 2024, 17 november 2024)) { vurdering ->
                    assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                    assertTrue(vurdering.harRett())
                },
                Segment(Periode(18 november 2024, 26 november 2024)) { vurdering ->
                    assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                    assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
                }
            )
        }
    }

    /*
        November 2024
     Mo Tu We Th Fr Sa Su
                  1  2  3
      4  5  6  7  8  9 10
     11 12 13 14 15 16 17
     18 19 20 21 22 23 24
     25 26 27 28 29 30
     */
    @Test
    fun `Veksler mellom ordinær, student -og sykepengeerstatning-kvote`() {
        val rettighetsperiode = Periode(11 november 2024, 26 november 2024)
        val vurderinger = regel.vurder(
            tomUnderveisInput.copy(
                periodeForVurdering = rettighetsperiode,
                kvoter = Kvoter.create(ordinærkvote = 6, studentkvote = 3, 3)
            ),
            listOf(
                Segment(
                    Periode(11 november 2024, 13 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.BISTANDSBEHOV,
                    )
                ),
                Segment(
                    Periode(14 november 2024, 18 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.STUDENT,
                    )
                ),
                Segment(
                    Periode(19 november 2024, 21 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.SYKEPENGEERSTATNING,
                    )
                ),
                Segment(
                    Periode(22 november 2024, 24 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.BISTANDSBEHOV,
                    )
                ),
                Segment(
                    Periode(25 november 2024, 25 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.STUDENT,
                    )
                ),
                Segment(
                    Periode(26 november 2024, 26 november 2024),
                    Vurdering(
                        fårAapEtter = RettighetsType.SYKEPENGEERSTATNING,
                    )
                ),
            ).let(::Tidslinje)
        )

        vurderinger.assertTidslinje(
            Segment(Periode(11 november 2024, 13 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(14 november 2024, 18 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR, Kvote.STUDENT), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(19 november 2024, 21 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.SYKEPENGEERSTATNING), vurdering.varighetVurdering?.brukerAvKvoter)
                assertTrue(vurdering.harRett())
            },
            Segment(Periode(22 november 2024, 24 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
            Segment(Periode(25 november 2024, 25 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.ORDINÆR, Kvote.STUDENT), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, STUDENTKVOTE_BRUKT_OPP, ORDINÆRKVOTE_BRUKT_OPP)
            },
            Segment(Periode(26 november 2024, 26 november 2024)) { vurdering ->
                assertEquals(setOf(Kvote.SYKEPENGEERSTATNING), vurdering.varighetVurdering?.brukerAvKvoter)
                assertStansGrunnet(vurdering, VARIGHETSKVOTE_BRUKT_OPP, SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
            },
        )
    }
}

private fun assertStansGrunnet(
    vurdering: Vurdering,
    avslagsÅrsak: UnderveisÅrsak,
    vararg kvoteAvslagsårsak: VarighetVurdering.Avslagsårsak
) {
    assertEquals(avslagsÅrsak, vurdering.avslagsårsak())
    assertFalse(vurdering.harRett())
    if (kvoteAvslagsårsak.isNotEmpty()) {
        assertTrue(vurdering.varighetVurdering is Avslag)
        assertEquals(kvoteAvslagsårsak.toSet(), (vurdering.varighetVurdering as Avslag).avslagsårsaker)
    }
}

