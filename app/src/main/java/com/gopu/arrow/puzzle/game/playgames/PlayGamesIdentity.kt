package com.gopu.arrow.puzzle.game.playgames

import android.app.Activity
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.Player
import com.google.android.gms.tasks.Task
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val Tag = "PlayGames"

/** The Play Store package, which is what actually brokers a Play Games session. */
private const val PlayStorePackage = "com.android.vending"

/**
 * How long the whole authentication phase may take before the player is treated
 * as a guest.
 *
 * A [Task] from the Play Games SDK carries no timeout of its own, so this is
 * what stops a service that never answers - offline, throttled, or simply
 * missing - from leaving the attempt pending forever. It is a ceiling rather
 * than a budget, and it is far longer than a healthy round-trip needs: the
 * phase finishes the moment Google answers.
 */
private const val AttemptTimeoutMillis = 10_000L

/**
 * The player's Google Play Games identity, if they have one.
 *
 * This is strictly additive. Arrow Puzzle's own progression is owned by
 * `ProgressRepository` and stored in DataStore, and nothing here reads, writes
 * or resets it: a player who authenticates, a player who fails to, and a player
 * with no Play Games account at all all keep exactly the same local level. Play
 * Games is never told what level the player is on, because there is nothing
 * here that would report it.
 *
 * The attempt is made once per process by [signInSilently] and is entirely
 * fire-and-forget - nothing waits on it, so the menu is on screen whether the
 * answer takes a second or never arrives. A caller wants [playerName] /
 * [playerId], which stay null until, and unless, Google supplies them.
 *
 * The order of business is deliberately Google's own:
 *
 * 1. [PlayGamesSdk.initialize] wires the SDK up without UI.
 * 2. `isAuthenticated()` asks whether there is already a signed-in Play Games
 *    player. When there is - the normal case for anyone who has opened the game
 *    before - that is the entire flow and it is completely silent.
 * 3. Only when there is not does `signIn()` run. It is the one call that can put
 *    a screen up, it is Google's own screen rather than a stand-in, and Google
 *    decides whether it is even needed: for a player it can identify without
 *    asking, the call completes straight away. Dismissing it leaves the player a
 *    guest and the game fully playable.
 *
 * Every other outcome - no Play Games, no account, a decline, a timeout, an
 * offline device - collapses to the same one: guest, no dialog, no error
 * surface. [isAuthenticated] only ever becomes true on Google's own answer.
 */
object PlayGamesIdentity {

    private val _isAuthenticated = MutableStateFlow(false)

    /** Whether Google reported a signed-in Play Games player. False means guest. */
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _playerName = MutableStateFlow<String?>(null)

    /** The player's Play Games display name, or null while still a guest. */
    val playerName: StateFlow<String?> = _playerName.asStateFlow()

    private val _playerId = MutableStateFlow<String?>(null)

    /** The player's opaque Play Games player ID, or null while still a guest. */
    val playerId: StateFlow<String?> = _playerId.asStateFlow()

    /**
     * One attempt per process, claimed before any work starts so that a second
     * foreground pass - or a call from anywhere else - cannot start a second
     * one. Nothing resets it: an attempt that failed stays failed for the life
     * of the process rather than nagging the player on every return.
     */
    private var claimed = false

    /**
     * The activity the attempt is running against, held weakly.
     *
     * Play Games needs an activity to build its clients and to host its own
     * resolution screen, but a process-lifetime singleton holding one strongly
     * would outlive the activity it was handed. The reference is dropped again
     * as soon as the attempt finishes, so nothing keeps a destroyed activity -
     * or its view tree - alive, and work that resumes late finds null rather
     * than something stale to touch.
     */
    private var activityRef: WeakReference<Activity>? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Starts this process's single silent attempt. Safe and cheap to call from
     * every foreground pass: it does nothing at all after the first, so neither
     * a resume nor a configuration change can re-trigger authentication.
     *
     * The activity is borrowed for the length of the attempt and never retained,
     * so calling this from `onStart` cannot leak it.
     */
    fun signInSilently(activity: Activity) {
        if (claimed) return
        claimed = true
        activityRef = WeakReference(activity)

        scope.launch {
            try {
                /*
                 * Idempotent and with no UI of its own. The SDK also initializes itself
                 * from a ContentProvider before Application.onCreate, so this is belt and
                 * braces rather than a dependency on that provider having run.
                 */
                runCatching { PlayGamesSdk.initialize(activity.applicationContext) }
                    .onFailure { Log.w(Tag, "PlayGamesSdk.initialize failed; staying a guest", it) }

                val host = liveActivity()
                if (host == null) {
                    Log.i(Tag, "No live activity to sign in against; staying a guest")
                    return@launch
                }

                if (!authenticate(host)) return@launch

                val player = loadPlayer(host)
                if (player == null) {
                    Log.w(Tag, "Authenticated but the player could not be read; staying a guest")
                    return@launch
                }

                /*
                 * Name and ID are published together and from one place, so there is no
                 * window in which the menu shows a name with no player behind it.
                 */
                _playerId.value = player.playerId
                _playerName.value = player.displayName
                _isAuthenticated.value = true
                Log.i(Tag, "Play Games player resolved: id=${player.playerId} name=${player.displayName}")
            } catch (error: Throwable) {
                /*
                 * Play Games is an optional extra, so nothing in here is allowed to take
                 * the app down with it. Any failure collapses to a guest, which is a
                 * complete and playable state.
                 */
                Log.w(Tag, "Play Games attempt failed; continuing as a guest", error)
            } finally {
                activityRef = null
            }
        }
    }

    /**
     * The silent half of the flow: whether there is already a player, otherwise a
     * single request for Google to sign one in. False means guest.
     *
     * The device is checked before either call. Asking a phone that cannot reach
     * Play Games to sign in is the one path that can put a failure in front of a
     * player who never asked for anything, so it is never asked.
     */
    private suspend fun authenticate(activity: Activity): Boolean =
        withTimeoutOrNull(AttemptTimeoutMillis) {
            if (!playGamesReachable(activity)) {
                Log.i(Tag, "Google Play services unavailable; staying a guest")
                return@withTimeoutOrNull false
            }

            val signIn = PlayGames.getGamesSignInClient(activity)

            /*
             * The silent question first. A player Google already knows answers it
             * without a single pixel of UI, which is the overwhelmingly common case.
             */
            val existing = signIn.isAuthenticated.awaitOrNull().getOrNull()
            if (existing != null && existing.isAuthenticated) {
                Log.i(Tag, "Already signed in to Play Games; no UI shown")
                return@withTimeoutOrNull true
            }
            existing?.let { Log.i(Tag, "Play Games reports no signed-in player") }

            /*
             * Not signed in. This is the only call that can show anything, and what it
             * shows is Google's own screen rather than a stand-in for one - and only
             * when Google needs to ask. Nothing is blocked while it is up: dismissing it
             * simply leaves the player a guest.
             */
            val result = signIn.signIn().awaitOrNull()
            result
                .onFailure { error -> Log.w(Tag, "Play Games sign-in failed", error) }
                .getOrNull()
                ?.isAuthenticated
                ?: false
        } ?: run {
            Log.i(Tag, "Play Games did not answer within ${AttemptTimeoutMillis}ms; staying a guest")
            false
        }

    /** Whether this device can reach Play Games at all. */
    private fun playGamesReachable(activity: Activity): Boolean {
        val status = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        }.getOrElse {
            Log.w(Tag, "Play services availability check failed", it)
            return false
        }
        val storeInstalled = runCatching {
            activity.packageManager.getPackageInfo(PlayStorePackage, 0)
            true
        }.getOrDefault(false)

        return status == ConnectionResult.SUCCESS && storeInstalled
    }

    /**
     * Reads the player Google's sign-in produced. Null means the read failed or
     * ran out of time, which is a guest - there is no state in which one of the
     * two published fields is set without the other.
     */
    private suspend fun loadPlayer(activity: Activity): Player? =
        withTimeoutOrNull(AttemptTimeoutMillis) {
            PlayGames.getPlayersClient(activity).currentPlayer.awaitOrNull().getOrNull()
        }

    /** The live activity, or null once it has gone. Read through a weak reference. */
    private fun liveActivity(): Activity? =
        activityRef?.get()?.takeUnless { it.isDestroyed || it.isFinishing }
}

/**
 * Awaits a Play Games [Task] as a [Result], without blocking.
 *
 * A Google `Task` has no coroutine-aware await of its own, and a wait that has
 * been abandoned has to be dropped rather than resumed - so the callback checks
 * whether its continuation is still wanted and returns quietly if it is not.
 * That is what makes this safe under `withTimeoutOrNull`: a task answering after
 * the player has already been treated as a guest cannot resume a dead
 * continuation and take the scope down with it.
 */
private suspend fun <T> Task<T>.awaitOrNull(): Result<T> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                val value = task.result
                if (value != null) {
                    continuation.resume(Result.success(value))
                } else {
                    continuation.resume(
                        Result.failure(IllegalStateException("Play Games task succeeded with no result"))
                    )
                }
            } else {
                val error = task.exception ?: IllegalStateException("Play Games task failed with no cause")
                continuation.resume(Result.failure(error))
            }
        }
    }