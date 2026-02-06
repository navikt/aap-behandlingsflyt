package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
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
import no.nav.aap.behandlingsflyt.flyt.VedtakslengdeUnleash
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakslengdeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class VedtakslengdeStegTest {
    private val random = Random(1235123)

    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `Skal forlenge sluttdato med 261 dager for fremtidig rett på ordinær`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)
        val person = person()
        val sak = sak(person, rettighetsperiode)
        val forrigeBehandling = førstegangsbehandling(sak.id)

        val ikkeOppfyltPeriode = Periode(2 desember 2020, 1 januar 2021)
        val vedtatteUnderveisperioder = listOf(
            underveisperiode(Utfall.OPPFYLT, RettighetsType.BISTANDSBEHOV, Periode(1 januar 2020, 1 desember 2020)),
            underveisperiode(
                Utfall.IKKE_OPPFYLT, null, ikkeOppfyltPeriode
            )
        )
        val vedtattVilkårsresultat = genererVilkårsresultat(rettighetsperiode, straffePeriode = ikkeOppfyltPeriode)

        InMemoryUnderveisRepository.lagre(
            behandlingId = forrigeBehandling.id,
            underveisperioder = vedtatteUnderveisperioder,
            input = object : Faktagrunnlag {}
        )
        
        val vedtakslengdeRepository = InMemoryVedtakslengdeRepository

        val inneværendeBehandling = revurdering(
            sak.id,
            forrigeBehandling.id,
            VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        Vurderingsbehov.UTVID_VEDTAKSLENGDE
                    )
                )
            )
        )

        InMemoryVilkårsresultatRepository.lagre(
            inneværendeBehandling.id,
            vedtattVilkårsresultat
        )

        val kontekst =
            FlytKontekstMedPeriodeService(SakService(sakRepository, behandlingRepository), behandlingRepository)
                .utled(
                    FlytKontekst(
                        inneværendeBehandling.sakId,
                        inneværendeBehandling.id,
                        inneværendeBehandling.forrigeBehandlingId,
                        inneværendeBehandling.typeBehandling()
                    ), StegType.FASTSETT_VEDTAKSLENGDE
                )

        val dagensDato = 10 desember 2020

        val steg = VedtakslengdeSteg(
            vedtakslengdeService = VedtakslengdeService(
                vedtakslengdeRepository = InMemoryVedtakslengdeRepository,
                underveisRepository = InMemoryUnderveisRepository,
                vilkårsresultatRepository = InMemoryVilkårsresultatRepository,
                clock = fixedClock(dagensDato),
                unleashGateway = AlleAvskruddUnleash
            ),
            unleashGateway = AlleAvskruddUnleash,
        )

        steg.utfør(kontekst)
        
        assertThat(vedtakslengdeRepository.hentHvisEksisterer(inneværendeBehandling.id)).isNotNull
        
    }

    private fun revurdering(
        sakId: SakId,
        forrigeBehandling: BehandlingId,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak
    ): Behandling {
        return behandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandling,
            vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak
        )
    }

    private fun førstegangsbehandling(sakId: SakId): Behandling {
        return behandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )

    }

    private fun sak(person: Person, periode: Periode): Sak =
        sakRepository.finnEllerOpprett(person, periode)


    private fun person(): Person =
        Person(PersonId(random.nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

    private fun genererVilkårsresultat(periode: Periode, straffePeriode: Periode): Vilkårsresultat {
        val aldersVilkåret =
            Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val sykdomsVilkåret =
            Vilkår(
                Vilkårtype.SYKDOMSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val lovvalgsVilkåret =
            Vilkår(
                Vilkårtype.LOVVALG, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val medlemskapVilkåret =
            Vilkår(
                Vilkårtype.MEDLEMSKAP, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val bistandVilkåret =
            Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val grunnlagVilkåret = Vilkår(
            Vilkårtype.GRUNNLAGET, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        )
        
        val straffegjennomføringVilkår = Vilkår(
            Vilkårtype.STRAFFEGJENNOMFØRING, setOf(
                Vilkårsperiode(
                    straffePeriode,
                    Utfall.IKKE_OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null,
                    avslagsårsak = Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
                )
            )
        )

        return Vilkårsresultat(
            vilkår = listOf(
                aldersVilkåret,
                lovvalgsVilkåret,
                sykdomsVilkåret,
                medlemskapVilkåret,
                bistandVilkåret,
                grunnlagVilkåret,
                straffegjennomføringVilkår
            )
        )
    }

    private fun underveisperiode(
        utfall: Utfall,
        rettighetsType: RettighetsType?,
        periode: Periode,
        avslagOrdinærKvote: Boolean = false
    ) = Underveisperiode(
        periode = periode,
        meldePeriode = Periode(periode.fom, periode.fom.plusDays(14)),
        utfall = utfall,
        rettighetsType = rettighetsType,
        avslagsårsak = when {
            utfall == Utfall.OPPFYLT -> null
            avslagOrdinærKvote -> UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP
            utfall == Utfall.IKKE_OPPFYLT -> UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS
            else -> null
        },
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(0)),
            andelArbeid = `0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = setOfNotNull(
            if (rettighetsType == RettighetsType.BISTANDSBEHOV) Kvote.ORDINÆR else null
        ),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = `0_PROSENT`,
    )

}