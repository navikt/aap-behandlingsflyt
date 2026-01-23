package no.nav.aap.behandlingsflyt.prosessering.datadeling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.assertTidslinjeEquals
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.utils.diffMap
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.junit.jupiter.api.Test

class UtledArenaVedtakstypeTest {
    private val sakId = SakId(1L)
    private val saksnummer = Saksnummer("o")

    /* Helt enkel case: gir avslag på første søknad. */
    @Test
    fun `gir avslag på første søknad`() {
        // Given
        val søknadMottatt = 1 april 2020
        val behandling = behandling(opprettet = søknadMottatt.atStartOfDay())

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = behandling,
                nyeSøknader = setOf(søknad(behandlingId = behandling.id, mottatt = søknadMottatt.atStartOfDay())),
                rettighetsType = Tidslinje(),
                stansOgOpphør = mapOf(),
            )
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            Periode(søknadMottatt, søknadMottatt) to UtledArenaVedtakstype.ArenaVedtak(
                referanse = behandling.referanse,
                vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_AVSLAG
            )
        )
    }

    @Test
    fun `gir avslag på første søknad og andre søknad`() {
        // Given
        val førsteSøknad = 1 april 2020
        val andreSøknad = 1 mai 2020
        val førsteAvslag = behandling(opprettet = førsteSøknad.atStartOfDay())
        val andreAvslag = behandling(opprettet = andreSøknad.atStartOfDay(), forrige = førsteAvslag)

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førsteAvslag,
                nyeSøknader = setOf(søknad(behandlingId = førsteAvslag.id, mottatt = førsteSøknad.atStartOfDay())),
                rettighetsType = Tidslinje(),
                stansOgOpphør = mapOf(),
            ),
            AvsluttetKelvinBehandling(
                behandling = andreAvslag,
                nyeSøknader = setOf(søknad(behandlingId = andreAvslag.id, mottatt = andreSøknad.atStartOfDay())),
                rettighetsType = Tidslinje(),
                stansOgOpphør = mapOf(),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            Periode(førsteSøknad, førsteSøknad) to UtledArenaVedtakstype.ArenaVedtak(
                referanse = førsteAvslag.referanse,
                vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_AVSLAG
            ),
            Periode(andreSøknad, andreSøknad) to UtledArenaVedtakstype.ArenaVedtak(
                referanse = andreAvslag.referanse,
                vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_AVSLAG
            )
        )
    }


    @Test
    fun `ordinær innvilgelse førstegangsvedtak`() {
        // Given
        val innvilgetPeriode = Periode(1 april 2020, 1 mai 2020)
        val søknadMottatt = innvilgetPeriode.fom.atStartOfDay()
        val behandling = behandling(søknadMottatt)

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = behandling,
                nyeSøknader = setOf(søknad(behandling.id, søknadMottatt)),
                rettighetsType = tidslinjeOf(innvilgetPeriode to RettighetsType.BISTANDSBEHOV),
                stansOgOpphør = mapOf(),
            )
        )

        // Then
        assertTidslinjeEquals(
            resultat, tidslinjeOf(
                innvilgetPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = behandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD
                )
            )
        )
    }

    @Test
    fun `avslag førstegangsbehandling, manuell innvilgelse etterpå `() {
        // Given
        val søknadMottatt = 1 april 2020
        val førstegangsbehandling = behandling(søknadMottatt.atStartOfDay())
        val revurdering =
            behandling(opprettet = søknadMottatt.plusDays(1).atStartOfDay(), forrige = førstegangsbehandling)
        val innvilgetPeriode = Periode(søknadMottatt, søknadMottatt.plusDays(30))

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førstegangsbehandling,
                nyeSøknader = setOf(søknad(førstegangsbehandling.id, søknadMottatt.atStartOfDay())),
                rettighetsType = tidslinjeOf(),
                stansOgOpphør = mapOf(),
            ),
            AvsluttetKelvinBehandling(
                behandling = revurdering,
                rettighetsType = tidslinjeOf(innvilgetPeriode to RettighetsType.BISTANDSBEHOV),
                stansOgOpphør = mapOf(),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                innvilgetPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = revurdering.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_NAV,
                )
            )
        )
    }

    @Test
    fun `avslag førstegangsbehandling, innvilgelse fra søknad etterpå `() {
        // Given
        val førsteSøknadMottatt = LocalDateTime.parse("2020-04-01T00:00:00")
        val andreSøknadMottatt = LocalDateTime.parse("2020-05-01T00:00:00")
        val førstegangsbehandling = behandling(opprettet = førsteSøknadMottatt)
        val revurdering = behandling(opprettet = andreSøknadMottatt, forrige = førstegangsbehandling)
        val innvilgetPeriode = Periode(
            andreSøknadMottatt.toLocalDate(),
            andreSøknadMottatt.toLocalDate().plusDays(30)
        )

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førstegangsbehandling,
                nyeSøknader = setOf(søknad(førstegangsbehandling.id, førsteSøknadMottatt)),
                rettighetsType = Tidslinje(),
                stansOgOpphør = mapOf(),
            ),
            AvsluttetKelvinBehandling(
                behandling = revurdering,
                nyeSøknader = setOf(søknad(revurdering.id, andreSøknadMottatt)),
                rettighetsType = tidslinjeOf(innvilgetPeriode to RettighetsType.BISTANDSBEHOV),
                stansOgOpphør = mapOf(),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                førsteSøknadMottatt.toLocalDate().somPeriode() to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = førstegangsbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_AVSLAG
                ),
                innvilgetPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = revurdering.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD
                )
            )
        )
    }

    @Test
    fun `innvilgelse og opphør på grunn av alder i førstegangsbehandling`() {
        // Given
        val søknadsdato = LocalDateTime.parse("2020-04-01T00:00:00")
        val førstegangsbehandling = behandling(opprettet = søknadsdato)

        val stansdato = søknadsdato.toLocalDate().plusDays(31)
        val innvilgetPeriode = Periode(søknadsdato.toLocalDate(), stansdato.minusDays(1))

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førstegangsbehandling,
                nyeSøknader = setOf(søknad(førstegangsbehandling.id, søknadsdato)),
                rettighetsType = tidslinjeOf(innvilgetPeriode to RettighetsType.BISTANDSBEHOV),
                stansOgOpphør = mapOf(stansdato to setOf(Avslagsårsak.BRUKER_OVER_67)),
            )
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                innvilgetPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = førstegangsbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD
                ),
                /* Selv om AAP er opphørt etter perioden, så indikerer vi ikke det her,
                 * slik at vi etterligner Arena.
                 */
            )
        )
    }

    @Test
    fun `Forlengelse av ordinær AAP`() {
        // Given
        val førsteStønadsperiode = Periode(1 april 2020, 31 mars 2021)
        val andreStønadsperiode = Periode(1 april 2021, 31 mars 2022)

        val førstegangsbehandling = behandling(opprettet = førsteStønadsperiode.fom.atStartOfDay())
        val forlengelse = behandling(
            opprettet = andreStønadsperiode.fom.atStartOfDay(),
            forrige = førstegangsbehandling,
            vurderingsbehov = Vurderingsbehov.UTVID_VEDTAKSLENGDE
        )

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førstegangsbehandling,
                nyeSøknader = setOf(søknad(førstegangsbehandling.id, førsteStønadsperiode.fom.atStartOfDay())),
                rettighetsType = tidslinjeOf(førsteStønadsperiode to RettighetsType.BISTANDSBEHOV),
                stansOgOpphør = mapOf(),
            ),
            AvsluttetKelvinBehandling(
                behandling = forlengelse,
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV,
                    andreStønadsperiode to RettighetsType.BISTANDSBEHOV,
                ).komprimer(),
                stansOgOpphør = mapOf(),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                førsteStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = førstegangsbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD
                ),
                andreStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = forlengelse.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.E_FORLENGE,
                )
            )
        )
    }

    @Test
    fun `Innvilgelse av sykepengeerstatning`() {
        // Given
        val stønadsperioden = Periode(1 april 2020, 31 mars 2021)
        val opphørsdato = stønadsperioden.tom.plusDays(1)

        val førstegangsbehandling = behandling(opprettet = stønadsperioden.fom.atStartOfDay())

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førstegangsbehandling,
                nyeSøknader = setOf(søknad(førstegangsbehandling.id, stønadsperioden.fom.atStartOfDay())),
                rettighetsType = tidslinjeOf(stønadsperioden to RettighetsType.SYKEPENGEERSTATNING),
                stansOgOpphør = mapOf(
                    opphørsdato to setOf(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP),
                ),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                stønadsperioden to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = førstegangsbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD,
                ),
            )
        )
    }

    @Test
    fun `Periode med sykepengeerstatning fortsetter med ordinær AAP`() {
        // Given
        val førsteStønadsperiode = Periode(1 april 2020, 31 mars 2021)
        val andreStønadsperiode = Periode(1 april 2021, 31 mars 2022)
        val opphørsdato = førsteStønadsperiode.tom.plusDays(1)

        val sykepengeBehandling = behandling(opprettet = førsteStønadsperiode.fom.atStartOfDay())
        val ordinærBehandling = behandling(opprettet = opphørsdato.atStartOfDay(), forrige = sykepengeBehandling)

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = sykepengeBehandling,
                nyeSøknader = setOf(søknad(sykepengeBehandling.id, førsteStønadsperiode.fom.atStartOfDay())),
                rettighetsType = tidslinjeOf(førsteStønadsperiode to RettighetsType.SYKEPENGEERSTATNING),
                stansOgOpphør = mapOf(
                    opphørsdato to setOf(Avslagsårsak.BRUKER_OVER_67), /* TODO: bytt ut med avslagsårsak for kvote brukt opp */
                ),
            ),
            AvsluttetKelvinBehandling(
                behandling = ordinærBehandling,
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.SYKEPENGEERSTATNING,
                    andreStønadsperiode to RettighetsType.BISTANDSBEHOV,
                ),
                stansOgOpphør = mapOf(),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                førsteStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = sykepengeBehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD,
                ),
                andreStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = ordinærBehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.E_VERDI,
                ),
            )
        )
    }

    @Test
    fun `gjenopptak etter brudd på aktivitetsplikten, uten søknad`() {
        // Given
        val førsteStønadsperiode = Periode(1 april 2020, 5 juni 2020)
        val stansperioden = Periode(6 juni 2020, 14 juni 2020)
        val andreStønadsperiode = Periode(15 juni 2020, 31 mars 2021)

        val innvilgelsesbehandling = behandling(opprettet = førsteStønadsperiode.fom.atStartOfDay())
        val stansbehandling = behandling(opprettet = stansperioden.fom.atStartOfDay(), forrige = innvilgelsesbehandling)
        val gjenopptakbehandling =
            behandling(opprettet = andreStønadsperiode.fom.atStartOfDay(), forrige = stansbehandling)

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = innvilgelsesbehandling,
                nyeSøknader = setOf(søknad(innvilgelsesbehandling.id, førsteStønadsperiode.fom.atStartOfDay())),
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV,
                    stansperioden to RettighetsType.BISTANDSBEHOV,
                    andreStønadsperiode to RettighetsType.BISTANDSBEHOV
                ).komprimer(),
                stansOgOpphør = mapOf(),
            ),
            AvsluttetKelvinBehandling(
                behandling = stansbehandling,
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV
                ),
                stansOgOpphør = mapOf(
                    stansperioden.fom to setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                ),
            ),
            AvsluttetKelvinBehandling(
                behandling = gjenopptakbehandling,
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV,
                    andreStønadsperiode to RettighetsType.BISTANDSBEHOV,
                ),
                stansOgOpphør = mapOf(
                    stansperioden.fom to setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS),
                ),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                førsteStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = innvilgelsesbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD,
                ),
                stansperioden.fom.somPeriode() to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = stansbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.S_STANS
                ),
                andreStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = gjenopptakbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.G_INNV_NAV
                ),
            )
        )
    }


    @Test
    fun `gjenopptak etter brudd på aktivitetsplikten, hvor bruker har søkt søknad`() {
        // Given
        val førsteStønadsperiode = Periode(1 april 2020, 5 juni 2020)
        val stansperioden = Periode(6 juni 2020, 14 juni 2020)
        val andreStønadsperiode = Periode(15 juni 2020, 31 mars 2021)

        val innvilgelsesbehandling = behandling(opprettet = førsteStønadsperiode.fom.atStartOfDay())
        val stansbehandling = behandling(opprettet = stansperioden.fom.atStartOfDay(), forrige = innvilgelsesbehandling)
        val gjenopptakbehandling =
            behandling(opprettet = andreStønadsperiode.fom.atStartOfDay(), forrige = stansbehandling)

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = innvilgelsesbehandling,
                nyeSøknader = setOf(søknad(innvilgelsesbehandling.id, førsteStønadsperiode.fom.atStartOfDay())),
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV,
                    stansperioden to RettighetsType.BISTANDSBEHOV,
                    andreStønadsperiode to RettighetsType.BISTANDSBEHOV
                ).komprimer(),
                stansOgOpphør = mapOf(),
            ),
            AvsluttetKelvinBehandling(
                behandling = stansbehandling,
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV
                ),
                stansOgOpphør = mapOf(
                    stansperioden.fom to setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR),
                ),
            ),
            AvsluttetKelvinBehandling(
                behandling = gjenopptakbehandling,
                rettighetsType = tidslinjeOf(
                    førsteStønadsperiode to RettighetsType.BISTANDSBEHOV,
                    andreStønadsperiode to RettighetsType.BISTANDSBEHOV,
                ),
                nyeSøknader = setOf(søknad(gjenopptakbehandling.id, mottatt = andreStønadsperiode.fom.atStartOfDay())),
                stansOgOpphør = mapOf(
                    stansperioden.fom to setOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR),
                ),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                førsteStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = innvilgelsesbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD,
                ),
                stansperioden.fom.somPeriode() to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = stansbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.S_OPPHOR,
                ),
                andreStønadsperiode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = gjenopptakbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.G_INNV_SOKNAD
                ),
            )
        )
    }

    @Test
    fun `Standard første vedtak for student`() {
        // Given
        val søknadsdato = LocalDateTime.parse("2020-04-01T00:00:00")
        val førstegangsbehandling = behandling(opprettet = søknadsdato)

        val stansdato = søknadsdato.toLocalDate().plusDays(31)
        val innvilgetPeriode = Periode(søknadsdato.toLocalDate(), stansdato.minusDays(1))

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = førstegangsbehandling,
                nyeSøknader = setOf(søknad(førstegangsbehandling.id, søknadsdato)),
                rettighetsType = tidslinjeOf(innvilgetPeriode to RettighetsType.STUDENT),
                stansOgOpphør = mapOf(stansdato to setOf(Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT)),
            )
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                innvilgetPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = førstegangsbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD
                ),
                /* Selv om AAP er opphørt på grunn av at varigheten er brukt opp, så
                 * viser vi ikke den årsaken, slik at det blir likt som Arena.
                 */
            )
        )
    }

    @Test
    fun `Søker om ordinær AAP rett etter utgangen av student-perioden`() {
        // Given
        val søknadsdatoStudent = 1 april 2020
        val opphørsdatoStudent = 30 mai 2020
        val studentPeriode = Periode(søknadsdatoStudent, opphørsdatoStudent.minusDays(1))
        val ordinærPeriode = Periode(opphørsdatoStudent, opphørsdatoStudent.plusDays(30))

        val studentbehandling = behandling(opprettet = søknadsdatoStudent.atStartOfDay())
        val ordinærbehandling = behandling(opprettet = opphørsdatoStudent.atStartOfDay(), forrige = studentbehandling)

        // When
        val resultat = utledArenaVedtakForKelvinSak(
            AvsluttetKelvinBehandling(
                behandling = studentbehandling,
                nyeSøknader = setOf(søknad(studentbehandling.id, søknadsdatoStudent.atStartOfDay())),
                rettighetsType = tidslinjeOf(studentPeriode to RettighetsType.STUDENT),
                stansOgOpphør = mapOf(søknadsdatoStudent to setOf(Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT)),
            ),
            AvsluttetKelvinBehandling(
                behandling = ordinærbehandling,
                nyeSøknader = setOf(søknad(ordinærbehandling.id, opphørsdatoStudent.atStartOfDay())),
                rettighetsType = tidslinjeOf(
                    studentPeriode to RettighetsType.STUDENT,
                    ordinærPeriode to RettighetsType.BISTANDSBEHOV,
                ),
                stansOgOpphør = mapOf(søknadsdatoStudent to setOf(Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT)),
            ),
        )

        // Then
        assertTidslinjeEquals(
            resultat,
            tidslinjeOf(
                studentPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = studentbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD,
                ),
                ordinærPeriode to UtledArenaVedtakstype.ArenaVedtak(
                    referanse = ordinærbehandling.referanse,
                    vedtaksvariant = UtledArenaVedtakstype.ArenaVedtaksvariant.E_VERDI,
                ),
            )
        )
    }

    class AvsluttetKelvinBehandling(
        val behandling: BehandlingMedVedtak,
        val nyeSøknader: Set<MottattDokument> = emptySet(),
        val rettighetsType: Tidslinje<RettighetsType>,
        val stansOgOpphør: Map<LocalDate, Set<Avslagsårsak>>,
    )

    fun utledArenaVedtakForKelvinSak(vararg behandlinger: AvsluttetKelvinBehandling): Tidslinje<UtledArenaVedtakstype.ArenaVedtak> {
        val søknader = mutableSetOf<MottattDokument>()
        var resultat = Tidslinje<UtledArenaVedtakstype.ArenaVedtak>()
        var forrigeRettighetsType = Tidslinje<RettighetsType>()
        var forrigeStansOgOpphør = mapOf<LocalDate, Set<Avslagsårsak>>()

        for (x in behandlinger) {
            søknader.addAll(x.nyeSøknader)
            resultat = UtledArenaVedtakstype.utledVedtak(
                eksisterendeVedtak = resultat,
                behandling = x.behandling,
                søknader = søknader,
                rettighetsTyper = diffTidslinjer(forrigeRettighetsType, x.rettighetsType),
                stansOgOpphør = diffMap(forrigeStansOgOpphør, x.stansOgOpphør),
            )
            forrigeRettighetsType = x.rettighetsType
            forrigeStansOgOpphør = x.stansOgOpphør
        }

        return resultat
    }


    private fun søknad(
        behandlingId: BehandlingId,
        mottatt: LocalDateTime
    ): MottattDokument = MottattDokument(
        referanse = InnsendingReferanse(JournalpostId("0")),
        sakId = sakId,
        behandlingId = behandlingId,
        mottattTidspunkt = mottatt,
        type = InnsendingType.SØKNAD,
        kanal = Kanal.DIGITAL,
        status = Status.BEHANDLET,
        strukturertDokument = null,
    )

    private var behandlingIdSeq = 0L

    private fun behandling(
        opprettet: LocalDateTime,
        forrige: BehandlingMedVedtak? = null,
        vurderingsbehov: Vurderingsbehov? = null,
    ): BehandlingMedVedtak {

        val vurderingsbehov = vurderingsbehov ?: if (forrige == null)
            Vurderingsbehov.MOTTATT_SØKNAD
        else
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND

        return BehandlingMedVedtak(
            id = BehandlingId(behandlingIdSeq.also { behandlingIdSeq += 1 }),
            forrigeBehandlingId = forrige?.id,
            referanse = BehandlingReferanse(UUID.randomUUID()),
            typeBehandling = if (forrige == null) TypeBehandling.Førstegangsbehandling else TypeBehandling.Revurdering,
            status = no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET,
            vurderingsbehov = setOf(vurderingsbehov),
            årsakTilOpprettelse =
                if (vurderingsbehov == Vurderingsbehov.MOTTATT_SØKNAD) ÅrsakTilOpprettelse.SØKNAD
                else ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
            opprettetTidspunkt = opprettet,
            saksnummer = saksnummer,
            vedtakstidspunkt = LocalDateTime.MIN,
            virkningstidspunkt = LocalDate.MIN,
        )
    }
}

private fun LocalDate.somPeriode() = Periode(this, this)
