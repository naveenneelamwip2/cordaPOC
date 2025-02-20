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
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
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
import java.util.List;
import java.util.UUID;

@InitiatingFlow(protocol = "transfer-bonds")
public class TransferBondFlow implements ClientStartableFlow {

    private static final Logger log = LoggerFactory.getLogger(TransferBondFlow.class);

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
        log.info("TransferBondFlow.call() Called");

        TransferBondFlowRequest request = requestBody.getRequestBodyAs(jsonMarshallingService, TransferBondFlowRequest.class);

        MemberX500Name newOwnerName = request.getNewOwner();
        UUID bondId = request.getBondId();

        PublicKey issuer = memberLookup.myInfo().getLedgerKeys().get(0);
        String issuerName = memberLookup.myInfo().getName().getCommonName();

        final MemberInfo newOwnerInfo = memberLookup.lookup(newOwnerName);
        if (newOwnerInfo == null) {
            throw new IllegalArgumentException(String.format("The new owner %s does not exist within the network", newOwnerName));
        }

        final PublicKey newOwner;
        try {
            newOwner = newOwnerInfo.getLedgerKeys().get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("The new owner %s has no ledger key", newOwnerName));
        }

        StateAndRef<BondState> bondStateAndRef;
        try {
            bondStateAndRef = utxoLedgerService
                    .findUnconsumedStatesByExactType(BondState.class, 100, Instant.now()).getResults()
                    .stream()
                    .filter(stateAndRef -> stateAndRef.getState().getContractState().getBondId().equals(bondId))
                    .iterator()
                    .next();
        } catch (Exception e) {
            throw new IllegalArgumentException("There are no bonds found with bondId");
        }

        BondState originalBondState = bondStateAndRef.getState().getContractState();

        BondState updatedBondState = originalBondState.transfer(newOwnerName);

        NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();

        UtxoSignedTransaction transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notary.getName())
                .addOutputState(updatedBondState)
                .addCommand(new BondCommands.Transfer())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(issuer, newOwner))
                .toSignedTransaction();

        FlowSession session = flowMessaging.initiateFlow(newOwnerInfo.getName());

        try {
            utxoLedgerService.finalize(transaction, List.of(session));
            return updatedBondState.getBondId().toString();
        } catch (Exception e) {
            log.error(e.getMessage());
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
