package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Fakes
class LovvalgMedlemskapInformasjonskravTest {

    object LovvalgInformasjonskravPå : FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.HentInnLovvalgRegisterdataForNyesteAktuelleKrav)
    )

    @Disabled
    @Test
    fun `Skal hente inn inntekt kun for alle ikke-vedtatte krav som har endret dato fra vedtatt`() {
        // TODO
    }

    @Test
    fun `Skal hente inn inntekt kun for nyeste (vedtatte) krav`() {
        /** 
         * TODO: Koden bør egentlig hente inn for alle nye krav eller krav med relevant endring (dato)
         * i inneværende behandling
         * Det vil ikke skje i praksis enda
         */
        val informasjonskrav = LovvalgMedlemskapInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            testGatewayProvider(LovvalgInformasjonskravPå::class)
        )


        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2025)


        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }

        settOppKravOgStønadsperiode(
            behandling, mapOf(
                1 mars 2025 to RelevantKravType.NY_STØNADSPERIODE,
                1 januar 2026 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR

            )
        )

        val input = informasjonskrav.klargjør(kontekst)

        assertThat(input.relevantDato).isEqualTo(
            1 januar 2026
        )

    }

    @Test
    fun `Det skal ikke hentes inn data for gjenopptak etter stans`() {
        val informasjonskrav = LovvalgMedlemskapInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            testGatewayProvider(LovvalgInformasjonskravPå::class)
        )


        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2025)


        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }

        settOppKravOgStønadsperiode(
            behandling, mapOf(
                1 mars 2025 to RelevantKravType.NY_STØNADSPERIODE,
                1 januar 2026 to RelevantKravType.GJENOPPTAK_ETTER_STANS(listOf(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS))
            )
        )

        val input = informasjonskrav.klargjør(kontekst)

        assertThat(input.relevantDato)
            .describedAs { "Det skal ikke hentes inn data for gjenopptak etter stans" }.isEqualTo(
                1 mars 2025
            )

    }


    private fun settOppKravOgStønadsperiode(
        behandling: Behandling,
        kravMap: Map<LocalDate, RelevantKravType>
    ) {
        val kravRepsitory: KravRepository = inMemoryRepositoryProvider.provide()
        val stønadsperiodeRepository: StønadsperiodeRepository = inMemoryRepositoryProvider.provide()

        val test: List<Pair<RelevantKrav, StønadsperiodeVurdering>> = kravMap.entries.map { (dato, kravType) ->
            val referanse = Kravreferanse(UUID.randomUUID())

            Pair(
                RelevantKrav(
                    referanse = referanse,
                    journalpostId = JournalpostId("123"),
                    vurdertAv = SYSTEMBRUKER,
                    begrunnelse = "Automatisk",
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    søknadsdato = Søknadsdato(dato, årsak = SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Automatisk"),
                    overstyrMuligRettFra = null,
                    muligRettFra = dato
                ),
                StønadsperiodeVurdering(
                    referanse = referanse,
                    startDato = dato,
                    begrunnelse = "Begrunnelse",
                    relevantKravType = kravType,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    harHattOrdinærSiste52Uker = when (kravType) {
                        RelevantKravType.NY_STØNADSPERIODE -> false
                        RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> true
                        is RelevantKravType.GJENOPPTAK_ETTER_STANS -> true
                        else -> throw NotImplementedError()
                    },
                    harGjenværendeKvote = when (kravType) {
                        RelevantKravType.NY_STØNADSPERIODE -> false
                        RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> true
                        is RelevantKravType.GJENOPPTAK_ETTER_STANS -> true
                    }
                )
            )
        }



        kravRepsitory.lagre(behandling.id, test.map { it.first }.toSet())
        stønadsperiodeRepository.lagre(behandling.id, test.map { it.second }.toSet())
    }

}

