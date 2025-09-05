package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKansellerRevurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ResultatUtlederTest {
    private val resultatUtleder = ResultatUtleder(
        underveisRepository = InMemoryUnderveisRepository,
        InMemoryBehandlingRepository,
        trukketSøknadService = TrukketSøknadService(
            InMemoryTrukketSøknadRepository
        ),
        kansellerRevurderingService = KansellerRevurderingService(
            InMemoryKansellerRevurderingRepository
        )
    )

    @Test
    fun `innvilgelse betyr minst en periode med oppfylt`() {
        val sak = nySak(Periode(1 januar 2023, 31 desember 2023))
        val behandling = opprettBehandling(sak)

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.OPPFYLT, Periode(1 januar 2023, 31 desember 2023)),
                underveisperiode(Utfall.IKKE_OPPFYLT, Periode(1 januar 2024, 31 desember 2024)),
            ),
            input = object : Faktagrunnlag {}
        )

        val resultat = resultatUtleder.utledResultat(behandling.id)

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
    }

    @Test
    fun `avslag betyr ingen oppfylte perioder`() {
        val sak = nySak(Periode(1 januar 2023, 31 desember 2023))
        val behandling = opprettBehandling(sak)

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.IKKE_OPPFYLT, Periode(1 januar 2023, 31 desember 2023)),
                underveisperiode(Utfall.IKKE_OPPFYLT, Periode(1 januar 2024, 31 desember 2024)),
            ),
            input = object : Faktagrunnlag {}
        )

        val resultat = resultatUtleder.utledResultat(behandling.id)

        assertThat(resultat).isEqualTo(Resultat.AVSLAG)
    }

    @Test
    fun `per nå, støtter kun å utlede resultat for førstegangsbehandling og revurdering`() {
        val sak = nySak(Periode(1 januar 2023, 31 desember 2023))
        val behandling = opprettBehandling(sak)
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

        val klage = InMemorySakOgBehandlingService.finnEllerOpprettOrdinærBehandling(
            sak.saksnummer,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTATT_KLAGE)),
                årsak = ÅrsakTilOpprettelse.KLAGE
            )
        )

        assertThat(klage.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        InMemoryUnderveisRepository.lagre(
            behandlingId = behandling.id,
            underveisperioder = listOf(
                underveisperiode(Utfall.IKKE_OPPFYLT, Periode(1 januar 2023, 31 desember 2023)),
                underveisperiode(Utfall.IKKE_OPPFYLT, Periode(1 januar 2024, 31 desember 2024)),
            ),
            input = object : Faktagrunnlag {}
        )

        assertThrows<IllegalArgumentException> {
            resultatUtleder.utledResultat(klage.id)
        }
    }

    private fun underveisperiode(utfall: Utfall, periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = Periode(periode.fom, periode.fom.plusDays(14)),
        utfall = utfall,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = when (utfall) {
            Utfall.OPPFYLT -> null
            Utfall.IKKE_OPPFYLT -> UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT
            else -> null
        },
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(0)),
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        bruddAktivitetspliktId = null,
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
    )

    private fun nySak(periode: Periode): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            InMemoryPersonRepository,
            InMemorySakRepository
        ).finnEllerOpprett(ident(), periode)
    }

    private fun opprettBehandling(sak: Sak): Behandling {
        return InMemorySakOgBehandlingService.finnEllerOpprettOrdinærBehandling(
            sak.saksnummer,
            VurderingsbehovOgÅrsak(
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                ÅrsakTilOpprettelse.SØKNAD
            )
        )
    }
}