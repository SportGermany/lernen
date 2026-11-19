package de.lernen.core.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Lädt wartende Fragen hoch, sobald Netz da ist. */
@HiltWorker
class FragenHochladenWorker @AssistedInject constructor(
    @Assisted ctx: Context, @Assisted p: WorkerParameters, private val fragen: FragenRepository,
) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result = if (fragen.hochladen()) Result.success() else Result.retry()
}

/** Löscht Fragen-Dateien 24 h nach dem Erstellen (mindestens alle 6 h). */
@HiltWorker
class FragenAufraeumenWorker @AssistedInject constructor(
    @Assisted ctx: Context, @Assisted p: WorkerParameters, private val fragen: FragenRepository,
) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        fragen.hochladen()
        return if (fragen.aufraeumen()) Result.success() else Result.retry()
    }
}

/** Tägliche Erinnerung. */
@HiltWorker
class ErinnerungWorker @AssistedInject constructor(
    @Assisted private val ctx: Context, @Assisted p: WorkerParameters,
) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        Benachrichtigung.zeige(ctx)
        return Result.success()
    }
}

object Benachrichtigung {
    const val KANAL = "erinnerung"
    private const val ID = 1

    fun kanalAnlegen(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(KANAL, "Tägliche Erinnerung", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun zeige(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        kanalAnlegen(ctx)
        val start = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(ctx, 0, start, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, KANAL)
            .setSmallIcon(R.drawable.ic_lernen_benachrichtigung)
            .setContentTitle("Lernen")
            .setContentText("Zeit für eine kleine Lerneinheit")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID, n)
    }
}

@Singleton
class Hintergrund @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val wm get() = WorkManager.getInstance(ctx)
    private val mitNetz = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun fragenHochladen() = wm.enqueueUniqueWork(
        "fragen-hochladen", ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<FragenHochladenWorker>().setConstraints(mitNetz)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build(),
    )

    fun aufraeumenPlanen() = wm.enqueueUniquePeriodicWork(
        "fragen-aufraeumen", ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<FragenAufraeumenWorker>(6, TimeUnit.HOURS).setConstraints(mitNetz).build(),
    )

    fun erinnerungPlanen(an: Boolean, minuten: Int) {
        if (!an) { wm.cancelUniqueWork("erinnerung"); return }
        val req = PeriodicWorkRequestBuilder<ErinnerungWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(verzoegerungBis(minuten, LocalDateTime.now()).toMillis(), TimeUnit.MILLISECONDS).build()
        wm.enqueueUniquePeriodicWork("erinnerung", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, req)
    }

    companion object {
        /** Zeit bis zur nächsten Uhrzeit [minuten] nach Mitternacht (heute oder morgen). */
        fun verzoegerungBis(minuten: Int, jetzt: LocalDateTime): Duration {
            var ziel = jetzt.toLocalDate().atTime(LocalTime.of(minuten / 60, minuten % 60))
            if (!ziel.isAfter(jetzt)) ziel = ziel.plusDays(1)
            return Duration.between(jetzt, ziel)
        }
    }
}
