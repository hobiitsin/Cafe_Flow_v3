package com.example.cafe_flow_v3

import android.app.Application
import android.util.Log
import com.example.cafe_flow_v3.data.AppContainer
import com.example.cafe_flow_v3.data.DatosPrueba
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Se crea UNA vez cuando arranca la app, antes que cualquier pantalla.
 */
class CafeFlowApplication : Application() {

    // Base de datos + repositorios.
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        applicationScope.launch {
            try {
                DatosPrueba.poblarSiVacia(container.database)
                Log.d(TAG, "Base de datos lista")
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar la base de datos", e)
            }
        }
    }

    companion object {
        const val TAG = "CafeFlow"
    }
}
