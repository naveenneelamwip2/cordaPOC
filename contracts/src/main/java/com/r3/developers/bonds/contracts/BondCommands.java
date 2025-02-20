package com.r3.developers.bonds.contracts;

import net.corda.v5.ledger.utxo.Command;

public interface BondCommands extends Command {
    public class Issue implements BondCommands {};
    public class Transfer implements  BondCommands {};
}
