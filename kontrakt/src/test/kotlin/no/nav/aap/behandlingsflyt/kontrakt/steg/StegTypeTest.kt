package no.nav.aap.behandlingsflyt.kontrakt.steg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StegTypeTest {

    @Test
    fun `tidligere persisterte stegtyper skal fortsatt kunne leses`() {
        val nåværendeStegtyper = StegType.entries.map { it.name }.toSet()
        val manglendeStegtyper = persisterteStegtyper - nåværendeStegtyper

        assertThat(manglendeStegtyper)
            .withFailMessage(
                "StegType-verdier som kan finnes i databasen er fjernet eller omdøpt: %s. " +
                    "Ikke fjern dem kun fra testen; migrer eller verifiser eksisterende data først.",
                manglendeStegtyper.joinToString(),
            )
            .isEmpty()

        val uregistrerteStegtyper = nåværendeStegtyper - persisterteStegtyper
        assertThat(uregistrerteStegtyper)
            .withFailMessage(
                "Nye StegType-verdier må legges til i persisterteStegtyper slik at senere fjerning oppdages: %s.",
                uregistrerteStegtyper.joinToString(),
            )
            .isEmpty()
    }

    private val persisterteStegtyper = setOf(
        "START_BEHANDLING",
        "AVKLAR_MIGRERINGSDATO",
        "KRAV",
        "AVKLAR_STØNADSPERIODE",
        "SEND_FORVALTNINGSMELDING",
        "AVBRYT_REVURDERING",
        "SØKNAD",
        "VURDER_RETTIGHETSPERIODE",
        "VURDER_LOVVALG",
        "FASTSETT_MELDEPERIODER",
        "VURDER_ALDER",
        "VURDER_AVSLAG_11_27",
        "AVKLAR_STUDENT",
        "AVKLAR_STUDENT_V2",
        "AVKLAR_SYKDOM",
        "VURDER_BISTANDSBEHOV",
        "ETABLERING_EGEN_VIRKSOMHET",
        "ARBEIDSOPPTRAPPING",
        "FRITAK_MELDEPLIKT",
        "FASTSETT_ARBEIDSEVNE",
        "OVERGANG_UFORE",
        "OVERGANG_ARBEID",
        "REFUSJON_KRAV",
        "SYKDOMSVURDERING_BREV",
        "BEKREFT_VURDERINGER_OPPFØLGING",
        "KVALITETSSIKRING",
        "VURDER_YRKESSKADE",
        "VURDER_SYKEPENGEERSTATNING",
        "FASTSETT_SYKDOMSVILKÅRET",
        "FASTSETT_BEREGNINGSTIDSPUNKT",
        "MANGLENDE_LIGNING",
        "FASTSETT_GRUNNLAG",
        "VURDER_INNTEKTSBORTFALL",
        "VURDER_MEDLEMSKAP",
        "VURDER_OPPHOLDSKRAV",
        "BARNETILLEGG",
        "DU_ER_ET_ANNET_STED",
        "SAMORDNING_GRADERING",
        "SAMORDNING_UFØRE",
        "SAMORDNING_TJENESTEPENSJON_REFUSJONSKRAV",
        "SAMORDNING_ARBEIDSGIVER",
        "SAMORDNING_AVSLAG",
        "SAMORDNING_BARNEPENSJON",
        "SAMORDNING_SYKESTIPEND",
        "SAMORDNING_ANDRE_STATLIGE_YTELSER",
        "EFFEKTUER_11_7",
        "FASTSETT_RETTIGHETSTYPE",
        "FASTSETT_VEDTAKSLENGDE",
        "FORESLÅ_VEDTAK_VEDTAKSLENGDE",
        "IKKE_OPPFYLT_MELDEPLIKT",
        "FASTSETT_UTTAK",
        "BEREGN_TILKJENT_YTELSE",
        "SIMULERING",
        "FORESLÅ_VEDTAK",
        "FATTE_VEDTAK",
        "IVERKSETT_VEDTAK",
        "BREV",
        "PÅKLAGET_BEHANDLING",
        "FULLMEKTIG",
        "FORMKRAV",
        "BEHANDLENDE_ENHET",
        "KLAGEBEHANDLING_KONTOR",
        "KLAGEBEHANDLING_NAY",
        "KLAGEBEHANDLING_OPPSUMMERING",
        "OMGJØRING",
        "TREKK_KLAGE",
        "OPPRETTHOLDELSE",
        "SVAR_FRA_ANDREINSTANS",
        "IVERKSETT_KONSEKVENS",
        "START_OPPFØLGINGSBEHANDLING",
        "AVKLAR_OPPFØLGING",
        "VURDER_AKTIVITETSPLIKT_11_7",
        "IVERKSETT_BRUDD",
        "VURDER_AKTIVITETSPLIKT_11_9",
        "AVBRYT_AKTIVITETSPLIKTBEHANDLING",
        "UDEFINERT",
        "OPPRETT_REVURDERING",
        "VIS_GRUNNLAG",
    )
}
