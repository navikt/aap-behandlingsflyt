package no.nav.aap.misc.institusjonsopphold

data class InstitusjonsoppholdGrunnlag(
    val oppholdene: Oppholdene? = null,
    val soningsVurderinger: Soningsvurderinger? = null,
    val helseoppholdvurderinger: Helseoppholdvurderinger? = null
)