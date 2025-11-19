package no.nav.aap.behandlingsflyt.repository

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak.GjenopptakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.mellomlagring.MellomlagretVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.Reduksjon11_9RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.samid.SamIdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.InformasjonskravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomsvurderingForBrevRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningAndreStatligeYtelserRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.MeldekortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.BehandlendeEnhetRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FullmektigRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.KlagebehandlingKontorRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.KlagebehandlingNayRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.PåklagetBehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.TrekkKlageRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.overgangarbeid.OvergangArbeidRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt.InntektGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.medlemsskap.MedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.opphold.OppholdskravGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsgiver.SamordningArbeidsgiverRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre.OvergangUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode.VurderRettighetsperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student.StudentRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.svarfraanadreinstans.SvarFraAndreinstansRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.log.ContextRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl

val postgresRepositoryRegistry = RepositoryRegistry()
    .register<PersonRepositoryImpl>()
    .register<SakRepositoryImpl>()
    .register<AvklaringsbehovRepositoryImpl>()
    .register<VilkårsresultatRepositoryImpl>()
    .register<PipRepositoryImpl>()
    .register<TaSkriveLåsRepositoryImpl>()
    .register<BeregningsgrunnlagRepositoryImpl>()
    .register<PersonopplysningRepositoryImpl>()
    .register<TilkjentYtelseRepositoryImpl>()
    .register<BrevbestillingRepositoryImpl>()
    .register<SamordningRepositoryImpl>()
    .register<MottattDokumentRepositoryImpl>()
    .register<MeldekortRepositoryImpl>()
    .register<UnderveisRepositoryImpl>()
    .register<ArbeidsevneRepositoryImpl>()
    .register<ArbeidsopptrappingRepositoryImpl>()
    .register<BarnetilleggRepositoryImpl>()
    .register<ContextRepositoryImpl>()
    .register<BistandRepositoryImpl>()
    .register<BeregningVurderingRepositoryImpl>()
    .register<SykdomRepositoryImpl>()
    .register<YrkesskadeRepositoryImpl>()
    .register<UføreRepositoryImpl>()
    .register<MedlemskapArbeidInntektRepositoryImpl>()
    .register<SykepengerErstatningRepositoryImpl>()
    .register<SamordningVurderingRepositoryImpl>()
    .register<SamordningYtelseRepositoryImpl>()
    .register<SamordningUføreRepositoryImpl>()
    .register<SamordningAndreStatligeYtelserRepositoryImpl>()
    .register<SamordningArbeidsgiverRepositoryImpl>()
    .register<StudentRepositoryImpl>()
    .register<MeldepliktRepositoryImpl>()
    .register<MedlemskapArbeidInntektForutgåendeRepositoryImpl>()
    .register<PersonopplysningForutgåendeRepositoryImpl>()
    .register<BarnRepositoryImpl>()
    .register<InstitusjonsoppholdRepositoryImpl>()
    .register<InntektGrunnlagRepositoryImpl>()
    .register<MeldeperiodeRepositoryImpl>()
    .register<VedtakRepositoryImpl>()
    .register<RefusjonkravRepositoryImpl>()
    .register<InformasjonskravRepositoryImpl>()
    .register<TjenestePensjonRepositoryImpl>()
    .register<TrukketSøknadRepositoryImpl>()
    .register<VurderRettighetsperiodeRepositoryImpl>()
    .register<FlytJobbRepositoryImpl>()
    .register<FormkravRepositoryImpl>()
    .register<PåklagetBehandlingRepositoryImpl>()
    .register<BehandlendeEnhetRepositoryImpl>()
    .register<MedlemskapRepositoryImpl>()
    .register<MedlemskapForutgåendeRepositoryImpl>()
    .register<TjenestepensjonRefusjonskravVurderingRepositoryImpl>()
    .register<BehandlingRepositoryImpl>()
    .register<KlagebehandlingKontorRepositoryImpl>()
    .register<KlagebehandlingNayRepositoryImpl>()
    .register<TrekkKlageRepositoryImpl>()
    .register<GjenopptakRepositoryImpl>()
    .register<ManuellInntektGrunnlagRepositoryImpl>()
    .register<SamIdRepositoryImpl>()
    .register<SvarFraAndreinstansRepositoryImpl>()
    .register<FullmektigRepositoryImpl>()
    .register<OppfølgingsBehandlingRepositoryImpl>()
    .register<OverstyringMeldepliktRepositoryImpl>()
    .register<SykdomsvurderingForBrevRepositoryImpl>()
    .register<MellomlagretVurderingRepositoryImpl>()
    .register<Aktivitetsplikt11_7RepositoryImpl>()
    .register<OvergangUføreRepositoryImpl>()
    .register<OvergangArbeidRepositoryImpl>()
    .register<OppholdskravGrunnlagRepositoryImpl>()
    .register<AvbrytRevurderingRepositoryImpl>()
    .register<Aktivitetsplikt11_9RepositoryImpl>()
    .register<Reduksjon11_9RepositoryImpl>()

