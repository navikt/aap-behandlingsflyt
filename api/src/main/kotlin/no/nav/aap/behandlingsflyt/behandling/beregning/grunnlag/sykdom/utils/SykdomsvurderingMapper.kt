package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.utils

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import java.time.ZoneId


    fun Sykdomsvurdering.tilResponse(ansattInfoService: AnsattInfoService): SykdomsvurderingResponse {
        val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv.ident)
        return SykdomsvurderingResponse(
            begrunnelse = begrunnelse,
            vurderingenGjelderFra = vurderingenGjelderFra,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            kodeverk = kodeverk,
            hoveddiagnose = hoveddiagnose,
            bidiagnoser = bidiagnoser,
            vurdertAv = VurdertAvResponse(
                ident = vurdertAv.ident,
                dato = opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet,
            )
        )
    }
