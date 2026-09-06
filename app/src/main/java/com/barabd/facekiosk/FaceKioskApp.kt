package com.barabd.facekiosk

import android.app.Application
import com.barabd.facekiosk.data.AppDatabase
import com.barabd.facekiosk.data.PersonRepository
import com.barabd.facekiosk.data.PunchOutboxRepository
import com.barabd.facekiosk.ml.FacePipeline
import com.barabd.facekiosk.net.ErpApiClient
import com.barabd.facekiosk.security.PinStore
import com.barabd.facekiosk.settings.KioskPreferences

class FaceKioskApp : Application() {
    lateinit var prefs: KioskPreferences
        private set
    lateinit var pinStore: PinStore
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var personRepository: PersonRepository
        private set
    lateinit var outboxRepository: PunchOutboxRepository
        private set
    lateinit var apiClient: ErpApiClient
        private set

    val facePipeline: FacePipeline by lazy { FacePipeline(this) }

    override fun onCreate() {
        super.onCreate()
        prefs = KioskPreferences(this)
        pinStore = PinStore(this)
        database = AppDatabase.get(this)
        personRepository = PersonRepository(database.personDao())
        outboxRepository = PunchOutboxRepository(database.punchOutboxDao())
        apiClient = ErpApiClient(prefs)
    }
}
