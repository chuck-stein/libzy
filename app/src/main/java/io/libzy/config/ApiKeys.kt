package io.libzy.config

import android.content.Context
import io.libzy.R

interface ApiKeys {
    val amplitudeApiKey: String
}

class ProdApiKeys(applicationContext: Context): ApiKeys {

    override val amplitudeApiKey = applicationContext.getString(R.string.amplitude_api_key_prod)
    
}

class DevApiKeys(applicationContext: Context): ApiKeys {

    override val amplitudeApiKey = applicationContext.getString(R.string.amplitude_api_key_dev)

}
