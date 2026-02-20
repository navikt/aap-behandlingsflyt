package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

fun Vurderingsbehov.tilKontraktVurderingsbehov(): no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov =
    when (this) {
        Vurderingsbehov.MOTTATT_SØKNAD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD
        Vurderingsbehov.MOTTATT_AKTIVITETSMELDING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AKTIVITETSMELDING
        Vurderingsbehov.MOTTATT_MELDEKORT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MELDEKORT
        Vurderingsbehov.MOTTATT_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LEGEERKLÆRING
        Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AVVIST_LEGEERKLÆRING
        Vurderingsbehov.MOTTATT_DIALOGMELDING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.DIALOGMELDING
        Vurderingsbehov.G_REGULERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.G_REGULERING
        Vurderingsbehov.BARNETILLEGG_SATS_REGULERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.BARNETILLEGG_SATS_REGULERING
        Vurderingsbehov.REVURDER_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_MEDLEMSKAP
        Vurderingsbehov.REVURDER_BEREGNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_BEREGNING
        Vurderingsbehov.REVURDER_YRKESSKADE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE
        Vurderingsbehov.REVURDER_LOVVALG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_LOVVALG
        Vurderingsbehov.REVURDER_SAMORDNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING
        Vurderingsbehov.REVURDER_STUDENT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_STUDENT
        Vurderingsbehov.MOTATT_KLAGE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.KLAGE
        Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP
        Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP
        Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        Vurderingsbehov.REVURDER_SYKEPENGEERSTATNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SYKEPENGEERSTATNING
        Vurderingsbehov.BARNETILLEGG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.BARNETILLEGG
        Vurderingsbehov.INSTITUSJONSOPPHOLD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.INSTITUSJONSOPPHOLD
        Vurderingsbehov.SAMORDNING_OG_AVREGNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SAMORDNING_OG_AVREGNING
        Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER
        Vurderingsbehov.REVURDER_SAMORDNING_UFØRE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_UFØRE
        Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER
        Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER
        Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON
        Vurderingsbehov.REFUSJONSKRAV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REFUSJONSKRAV
        Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT
        Vurderingsbehov.FASTSATT_PERIODE_PASSERT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MELDEKORT
        Vurderingsbehov.VURDER_RETTIGHETSPERIODE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_RETTIGHETSPERIODE
        Vurderingsbehov.SØKNAD_TRUKKET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET
        Vurderingsbehov.REVURDERING_AVBRUTT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDERING_AVBRUTT
        Vurderingsbehov.KLAGE_TRUKKET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.KLAGE_TRUKKET
        Vurderingsbehov.REVURDER_MANUELL_INNTEKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_MANUELL_INNTEKT
        Vurderingsbehov.FRITAK_MELDEPLIKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FRITAK_MELDEPLIKT
        Vurderingsbehov.MOTTATT_KABAL_HENDELSE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MOTTATT_KABAL_HENDELSE
        Vurderingsbehov.HELHETLIG_VURDERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.HELHETLIG_VURDERING
        Vurderingsbehov.OPPFØLGINGSOPPGAVE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OPPFØLGINGSOPPGAVE
        Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN
        Vurderingsbehov.AKTIVITETSPLIKT_11_7 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AKTIVITETSPLIKT_11_7
        Vurderingsbehov.AKTIVITETSPLIKT_11_9 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.AKTIVITETSPLIKT_11_9
        Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT
        Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9
        Vurderingsbehov.OPPHOLDSKRAV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OPPHOLDSKRAV
        Vurderingsbehov.OVERGANG_UFORE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_UFORE
        Vurderingsbehov.OVERGANG_ARBEID -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_ARBEID
        Vurderingsbehov.DØDSFALL_BRUKER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.DØDSFALL_BRUKER
        Vurderingsbehov.DØDSFALL_BARN -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.DØDSFALL_BARN
        Vurderingsbehov.UTVID_VEDTAKSLENGDE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.UTVID_VEDTAKSLENGDE
        Vurderingsbehov.MIGRER_RETTIGHETSPERIODE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.MIGRER_RETTIGHETSPERIODE
        Vurderingsbehov.REVURDER_SYKESTIPEND -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SYKESTIPEND
        Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET
    }


internal fun Tilkjent.tilKontrakt(): no.nav.aap.behandlingsflyt.kontrakt.statistikk.Minstesats =
    when (this.minsteSats) {
        Minstesats.IKKE_MINSTESATS -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Minstesats.IKKE_MINSTESATS
        Minstesats.MINSTESATS_OVER_25 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Minstesats.MINSTESATS_OVER_25
        Minstesats.MINSTESATS_UNDER_25 -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.Minstesats.MINSTESATS_UNDER_25
    }


internal fun RettighetsType?.tilKontrakt(): no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType =
    when (requireNotNull(this)) {
        RettighetsType.BISTANDSBEHOV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.BISTANDSBEHOV
        RettighetsType.SYKEPENGEERSTATNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.SYKEPENGEERSTATNING
        RettighetsType.STUDENT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.STUDENT
        RettighetsType.ARBEIDSSØKER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.ARBEIDSSØKER
        RettighetsType.VURDERES_FOR_UFØRETRYGD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType.VURDERES_FOR_UFØRETRYGD
    }

