package com.r3.developers.bonds.contracts;

import com.r3.developers.bonds.states.BondState;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;


public class BondContract implements Contract {
    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        final Command command = transaction.getCommands().get(0);

//        BondState output = transaction.getOutputStates(BondState.class).get(0);
        if (command instanceof BondCommands.Issue){
            require(transaction.getOutputContractStates().size() == 1,
                    "The transaction should only have one state");
        } else if (command instanceof  BondCommands.Transfer){
            require(transaction.getOutputContractStates().size() == 1,
                    "The transaction should only have one state");
        } else {
            throw new CordaRuntimeException(String.format("Incorrect type of BondContract command %s", command.getClass().toString()));
        }
    }

    private void require(boolean asserted,String errorMessage){
        if(!asserted){
            throw new CordaRuntimeException(errorMessage);
        }
    }
}
