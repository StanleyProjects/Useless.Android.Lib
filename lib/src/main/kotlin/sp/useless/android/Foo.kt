package sp.useless.android

import android.content.Context
import java.net.URI

fun Context.cacheUri(): URI {
    return cacheDir.toURI()
}
