package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaStatusResponse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArenaVedtaksvariantDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArenavedtakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.AvslagsårsakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.GjeldendeStansEllerOpphørDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.StansEllerOpphørEnumDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.datadeling.UtledArenaVedtakstype
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class FakeApiInternGateway : ApiInternGateway {
    companion object : Factory<ApiInternGateway> {
        override fun konstruer(): ApiInternGateway {
            return FakeApiInternGateway()
        }
        // No-op
    }

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
        // No-op
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
        // No-op
    }

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long,
        samId: String?,
        tilkjent: List<TilkjentYtelsePeriode>,
        beregningsgrunnlag: BigDecimal?,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<RettighetsType>,
        stansOpphørGrunnlag: Set<GjeldendeStansEllerOpphør>?,
        arenavedtak: Tidslinje<UtledArenaVedtakstype.ArenaVedtak>,
        muligMaksdato: LocalDate?
    ) {
        // No-op
        log.info("Sender behandling for behandlingId=${behandling.id} med vedtakId=$vedtakId, sak: ${sak.saksnummer}. Beregningsgrunnlag: $beregningsgrunnlag")
        val body = DatadelingDTO(
            behandlingsId = behandling.id.id.toString(),
            behandlingsReferanse = behandling.referanse.toString(),
            rettighetsPeriodeFom = sak.rettighetsperiode.fom,
            rettighetsPeriodeTom = sak.rettighetsperiode.tom,
            behandlingStatus = behandling.status(),
            vedtaksDato = vedtaksDato,
            beregningsgrunnlag = beregningsgrunnlag,
            sak = SakDTO(
                saksnummer = sak.saksnummer.toString(),
                fnr = sak.person.identer().map { ident -> ident.identifikator },
                opprettetTidspunkt = sak.opprettetTidspunkt
            ),
            tilkjent = tilkjent.map { tilkjentPeriode ->
                TilkjentDTO(
                    tilkjentFom = tilkjentPeriode.periode.fom,
                    tilkjentTom = tilkjentPeriode.periode.tom,
                    dagsats = tilkjentPeriode.tilkjent.dagsats.verdi.toInt(),
                    // legg til redusert dagsats
                    gradering = tilkjentPeriode.tilkjent.gradering.prosentverdi(),
                    samordningUføregradering = tilkjentPeriode.tilkjent.graderingGrunnlag.samordningUføregradering.prosentverdi(),
                    grunnlagsfaktor = tilkjentPeriode.tilkjent.grunnlagsfaktor.verdi(),
                    grunnbeløp = tilkjentPeriode.tilkjent.grunnbeløp.verdi,
                    antallBarn = tilkjentPeriode.tilkjent.antallBarn,
                    barnetilleggsats = tilkjentPeriode.tilkjent.barnetilleggsats.verdi,
                    barnetillegg = tilkjentPeriode.tilkjent.barnetillegg.verdi,
                )
            },
            rettighetsTypeTidsLinje = rettighetsTypeTidslinje.segmenter().map { segment ->
                RettighetsTypePeriode(
                    segment.periode.fom,
                    segment.periode.tom,
                    segment.verdi.toString()
                )
            },
            muligMaksdato = muligMaksdato,
            vedtakId = vedtakId,
            samId = samId,
            stansOpphørVurdering = stansOpphørGrunnlag.orEmpty().map {
                GjeldendeStansEllerOpphørDTO(
                    fom = it.fom,
                    opprettet = it.opprettet,
                    vurdering = when (it.vurdering) {
                        is Stans -> StansEllerOpphørEnumDTO.STANS
                        is Opphør -> StansEllerOpphørEnumDTO.OPPHØR
                    },
                    avslagsårsaker = it.vurdering.årsaker.mapNotNull { årsak -> mapAvslagsårsak(årsak) }
                        .toSet(),
                )
            }.toSet(),
            arenavedtak = arenavedtak.segmenter().map {
                ArenavedtakDTO(
                    vedtakId = it.verdi.vedtakId.id,
                    fom = it.fom(),
                    tom = it.tom(),
                    vedtaksvariant = when (it.verdi.vedtaksvariant) {
                        UtledArenaVedtakstype.ArenaVedtaksvariant.O_AVSLAG -> ArenaVedtaksvariantDTO.O_AVSLAG
                        UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_NAV -> ArenaVedtaksvariantDTO.O_INNV_NAV
                        UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD -> ArenaVedtaksvariantDTO.O_INNV_SOKNAD
                        UtledArenaVedtakstype.ArenaVedtaksvariant.E_FORLENGE -> ArenaVedtaksvariantDTO.E_FORLENGE
                        UtledArenaVedtakstype.ArenaVedtaksvariant.E_VERDI -> ArenaVedtaksvariantDTO.E_VERDI
                        UtledArenaVedtakstype.ArenaVedtaksvariant.G_AVSLAG -> ArenaVedtaksvariantDTO.G_AVSLAG
                        UtledArenaVedtakstype.ArenaVedtaksvariant.G_INNV_NAV -> ArenaVedtaksvariantDTO.G_INNV_NAV
                        UtledArenaVedtakstype.ArenaVedtaksvariant.G_INNV_SOKNAD -> ArenaVedtaksvariantDTO.G_INNV_SOKNAD
                        UtledArenaVedtakstype.ArenaVedtaksvariant.S_DOD -> ArenaVedtaksvariantDTO.S_DOD
                        UtledArenaVedtakstype.ArenaVedtaksvariant.S_OPPHOR -> ArenaVedtaksvariantDTO.S_OPPHOR
                        UtledArenaVedtakstype.ArenaVedtaksvariant.S_STANS -> ArenaVedtaksvariantDTO.S_STANS
                    }
                )
            },
        )

        val json = DefaultJsonMapper.toJson(body)
        log.info("Sender behandling med body=$json")
    }

    override fun sendDetaljertMeldekortListe(
        detaljertMeldekortListe: List<DetaljertMeldekortDTO>,
        sakId: SakId,
        behandlingId: BehandlingId
    ) {
        // No-op
    }

    override fun hentArenaStatus(personidentifikatorer: Set<String>): Result<ArenaStatusResponse> {
        return Result.success(ArenaStatusResponse(false))
    }

    override fun oppdaterIdenter(
        saksnummer: Saksnummer,
        identer: List<Ident>
    ) {
        // No-op
    }

    fun mapAvslagsårsak(it: Avslagsårsak): AvslagsårsakDTO? {
        return when (it) {
            Avslagsårsak.BRUKER_OVER_67 -> AvslagsårsakDTO.BRUKER_OVER_67
            Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING -> AvslagsårsakDTO.IKKE_RETT_PA_SYKEPENGEERSTATNING
            Avslagsårsak.IKKE_RETT_PA_STUDENT -> AvslagsårsakDTO.IKKE_RETT_PA_STUDENT
            Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT -> AvslagsårsakDTO.VARIGHET_OVERSKREDET_STUDENT
            Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET -> AvslagsårsakDTO.IKKE_SYKDOM_AV_VISS_VARIGHET
            Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL -> AvslagsårsakDTO.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE -> AvslagsårsakDTO.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING -> AvslagsårsakDTO.IKKE_BEHOV_FOR_OPPFOLGING
            Avslagsårsak.IKKE_MEDLEM -> AvslagsårsakDTO.IKKE_MEDLEM
            Avslagsårsak.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS -> AvslagsårsakDTO.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS
            Avslagsårsak.ANNEN_FULL_YTELSE -> AvslagsårsakDTO.ANNEN_FULL_YTELSE
            Avslagsårsak.INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING -> AvslagsårsakDTO.INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING
            Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE -> AvslagsårsakDTO.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE -> AvslagsårsakDTO.VARIGHET_OVERSKREDET_OVERGANG_UFORE
            Avslagsårsak.VARIGHET_OVERSKREDET_ARBEIDSSØKER -> AvslagsårsakDTO.VARIGHET_OVERSKREDET_ARBEIDSSØKER
            Avslagsårsak.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER -> AvslagsårsakDTO.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER
            Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING -> AvslagsårsakDTO.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING
            Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS -> AvslagsårsakDTO.BRUDD_PÅ_AKTIVITETSPLIKT_STANS
            Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR -> AvslagsårsakDTO.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS -> AvslagsårsakDTO.BRUDD_PÅ_OPPHOLDSKRAV_STANS
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR -> AvslagsårsakDTO.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR
            Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP -> AvslagsårsakDTO.ORDINÆRKVOTE_BRUKT_OPP
            Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP -> AvslagsårsakDTO.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
            Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE -> AvslagsårsakDTO.IKKE_SYKDOM_SKADE_LYTE
            Avslagsårsak.BRUKER_UNDER_18 -> null
            Avslagsårsak.MANGLENDE_DOKUMENTASJON -> null
            Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE -> null
            Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT -> null
            Avslagsårsak.HAR_RETT_TIL_FULLT_UTTAK_ALDERSPENSJON -> null
        }
    }

}