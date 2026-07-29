package com.example.domain.marketplace

class EvaluateRoleAccessUseCase {
    operator fun invoke(
        policy: RoleAuthPolicy,
        evidence: RoleAccessEvidence
    ): RoleAccessDecision {
        return RoleAccessDecision(
            isPrimaryFactorMissing = evidence.verifiedFactors.none(
                policy.acceptedPrimaryFactors::contains
            ),
            missingSecondaryFactors = policy.requiredSecondaryFactors - evidence.verifiedFactors,
            needsAdminApproval = policy.requiresAdminApproval && !evidence.isAdminApproved,
            needsKycApproval = policy.requiresKycApproval && !evidence.isKycApproved
        )
    }
}
