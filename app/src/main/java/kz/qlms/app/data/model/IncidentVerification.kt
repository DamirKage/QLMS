package kz.qlms.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class VerificationVote { CONFIRM, DISPUTE }

/**
 * One community member's vote on whether a report still looks accurate —
 * Waze/Citizen-style crowd verification, aimed at the credibility problem
 * unverified crowd reports run into at scale (see docs/MARKET_RESEARCH.md).
 * Stored one doc per uid at incidents/{id}/verifications/{uid} so a person
 * can only ever have one active vote per incident and can change their mind;
 * the incident doc's confirmCount/disputeCount are the aggregate the feed
 * actually reads, kept in sync transactionally in IncidentRepository.
 */
data class IncidentVerification(
    @get:Exclude @set:Exclude var uid: String = "",
    var voteName: String = VerificationVote.CONFIRM.name,
    @ServerTimestamp var votedAt: Date? = null,
) {
    @get:Exclude
    val vote: VerificationVote
        get() = VerificationVote.entries.find { it.name == voteName } ?: VerificationVote.CONFIRM
}
