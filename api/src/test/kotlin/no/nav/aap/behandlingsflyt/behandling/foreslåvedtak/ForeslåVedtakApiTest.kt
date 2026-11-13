package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStansOpphørRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Fakes
class ForeslåVedtakApiTest : BaseApiTest() {

    private fun gatewayProvider(vedtaksperiodeBeslutter: Boolean = true) = createGatewayProvider {
        if (vedtaksperiodeBeslutter) register<VedtaksperiodeBeslutterPåUnleash>() else register<AlleAvskruddUnleash>()
    }

    @Test
    fun `returnerer tom liste av perioder når det ikke finnes underveisgrunnlag`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak(dato(2026, 1, 1)))
        InMemoryVilkårsresultatRepository.slett(behandling.id)

        val respons = hentForeslåVedtak(behandling)

        assertThat(respons.perioder).isEmpty()
        assertThat(respons.stansOpphør).isEmpty()
        assertThat(respons.harTilgangTilÅSaksbehandle).isTrue()
    }

    @Test
    fun `returnerer foreslått vedtak-periode uten vilkårsavslag når underveisgrunnlag finnes og vilkår er oppfylt`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak(dato(2026, 1, 1)))
        val periode = Periode(dato(2026, 1, 1), dato(2026, 1, 15))
        lagreGrunnlag(behandling.id, periode to Utfall.OPPFYLT)

        val foreslåttPeriode = hentForeslåVedtak(behandling).perioder.single()

        assertThat(foreslåttPeriode.periode).isEqualTo(periode)
        assertThat(foreslåttPeriode.utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(foreslåttPeriode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
        assertThat(foreslåttPeriode.avslagsårsak.vilkårsavslag).isEmpty()
        assertThat(foreslåttPeriode.avslagsårsak.underveisavslag).isNull()
    }

    @Test
    fun `inkluderer vilkårsavslag i respons når tidligereVurderinger gir UunngåeligAvslag`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak(dato(2026, 1, 1)))
        val periode = Periode(dato(2026, 1, 1), dato(2026, 1, 15))
        lagreGrunnlag(behandling.id, periode to Utfall.IKKE_OPPFYLT)

        // Kun ALDERSVILKÅRET er ikke oppfylt, slik at det gir opphav til UunngåeligAvslag.
        val aldersvilkåretIkkeOppfylt = Vilkår(
            Vilkårtype.ALDERSVILKÅRET,
            setOf(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                )
            )
        )
        InMemoryVilkårsresultatRepository.lagre(
            behandling.id,
            Vilkårsresultat(
                vilkår = listOf(aldersvilkåretIkkeOppfylt) +
                        oppfylteVilkår(periode).filter { it.type != Vilkårtype.ALDERSVILKÅRET }
            )
        )

        val foreslåttPeriode = hentForeslåVedtak(behandling).perioder.single()

        assertThat(foreslåttPeriode.avslagsårsak.vilkårsavslag)
            .extracting("vilkår")
            .containsExactly(Vilkårtype.ALDERSVILKÅRET.hjemmel)
    }

    @Test
    fun `markerer periode med endret tom som endret og viser forrige tom`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)

        lagreGrunnlag(forrige.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreGrunnlag(
            gjeldende.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )

        val perioder = hentForeslåVedtak(gjeldende).perioder

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].erNy).isFalse()
        assertThat(perioder[0].tidligerePerioder)
            .containsExactly(Periode(dato(2025, 1, 1), dato(2025, 12, 31)))
        assertThat(perioder[1].erNy).isTrue()
        assertThat(perioder[1].tidligerePerioder).isEmpty()
    }

    @Test
    fun `viser kun endringer fra forrige behandling og ikke fra eldre behandlinger`() {
        val (første, andre, tredje) = opprettBehandlingskjede(3)
        val medStans = arrayOf(
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )

        lagreGrunnlag(første.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreGrunnlag(andre.id, *medStans)
        lagreGrunnlag(tredje.id, *medStans)

        val perioder = hentForeslåVedtak(tredje).perioder

        assertThat(perioder[0].erNy).isFalse()
        assertThat(perioder[0].tidligerePerioder).isEmpty()
    }

    @Test
    fun `markerer ny innvilget periode og at stans fikk sluttdato`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)

        lagreGrunnlag(
            forrige.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreGrunnlag(
            gjeldende.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 3, 30)) to Utfall.IKKE_OPPFYLT,
            Periode(dato(2025, 3, 31), dato(2025, 12, 31)) to Utfall.OPPFYLT,
        )

        val perioder = hentForeslåVedtak(gjeldende).perioder

        assertThat(perioder).hasSize(3)
        assertThat(perioder[0].erNy).isFalse()
        assertThat(perioder[0].tidligerePerioder).isEmpty()
        assertThat(perioder[1].tidligerePerioder)
            .containsExactly(Periode(dato(2025, 3, 15), dato(2025, 12, 31)))
        assertThat(perioder[1].tidligereVarÅpen).isTrue()
        assertThat(perioder[2].erNy).isTrue()
        assertThat(perioder[2].tidligerePerioder).isEmpty()
    }

    @Test
    fun `ingen endringer når forrige behandling er identisk`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        val periode = Periode(dato(2025, 1, 1), dato(2025, 12, 31))

        lagreGrunnlag(forrige.id, periode to Utfall.OPPFYLT)
        lagreGrunnlag(gjeldende.id, periode to Utfall.OPPFYLT)

        val respons = hentForeslåVedtak(gjeldende)

        assertThat(respons.perioder.single().erNy).isFalse()
        assertThat(respons.perioder.single().tidligerePerioder).isEmpty()
        assertThat(respons.endretRettighetsperiode).isNull()
    }

    @Test
    fun `førstegangsbehandling uten forrige behandling har ingen endringer`() {
        val (behandling) = opprettBehandlingskjede(1)
        lagreGrunnlag(behandling.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)

        val respons = hentForeslåVedtak(behandling)

        assertThat(respons.perioder.single().erNy).isFalse()
        assertThat(respons.endretRettighetsperiode).isNull()
    }

    @Test
    fun `returnerer endret rettighetsperiode når helhetlig periode er utvidet`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        val forrigePeriode = Periode(dato(2025, 1, 1), dato(2025, 12, 31))
        val nyPeriode = Periode(dato(2025, 1, 1), dato(2026, 12, 31))

        lagreGrunnlag(forrige.id, forrigePeriode to Utfall.OPPFYLT)
        lagreGrunnlag(gjeldende.id, nyPeriode to Utfall.OPPFYLT)

        val endret = hentForeslåVedtak(gjeldende).endretRettighetsperiode

        assertThat(endret).isNotNull
        assertThat(endret!!.fra).isEqualTo(forrigePeriode)
        assertThat(endret.til).isEqualTo(nyPeriode)
    }

    @Test
    fun `ingen endringsinformasjon når VedtaksperiodeBeslutter er skrudd av`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        lagreGrunnlag(forrige.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreGrunnlag(
            gjeldende.id,
            Periode(dato(2025, 1, 1), dato(2026, 12, 31)) to Utfall.OPPFYLT,
        )

        val respons = hentForeslåVedtak(gjeldende, vedtaksperiodeBeslutter = false)

        assertThat(respons.endretRettighetsperiode).isNull()
        assertThat(respons.perioder.single().erNy).isFalse()
        assertThat(respons.perioder.single().tidligerePerioder).isEmpty()
    }

    @Test
    fun `avslagsperiode med endret fom er endret og ikke ny`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        lagreGrunnlag(
            forrige.id,
            Periode(dato(2025, 1, 1), dato(2025, 11, 30)) to Utfall.OPPFYLT,
            Periode(dato(2025, 12, 1), dato(2026, 1, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreGrunnlag(
            gjeldende.id,
            Periode(dato(2025, 1, 1), dato(2025, 12, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 12, 15), dato(2026, 1, 31)) to Utfall.IKKE_OPPFYLT,
        )

        val avslag = hentForeslåVedtak(gjeldende).perioder[1]

        assertThat(avslag.erNy).isFalse()
        assertThat(avslag.tidligerePerioder)
            .containsExactly(Periode(dato(2025, 12, 1), dato(2026, 1, 31)))
    }

    @Test
    fun `avslagsperiode uten overlapp med forrige er ny`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        lagreGrunnlag(forrige.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreGrunnlag(
            gjeldende.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 31)) to Utfall.OPPFYLT,
            Periode(dato(2025, 4, 1), dato(2025, 5, 31)) to Utfall.IKKE_OPPFYLT,
            Periode(dato(2025, 6, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT,
        )

        val avslag = hentForeslåVedtak(gjeldende).perioder[1]

        assertThat(avslag.erNy).isTrue()
        assertThat(avslag.tidligerePerioder).isEmpty()
    }

    @Test
    fun `avslagsperiode med både fom og tom endret matches på overlapp`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        lagreGrunnlag(
            forrige.id,
            Periode(dato(2025, 1, 1), dato(2025, 11, 30)) to Utfall.OPPFYLT,
            Periode(dato(2025, 12, 1), dato(2026, 1, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreGrunnlag(
            gjeldende.id,
            Periode(dato(2025, 1, 1), dato(2025, 11, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 11, 15), dato(2026, 2, 14)) to Utfall.IKKE_OPPFYLT,
        )

        val avslag = hentForeslåVedtak(gjeldende).perioder[1]

        assertThat(avslag.erNy).isFalse()
        assertThat(avslag.tidligerePerioder)
            .containsExactly(Periode(dato(2025, 12, 1), dato(2026, 1, 31)))
    }

    @Test
    fun `forrige behandling uten underveisgrunnlag gir ingen Ny eller Endret`() {
        val (forrige, gjeldende) = opprettBehandlingskjede(2)
        lagreGrunnlag(gjeldende.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)

        val respons = hentForeslåVedtak(gjeldende)

        assertThat(respons.perioder.single().erNy).isFalse()
        assertThat(respons.perioder.single().tidligerePerioder).isEmpty()
        assertThat(respons.endretRettighetsperiode).isNull()
    }

    @Test
    fun `bruker ikke underveisgrunnlag fra eldre behandling når forrige mangler`() {
        val (første, _, tredje) = opprettBehandlingskjede(3)
        lagreGrunnlag(første.id, Periode(dato(2025, 1, 1), dato(2025, 6, 30)) to Utfall.OPPFYLT)
        lagreGrunnlag(tredje.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)

        val respons = hentForeslåVedtak(tredje)

        assertThat(respons.perioder.single().tidligerePerioder).isEmpty()
        assertThat(respons.endretRettighetsperiode).isNull()
    }

    @Test
    fun `stans er ikke ny i førstegangsbehandling`() {
        val (behandling) = opprettBehandlingskjede(1)
        lagreGrunnlag(
            behandling.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreStansOpphør(
            behandling, behandling, dato(2025, 3, 15),
            Stans(setOf(Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING))
        )

        val vurdering = hentForeslåVedtak(behandling).stansOpphør.single().historikk.single()

        assertThat(vurdering.type).isEqualTo(StansOpphørVurderingTypeDto.STANS)
        assertThat(vurdering.erNy).isFalse()
    }

    @Test
    fun `opphør er ikke ny i førstegangsbehandling`() {
        val (behandling) = opprettBehandlingskjede(1)
        lagreGrunnlag(behandling.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreStansOpphør(
            behandling, behandling, dato(2026, 1, 1),
            Opphør(setOf(Avslagsårsak.BRUKER_OVER_67))
        )

        val vurdering = hentForeslåVedtak(behandling).stansOpphør.single().historikk.single()

        assertThat(vurdering.type).isEqualTo(StansOpphørVurderingTypeDto.OPPHØR)
        assertThat(vurdering.erNy).isFalse()
    }

    @Test
    fun `stans vurdert i revurderingen er ny`() {
        val sak = opprettInMemorySak(dato(2025, 1, 1))
        val forrige = opprettFørstegangsbehandling(sak)
        val revurdering = opprettRevurdering(sak, forrige)
        lagreGrunnlag(forrige.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreGrunnlag(
            revurdering.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreStansOpphør(
            revurdering, revurdering, dato(2025, 3, 15),
            Stans(setOf(Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING))
        )

        val vurdering = hentForeslåVedtak(revurdering).stansOpphør.single().historikk.single()

        assertThat(vurdering.erNy).isTrue()
    }

    @Test
    fun `stans vurdert i tidligere behandling er ikke ny i revurderingen`() {
        val sak = opprettInMemorySak(dato(2025, 1, 1))
        val forrige = opprettFørstegangsbehandling(sak)
        val revurdering = opprettRevurdering(sak, forrige)
        val perioder = arrayOf(
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreGrunnlag(forrige.id, *perioder)
        lagreGrunnlag(revurdering.id, *perioder)
        lagreStansOpphør(
            revurdering, forrige, dato(2025, 3, 15),
            Stans(setOf(Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING))
        )

        val vurdering = hentForeslåVedtak(revurdering).stansOpphør.single().historikk.single()

        assertThat(vurdering.erNy).isFalse()
    }

    @Test
    fun `stans er ikke ny når VedtaksperiodeBeslutter er skrudd av`() {
        val sak = opprettInMemorySak(dato(2025, 1, 1))
        val forrige = opprettFørstegangsbehandling(sak)
        val revurdering = opprettRevurdering(sak, forrige)
        lagreGrunnlag(
            revurdering.id,
            Periode(dato(2025, 1, 1), dato(2025, 3, 14)) to Utfall.OPPFYLT,
            Periode(dato(2025, 3, 15), dato(2025, 12, 31)) to Utfall.IKKE_OPPFYLT,
        )
        lagreStansOpphør(
            revurdering, revurdering, dato(2025, 3, 15),
            Stans(setOf(Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING))
        )

        val vurdering = hentForeslåVedtak(revurdering, vedtaksperiodeBeslutter = false)
            .stansOpphør.single().historikk.single()

        assertThat(vurdering.erNy).isFalse()
    }

    @Test
    fun `stans og opphør har hjemler fra avslagsårsakene`() {
        val (behandling) = opprettBehandlingskjede(1)
        lagreGrunnlag(behandling.id, Periode(dato(2025, 1, 1), dato(2025, 12, 31)) to Utfall.OPPFYLT)
        lagreStansOpphør(
            behandling, behandling, dato(2026, 1, 1),
            Opphør(setOf(Avslagsårsak.BRUKER_OVER_67))
        )

        val vurdering = hentForeslåVedtak(behandling).stansOpphør.single().historikk.single()

        assertThat(vurdering.årsaker).containsExactly(Avslagsårsak.BRUKER_OVER_67)
        assertThat(vurdering.hjemler).containsExactly(Avslagsårsak.BRUKER_OVER_67.hjemmel)
    }

    private fun dato(år: Int, måned: Int, dag: Int) = LocalDate.of(år, måned, dag)

    private fun opprettFørstegangsbehandling(sak: Sak, forrige: Behandling? = null) =
        opprettBehandling(sak, TypeBehandling.Førstegangsbehandling, forrige?.id)

    private fun opprettRevurdering(sak: Sak, forrige: Behandling) =
        opprettBehandling(sak, TypeBehandling.Revurdering, forrige.id)

    private fun lagreStansOpphør(
        behandling: Behandling,
        vurdertI: Behandling,
        fom: LocalDate,
        vurdering: StansEllerOpphør,
    ) {
        InMemoryStansOpphørRepository.lagre(
            behandling.id,
            StansOpphørGrunnlag(
                setOf(
                    GjeldendeStansEllerOpphør(
                        fom = fom,
                        opprettet = Instant.now(),
                        vurdertIBehandling = vurdertI.id,
                        vurdering = vurdering,
                    )
                )
            )
        )
    }

    /** Oppretter [antall] behandlinger der hver peker på forrige. */
    private fun opprettBehandlingskjede(antall: Int): List<Behandling> {
        val sak = opprettInMemorySak(LocalDate.of(2025, 1, 1))
        return (1 until antall).fold(listOf(opprettFørstegangsbehandling(sak))) { kjede, _ ->
            kjede + opprettFørstegangsbehandling(sak, kjede.last())
        }
    }

    private fun oppfylteVilkår(periode: Periode) = listOf(
        Vilkårtype.LOVVALG,
        Vilkårtype.ALDERSVILKÅRET,
        Vilkårtype.STUDENT,
        Vilkårtype.SYKDOMSVILKÅRET,
        Vilkårtype.OVERGANGUFØREVILKÅRET,
        Vilkårtype.OVERGANGARBEIDVILKÅRET,
        Vilkårtype.SYKEPENGEERSTATNING,
        Vilkårtype.GRUNNLAGET,
        Vilkårtype.INNTEKTSBORTFALL,
        Vilkårtype.MEDLEMSKAP,
        Vilkårtype.SAMORDNING,
        Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING,
    ).map { type ->
        Vilkår(
            type,
            setOf(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                )
            )
        )
    }

    private fun underveisperiode(periode: Periode, utfall: Utfall) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = utfall,
        rettighetsType = if (utfall == Utfall.OPPFYLT) RettighetsType.BISTANDSBEHOV else null,
        avslagsårsak = if (utfall == Utfall.OPPFYLT) null else UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
        grenseverdi = Prosent.`100_PROSENT`,
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        meldepliktStatus = null,
        meldepliktGradering = null,
    )

    /** Lagrer underveisperioder, og et vilkårsresultat der alle vilkår er oppfylt i hele perioden. */
    private fun lagreGrunnlag(behandlingId: BehandlingId, vararg perioder: Pair<Periode, Utfall>) {
        InMemoryUnderveisRepository.lagre(
            behandlingId = behandlingId,
            underveisperioder = perioder.map { (periode, utfall) -> underveisperiode(periode, utfall) },
            input = object : Faktagrunnlag {},
        )
        val helePerioden = Periode(perioder.minOf { it.first.fom }, perioder.maxOf { it.first.tom })
        InMemoryVilkårsresultatRepository.lagre(behandlingId, Vilkårsresultat(vilkår = oppfylteVilkår(helePerioden)))
    }

    private fun hentForeslåVedtak(
        behandling: Behandling,
        vedtaksperiodeBeslutter: Boolean = true
    ): ForeslåVedtakResponse {
        var body: ForeslåVedtakResponse? = null
        testApplication {
            installApplication {
                foreslaaVedtakApi(
                    MockDataSource(),
                    inMemoryRepositoryRegistry,
                    gatewayProvider(vedtaksperiodeBeslutter)
                )
            }
            val response =
                createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/foreslaa-vedtak") {
                    header("Authorization", "Bearer ${getToken().token()}")
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            body = response.body<ForeslåVedtakResponse>()
        }
        return requireNotNull(body)
    }
}

object VedtaksperiodeBeslutterPåUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(BehandlingsflytFeature.VedtaksperiodeBeslutter)
)
