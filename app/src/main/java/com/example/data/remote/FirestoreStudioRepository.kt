package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.example.model.BassBoostParams
import com.example.model.ChorusParams
import com.example.model.CloudDraft
import com.example.model.CompressorParams
import com.example.model.DistortionMode
import com.example.model.DistortionParams
import com.example.model.DspConfig
import com.example.model.EchoReverbParams
import com.example.model.EffectSlot
import com.example.model.EffectType
import com.example.model.EqualizerParams
import com.example.model.FlangerParams
import com.example.model.LimiterParams
import com.example.model.NoiseReductionParams
import com.example.model.PhaserParams
import com.example.model.PitchShiftParams
import com.example.model.StudioPreset
import com.example.model.TimeStretchParams
import com.example.model.TrebleParams
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.net.SocketTimeoutException
import kotlin.math.abs

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE,
}

@Keep
@IgnoreExtraProperties
data class FirestoreCustomPresetDoc(
    val userId: String = "",
    val localId: Long = 0L,
    val name: String = "",
    val description: String = "",
    val dspConfig: Map<String, Any> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FirestoreDraftDoc(
    val userId: String = "",
    val name: String = "",
    val selectedPresetName: String = "",
    val durationMs: Long = 0L,
    val sampleRate: Int = 44100,
    val dspConfig: Map<String, Any> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

object DspConfigFirestoreMapper {
    fun toMap(config: DspConfig): Map<String, Any> {
        val norm = config.normalized()
        return mapOf(
            "bypassAll" to norm.bypassAll,
            "masterGainDb" to norm.masterGainDb.toDouble(),
            "spatialPanHz" to norm.spatialPanHz.toDouble(),
            "vibratoHz" to norm.vibratoHz.toDouble(),
            "vibratoDepth" to norm.vibratoDepth.toDouble(),
            "chain" to norm.chain.map { slot ->
                mapOf(
                    "type" to slot.type.name,
                    "enabled" to slot.enabled,
                )
            },
            "echoReverb" to mapOf(
                "echoStrength" to norm.echoReverb.echoStrength.toDouble(),
                "echoDepth" to norm.echoReverb.echoDepth.toDouble(),
                "delayTimeMs" to norm.echoReverb.delayTimeMs.toDouble(),
                "feedback" to norm.echoReverb.feedback.toDouble(),
                "reverbSize" to norm.echoReverb.reverbSize.toDouble(),
                "reverbDecay" to norm.echoReverb.reverbDecay.toDouble(),
                "preDelayMs" to norm.echoReverb.preDelayMs.toDouble(),
                "wetDryMix" to norm.echoReverb.wetDryMix.toDouble(),
                "stereoWidth" to norm.echoReverb.stereoWidth.toDouble(),
                "earlyReflections" to norm.echoReverb.earlyReflections.toDouble(),
                "lateReflections" to norm.echoReverb.lateReflections.toDouble(),
                "damping" to norm.echoReverb.damping.toDouble(),
                "diffusion" to norm.echoReverb.diffusion.toDouble(),
            ),
            "equalizer" to run {
                val b10 = norm.equalizer.resolved10Bands()
                mapOf(
                    "band60HzDb" to norm.equalizer.band60HzDb.toDouble(),
                    "band230HzDb" to norm.equalizer.band230HzDb.toDouble(),
                    "band910HzDb" to norm.equalizer.band910HzDb.toDouble(),
                    "band3600HzDb" to norm.equalizer.band3600HzDb.toDouble(),
                    "band14000HzDb" to norm.equalizer.band14000HzDb.toDouble(),
                    "band31HzDb" to b10[0].toDouble(),
                    "band62HzDb" to b10[1].toDouble(),
                    "band125HzDb" to b10[2].toDouble(),
                    "band250HzDb" to b10[3].toDouble(),
                    "band500HzDb" to b10[4].toDouble(),
                    "band1000HzDb" to b10[5].toDouble(),
                    "band2000HzDb" to b10[6].toDouble(),
                    "band4000HzDb" to b10[7].toDouble(),
                    "band8000HzDb" to b10[8].toDouble(),
                    "band16000HzDb" to b10[9].toDouble(),
                )
            },
            "bassBoost" to mapOf(
                "gainDb" to norm.bassBoost.gainDb.toDouble(),
                "frequencyHz" to norm.bassBoost.frequencyHz.toDouble(),
            ),
            "treble" to mapOf(
                "gainDb" to norm.treble.gainDb.toDouble(),
                "frequencyHz" to norm.treble.frequencyHz.toDouble(),
            ),
            "pitchShift" to mapOf(
                "semitones" to norm.pitchShift.semitones.toDouble(),
                "wetMix" to norm.pitchShift.wetMix.toDouble(),
                "preserveFormants" to norm.pitchShift.preserveFormants,
            ),
            "timeStretch" to mapOf(
                "tempoRatio" to norm.timeStretch.tempoRatio.toDouble(),
            ),
            "chorus" to mapOf(
                "rateHz" to norm.chorus.rateHz.toDouble(),
                "depthMs" to norm.chorus.depthMs.toDouble(),
                "mix" to norm.chorus.mix.toDouble(),
            ),
            "flanger" to mapOf(
                "rateHz" to norm.flanger.rateHz.toDouble(),
                "depthMs" to norm.flanger.depthMs.toDouble(),
                "feedback" to norm.flanger.feedback.toDouble(),
                "mix" to norm.flanger.mix.toDouble(),
            ),
            "phaser" to mapOf(
                "rateHz" to norm.phaser.rateHz.toDouble(),
                "depth" to norm.phaser.depth.toDouble(),
                "feedback" to norm.phaser.feedback.toDouble(),
                "mix" to norm.phaser.mix.toDouble(),
            ),
            "distortion" to mapOf(
                "drive" to norm.distortion.drive.toDouble(),
                "toneHz" to norm.distortion.toneHz.toDouble(),
                "mix" to norm.distortion.mix.toDouble(),
                "mode" to norm.distortion.mode.name,
            ),
            "compressor" to mapOf(
                "thresholdDb" to norm.compressor.thresholdDb.toDouble(),
                "ratio" to norm.compressor.ratio.toDouble(),
                "attackMs" to norm.compressor.attackMs.toDouble(),
                "releaseMs" to norm.compressor.releaseMs.toDouble(),
                "makeupGainDb" to norm.compressor.makeupGainDb.toDouble(),
            ),
            "limiter" to mapOf(
                "ceilingDb" to norm.limiter.ceilingDb.toDouble(),
                "releaseMs" to norm.limiter.releaseMs.toDouble(),
            ),
            "noiseReduction" to mapOf(
                "thresholdDb" to norm.noiseReduction.thresholdDb.toDouble(),
                "reductionDb" to norm.noiseReduction.reductionDb.toDouble(),
                "highPassHz" to norm.noiseReduction.highPassHz.toDouble(),
                "spectralStrength" to norm.noiseReduction.spectralStrength.toDouble(),
                "deEssAmount" to norm.noiseReduction.deEssAmount.toDouble(),
                "clarityBoostDb" to norm.noiseReduction.clarityBoostDb.toDouble(),
                "spectralCleanerEnabled" to norm.noiseReduction.spectralCleanerEnabled,
            ),
        )
    }

    private fun Map<*, *>?.f(key: String, default: Float): Float {
        val v = this?.get(key)
        return when (v) {
            is Number -> v.toFloat()
            else -> default
        }
    }

    private fun Map<*, *>?.b(key: String, default: Boolean): Boolean {
        val v = this?.get(key)
        return v as? Boolean ?: default
    }

    private fun Map<*, *>?.s(key: String, default: String): String {
        val v = this?.get(key)
        return v as? String ?: default
    }

    fun fromMap(raw: Map<*, *>?): DspConfig {
        if (raw == null) return DspConfig()
        val def = DspConfig()

        val chainRaw = raw["chain"] as? List<*>
        val parsedChain = chainRaw?.mapNotNull { entry ->
            val m = entry as? Map<*, *> ?: return@mapNotNull null
            val typeStr = m.s("type", "")
            val type = runCatching { EffectType.valueOf(typeStr) }.getOrNull() ?: return@mapNotNull null
            EffectSlot(type = type, enabled = m.b("enabled", false))
        }.orEmpty()

        val echoM = raw["echoReverb"] as? Map<*, *>
        val eqM = raw["equalizer"] as? Map<*, *>
        val bassM = raw["bassBoost"] as? Map<*, *>
        val trebM = raw["treble"] as? Map<*, *>
        val pitchM = raw["pitchShift"] as? Map<*, *>
        val timeM = raw["timeStretch"] as? Map<*, *>
        val choM = raw["chorus"] as? Map<*, *>
        val flgM = raw["flanger"] as? Map<*, *>
        val phsM = raw["phaser"] as? Map<*, *>
        val dstM = raw["distortion"] as? Map<*, *>
        val cmpM = raw["compressor"] as? Map<*, *>
        val limM = raw["limiter"] as? Map<*, *>
        val nrM = raw["noiseReduction"] as? Map<*, *>

        val distMode = runCatching {
            DistortionMode.valueOf(dstM.s("mode", def.distortion.mode.name))
        }.getOrDefault(def.distortion.mode)

        return DspConfig(
            bypassAll = raw.b("bypassAll", false),
            masterGainDb = raw.f("masterGainDb", 0f),
            spatialPanHz = raw.f("spatialPanHz", 0f),
            vibratoHz = raw.f("vibratoHz", 0f),
            vibratoDepth = raw.f("vibratoDepth", 0f),
            chain = parsedChain.ifEmpty { def.chain },
            echoReverb = EchoReverbParams(
                echoStrength = echoM.f("echoStrength", 0f),
                echoDepth = echoM.f("echoDepth", 0f),
                delayTimeMs = echoM.f("delayTimeMs", 240f),
                feedback = echoM.f("feedback", 0f),
                reverbSize = echoM.f("reverbSize", 0f),
                reverbDecay = echoM.f("reverbDecay", 0f),
                preDelayMs = echoM.f("preDelayMs", 0f),
                wetDryMix = echoM.f("wetDryMix", 0f),
                stereoWidth = echoM.f("stereoWidth", 0f),
                earlyReflections = echoM.f("earlyReflections", 0f),
                lateReflections = echoM.f("lateReflections", 0f),
                damping = echoM.f("damping", 0f),
                diffusion = echoM.f("diffusion", 0f),
            ),
            equalizer = EqualizerParams(
                band60HzDb = eqM.f("band60HzDb", 0f),
                band230HzDb = eqM.f("band230HzDb", 0f),
                band910HzDb = eqM.f("band910HzDb", 0f),
                band3600HzDb = eqM.f("band3600HzDb", 0f),
                band14000HzDb = eqM.f("band14000HzDb", 0f),
                band31HzDb = (eqM?.get("band31HzDb") as? Number)?.toFloat(),
                band62HzDb = (eqM?.get("band62HzDb") as? Number)?.toFloat(),
                band125HzDb = (eqM?.get("band125HzDb") as? Number)?.toFloat(),
                band250HzDb = (eqM?.get("band250HzDb") as? Number)?.toFloat(),
                band500HzDb = (eqM?.get("band500HzDb") as? Number)?.toFloat(),
                band1000HzDb = (eqM?.get("band1000HzDb") as? Number)?.toFloat(),
                band2000HzDb = (eqM?.get("band2000HzDb") as? Number)?.toFloat(),
                band4000HzDb = (eqM?.get("band4000HzDb") as? Number)?.toFloat(),
                band8000HzDb = (eqM?.get("band8000HzDb") as? Number)?.toFloat(),
                band16000HzDb = (eqM?.get("band16000HzDb") as? Number)?.toFloat(),
            ),
            bassBoost = BassBoostParams(
                gainDb = bassM.f("gainDb", 0f),
                frequencyHz = bassM.f("frequencyHz", 95f),
            ),
            treble = TrebleParams(
                gainDb = trebM.f("gainDb", 0f),
                frequencyHz = trebM.f("frequencyHz", 6500f),
            ),
            pitchShift = PitchShiftParams(
                semitones = pitchM.f("semitones", 0f),
                wetMix = pitchM.f("wetMix", 1f),
                preserveFormants = pitchM.b("preserveFormants", true),
            ),
            timeStretch = TimeStretchParams(
                tempoRatio = timeM.f("tempoRatio", 1f),
            ),
            chorus = ChorusParams(
                rateHz = choM.f("rateHz", 1.2f),
                depthMs = choM.f("depthMs", 6.5f),
                mix = choM.f("mix", 0f),
            ),
            flanger = FlangerParams(
                rateHz = flgM.f("rateHz", 0.45f),
                depthMs = flgM.f("depthMs", 3f),
                feedback = flgM.f("feedback", 0f),
                mix = flgM.f("mix", 0f),
            ),
            phaser = PhaserParams(
                rateHz = phsM.f("rateHz", 0.6f),
                depth = phsM.f("depth", 0f),
                feedback = phsM.f("feedback", 0f),
                mix = phsM.f("mix", 0f),
            ),
            distortion = DistortionParams(
                drive = dstM.f("drive", 1f),
                toneHz = dstM.f("toneHz", 4200f),
                mix = dstM.f("mix", 0f),
                mode = distMode,
            ),
            compressor = CompressorParams(
                thresholdDb = cmpM.f("thresholdDb", 0f),
                ratio = cmpM.f("ratio", 1f),
                attackMs = cmpM.f("attackMs", 12f),
                releaseMs = cmpM.f("releaseMs", 140f),
                makeupGainDb = cmpM.f("makeupGainDb", 0f),
            ),
            limiter = LimiterParams(
                ceilingDb = limM.f("ceilingDb", 0f),
                releaseMs = limM.f("releaseMs", 80f),
            ),
            noiseReduction = NoiseReductionParams(
                thresholdDb = nrM.f("thresholdDb", -70f),
                reductionDb = nrM.f("reductionDb", 0f),
                highPassHz = nrM.f("highPassHz", 20f),
                spectralStrength = nrM.f("spectralStrength", 0f),
                deEssAmount = nrM.f("deEssAmount", 0f),
                clarityBoostDb = nrM.f("clarityBoostDb", 0f),
                spectralCleanerEnabled = nrM.b("spectralCleanerEnabled", false),
            ),
        ).normalized()
    }
}

class FirestoreStudioRepository(context: Context) {
    private val appContext = context.applicationContext

    private fun dbOrNull(): FirebaseFirestore? {
        if (FirebaseApp.getApps(appContext).isEmpty()) return null
        val dbId = runCatching { appContext.getString(R.string.firestore_database_id) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return if (dbId != null) {
            runCatching { FirebaseFirestore.getInstance(dbId) }.getOrNull()
                ?: runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        } else {
            runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        }
    }

    private fun handleFirestoreError(
        exception: Throwable,
        operationType: OperationType,
        path: String,
    ) {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        val providerIds = currentUser?.providerData?.map { it.providerId }?.joinToString(",") ?: "none"
        val errJson = buildString {
            append("{")
            append("\"error\":\"${exception.message?.replace("\"", "\\\"") ?: "Unknown error"}\",")
            append("\"operationType\":\"${operationType.name}\",")
            append("\"path\":\"$path\",")
            append("\"authInfo\":{")
            append("\"userId\":\"${currentUser?.uid ?: "null"}\",")
            append("\"email\":\"${currentUser?.email ?: "null"}\",")
            append("\"emailVerified\":${currentUser?.isEmailVerified ?: false},")
            append("\"isAnonymous\":${currentUser?.isAnonymous ?: false},")
            append("\"providers\":\"$providerIds\"")
            append("}}")
        }
        Log.e("FirestoreError", errJson, exception)
        if (exception is FirebaseFirestoreException &&
            exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            Log.e("FirestoreError", "Permission denied during ${operationType.name} on $path")
        }
    }

    fun observeCustomPresets(userId: String): Flow<List<StudioPreset>> {
        val db = dbOrNull() ?: return flow {
            throw IllegalStateException("Firebase is not initialized")
        }
        val path = "users/$userId/custom_presets"
        val snapshotFlow = runCatching {
            db.collection("users")
                .document(userId)
                .collection("custom_presets")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
        }.getOrElse { e ->
            handleFirestoreError(e, OperationType.LIST, path)
            return flow { throw e }
        }
        return snapshotFlow
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val userOwner = doc.getString("userId").orEmpty()
                        val name = doc.getString("name")?.trim().orEmpty()
                        if (userOwner != userId || name.isEmpty()) return@mapNotNull null
                        val localId = doc.getLong("localId")
                            ?: (10_000L + (abs(doc.id.hashCode().toLong()) % 900_000L))
                        val desc = doc.getString("description").orEmpty()
                        val rawCfg = doc.get("dspConfig") as? Map<*, *>
                        StudioPreset(
                            id = localId,
                            name = name,
                            description = desc,
                            isBuiltIn = false,
                            config = DspConfigFirestoreMapper.fromMap(rawCfg),
                            cloudDocId = doc.id,
                        )
                    }.getOrNull()
                }
            }
            .catch { e ->
                if (e is CancellationException) {
                    throw e
                }
                handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveCustomPreset(
        userId: String,
        name: String,
        description: String,
        config: DspConfig,
    ): Result<String> {
        val db = dbOrNull()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized"))
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Preset name cannot be blank"))
        }
        val path = "users/$userId/custom_presets"
        return try {
            val col = db.collection("users").document(userId).collection("custom_presets")
            val docRef = col.document()
            val deterministicLocalId = 10_000L + (abs(docRef.id.hashCode().toLong()) % 900_000L)
            val payload = hashMapOf<String, Any>(
                "userId" to userId,
                "localId" to deterministicLocalId,
                "name" to cleanName,
                "description" to description.trim().take(240),
                "dspConfig" to DspConfigFirestoreMapper.toMap(config),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val completed = withTimeoutOrNull(30_000L) {
                docRef.set(payload).await()
                true
            }
            if (completed != true) {
                val timeoutEx = SocketTimeoutException("Firestore write timed out after 30000ms at $path")
                handleFirestoreError(timeoutEx, OperationType.CREATE, path)
                return Result.failure(timeoutEx)
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomPreset(userId: String, docId: String): Result<Unit> {
        val db = dbOrNull()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized"))
        val path = "users/$userId/custom_presets/$docId"
        return try {
            val completed = withTimeoutOrNull(30_000L) {
                db.collection("users")
                    .document(userId)
                    .collection("custom_presets")
                    .document(docId)
                    .delete()
                    .await()
                true
            }
            if (completed != true) {
                val timeoutEx = SocketTimeoutException("Firestore delete timed out after 30000ms at $path")
                handleFirestoreError(timeoutEx, OperationType.DELETE, path)
                return Result.failure(timeoutEx)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeDrafts(userId: String): Flow<List<CloudDraft>> {
        val db = dbOrNull() ?: return flow {
            throw IllegalStateException("Firebase is not initialized")
        }
        val path = "users/$userId/drafts"
        val snapshotFlow = runCatching {
            db.collection("users")
                .document(userId)
                .collection("drafts")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
        }.getOrElse { e ->
            handleFirestoreError(e, OperationType.LIST, path)
            return flow { throw e }
        }
        return snapshotFlow
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val userOwner = doc.getString("userId").orEmpty()
                        val name = doc.getString("name")?.trim().orEmpty()
                        if (userOwner != userId || name.isEmpty()) return@mapNotNull null
                        val updatedTs = doc.getTimestamp(
                            "updatedAt",
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )?.toDate()?.time ?: System.currentTimeMillis()
                        val presetName = doc.getString("selectedPresetName").orEmpty()
                        val durationMs = (doc.getLong("durationMs") ?: 0L).coerceAtLeast(0L)
                        val sampleRate = (doc.getLong("sampleRate")?.toInt() ?: 44100).coerceIn(8000, 192000)
                        val rawCfg = doc.get("dspConfig") as? Map<*, *>
                        CloudDraft(
                            docId = doc.id,
                            name = name,
                            selectedPresetName = presetName,
                            durationMs = durationMs,
                            sampleRate = sampleRate,
                            config = DspConfigFirestoreMapper.fromMap(rawCfg),
                            updatedAtMs = updatedTs,
                        )
                    }.getOrNull()
                }
            }
            .catch { e ->
                if (e is CancellationException) {
                    throw e
                }
                handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun saveDraft(
        userId: String,
        existingDraftId: String?,
        draftId: String? = null,
        name: String,
        selectedPresetName: String,
        durationMs: Long,
        sampleRate: Int,
        config: DspConfig,
    ): Result<String> {
        val db = dbOrNull()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized"))
        val cleanName = name.trim().take(100)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Draft name cannot be blank"))
        }
        val cleanPreset = selectedPresetName.trim().take(100)
        val safeRate = sampleRate.coerceIn(8000, 192000)
        val safeDuration = durationMs.coerceAtLeast(0L)
        val col = db.collection("users").document(userId).collection("drafts")
        val targetRef = when {
            !existingDraftId.isNullOrBlank() -> col.document(existingDraftId)
            !draftId.isNullOrBlank() -> col.document(draftId)
            else -> col.document()
        }
        val path = "users/$userId/drafts/${targetRef.id}"
        return try {
            val opType = if (existingDraftId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
            val payload = hashMapOf<String, Any>(
                "userId" to userId,
                "name" to cleanName,
                "selectedPresetName" to cleanPreset,
                "durationMs" to safeDuration,
                "sampleRate" to safeRate,
                "dspConfig" to DspConfigFirestoreMapper.toMap(config),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).apply {
                if (existingDraftId.isNullOrBlank()) {
                    put("createdAt", FieldValue.serverTimestamp())
                }
            }
            val completed = withTimeoutOrNull(30_000L) {
                targetRef.set(payload, SetOptions.merge()).await()
                true
            }
            if (completed != true) {
                val timeoutEx = SocketTimeoutException("Firestore draft write timed out after 30000ms at $path")
                handleFirestoreError(timeoutEx, opType, path)
                return Result.failure(timeoutEx)
            }
            Result.success(targetRef.id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(
                e,
                if (existingDraftId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                path,
            )
            Result.failure(e)
        }
    }

    suspend fun deleteDraft(userId: String, draftId: String): Result<Unit> {
        val db = dbOrNull()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized"))
        val path = "users/$userId/drafts/$draftId"
        return try {
            val completed = withTimeoutOrNull(30_000L) {
                db.collection("users")
                    .document(userId)
                    .collection("drafts")
                    .document(draftId)
                    .delete()
                    .await()
                true
            }
            if (completed != true) {
                val timeoutEx = SocketTimeoutException("Firestore draft delete timed out after 30000ms at $path")
                handleFirestoreError(timeoutEx, OperationType.DELETE, path)
                return Result.failure(timeoutEx)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserProjectData(userId: String): Result<Unit> {
        val db = dbOrNull()
            ?: return Result.failure(IllegalStateException("Firebase is not initialized"))
        if (!userId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
            return Result.failure(IllegalArgumentException("Invalid user identifier"))
        }

        val userRef = db.collection("users").document(userId)
        val path = "users/$userId"
        return try {
            val completed = withTimeoutOrNull(120_000L) {
                for (subcollection in listOf("custom_presets", "drafts")) {
                    val collection = userRef.collection(subcollection)
                    while (true) {
                        val page = collection.limit(450).get(Source.SERVER).await()
                        if (page.isEmpty) break
                        val batch = db.batch()
                        page.documents.forEach { document -> batch.delete(document.reference) }
                        batch.commit().await()
                    }
                }
                true
            }
            if (completed != true) {
                val timeoutEx = SocketTimeoutException("User project data deletion timed out after 120000ms at $path")
                handleFirestoreError(timeoutEx, OperationType.DELETE, path)
                return Result.failure(timeoutEx)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
