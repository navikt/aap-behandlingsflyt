package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering

data class FatteVedtakGrunnlagDto(val vurderinger: List<TotrinnsVurdering>, val historikk: List<Historikk>)
