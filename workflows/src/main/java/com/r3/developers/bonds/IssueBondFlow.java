package com.r3.developers.bonds;

import com.r3.developers.bonds.contracts.BondCommands;
import com.r3.developers.bonds.states.BondState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@InitiatingFlow(protocol = "issue-bonds")
public class IssueBondFlow implements ClientStartableFlow {

    private static final Logger log = LoggerFactory.getLogger(IssueBondFlow.class);

    @CordaInject
    public FlowMessaging flowMessaging;

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    public NotaryLookup notaryLookup;

    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull ClientRequestBody requestBody) {
        log.info("IssueBondFlow.call() Called");

        IssueBondFlowRequest request = requestBody.getRequestBodyAs(jsonMarshallingService, IssueBondFlowRequest.class);

        MemberX500Name ownerName = request.getOwner();
        int amount = request.getAmount();
        int couponRate = request.getCouponRate();
        Date maturityDate = request.getMaturityDate();

        PublicKey issuer = memberLookup.myInfo().getLedgerKeys().get(0);

        MemberX500Name issuerName = memberLookup.myInfo().getName();

        final MemberInfo bondOwnerInfo = memberLookup.lookup(ownerName);
        if (bondOwnerInfo == null) {
            throw new IllegalArgumentException(String.format("The bond holder %s does not exist within the network", ownerName));
        }

        final PublicKey owner;
        try {
            owner = bondOwnerInfo.getLedgerKeys().get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("The owner %s has no ledger key", ownerName));
        }

        BondState newState = new BondState(
                UUID.randomUUID(),
                issuerName,
                ownerName,
                amount,
                List.of(issuer, owner),
                couponRate,
                maturityDate
        );

        NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

        UtxoSignedTransaction transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notary.getName())
                .addOutputState(newState)
                .addCommand(new BondCommands.Issue())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(issuer))
                .toSignedTransaction();

        FlowSession session = flowMessaging.initiateFlow(bondOwnerInfo.getName());

        try {
            utxoLedgerService.finalize(transaction, List.of(session));
            return newState.getBondId().toString();
        } catch (Exception e) {
            log.error(e.getMessage());
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
