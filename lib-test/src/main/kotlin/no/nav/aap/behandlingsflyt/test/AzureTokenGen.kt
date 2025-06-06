package no.nav.aap.behandlingsflyt.test

import ch.qos.logback.core.util.OptionHelper.getEnv
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.intellij.lang.annotations.Language
import java.lang.System.getProperty
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AzureTokenGen(private val issuer: String, private val audience: String) {
    private val rsaKey: RSAKey = JWKSet.parse(AZURE_JWKS).getKeyByKeyId("localhost-signer") as RSAKey

    private fun signed(claims: JWTClaimsSet): SignedJWT {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).type(JOSEObjectType.JWT).build()
        val signer = RSASSASigner(rsaKey.toPrivateKey())
        val signedJWT = SignedJWT(header, claims)
        signedJWT.sign(signer)
        return signedJWT
    }

    private fun claims(isApp: Boolean, azp: String?, NAVident: String?): JWTClaimsSet {
        val builder = JWTClaimsSet
            .Builder()
            .subject(UUID.randomUUID().toString())
            .issuer(issuer)
            .audience(audience)
            .expirationTime(LocalDateTime.now().plusHours(4).toDate())

        if (isApp) {
            builder
                .claim("idtyp", "app")
                .claim(
                    "roles", listOf(
                        "opprett-sak",
                        "hent-personinfo",
                        "bestill-varselbrev",
                        "brev",
                        "pip-api",
                        "medlemskaplovvalg-api"
                    )
                )
                .claim(
                    "azp", azp
                )
        } else {
            builder.claim("NAVident", NAVident ?: "X123456")
        }

        return builder.build()
    }

    private fun LocalDateTime.toDate(): Date {
        return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
    }

    fun generate(isApp: Boolean, azp: String, NAVident: String? = null): String {
        return signed(claims(isApp, azp, NAVident)).serialize()
    }
}

@Language("JSON")
internal const val AZURE_JWKS: String = """{
  "keys": [
    {
      "kty": "RSA",
      "d": "O4HE82G7UP-KVIryTboX-VqbxBbSo16_shQ-zIGUiHo0DVoTBJYfmRWSIx4bPT-n80imaYhohHd79UO1lqWMF-GrZdFJaYjU7yzKGc_W7Pw5QVVng9JZRlgIuz_L7Zl-q3R1gV0-FZWhRZtkhIbETl8216cBFjSrUVF04Fpv4n9dBV3ySgjfG_0MMuysAWx6gZFyP2g1IOnuCY7v32kLR9wdLWPSKFz-icm66AR5DX0hyMdUuwQ56DEBAzf6-1MqznqKiwg-whL6zcHHLdaWzj02J8bMLpeZ9PylbxdTHEWdMP6HaXNdqVMx920UnWmCVVcFOIxs53PdGnyJThVPEQ",
      "e": "AQAB",
      "use": "sig",
      "kid": "localhost-signer",
      "alg": "RS256",
      "n": "wQkxSymiZJ8k4zBTo8HhjmvMB-OZl6F1qg_ZsPXwfa8jTzxbxkicAAPKowh7T0vT_dQAR_Vhy9G6v2jkUUnlbvxULqOt395TTUEB-MBPb0gxIk9O65Ws9eRj12hWo6gDaHBuxWEEjzvVHEDAmqHs7mswoY7nkn2ktxYDPdCjKystyCyR6TCMxkOMXLt0gUfdZyGir60d4ZsGeSIV66L2_pGI0qsEELGvXCLKQe7-UceyYioxmjRs_GGl8Zd1psSiXiZYXHUYIIslZakPUPNUM5_2eFwTbwQPybhJ0WLqUxWEGfoZjyMflR0FTTo5ZLOKLZAsXCpZlR7nY_tuMNWWhw"
    }
  ]
}"""
