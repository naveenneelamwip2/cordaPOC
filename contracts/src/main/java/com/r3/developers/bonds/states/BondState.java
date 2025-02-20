package com.r3.developers.bonds.states;
import com.r3.developers.bonds.contracts.BondContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@BelongsToContract(BondContract.class)
public class BondState implements ContractState {
    private final UUID bondId;
    private final MemberX500Name issuer;
    private final MemberX500Name owner;
    private final int amount;
    private final List<PublicKey> participants;
    private final int couponRate;
    private final Date marutiryDate;

    @ConstructorForDeserialization()
    public BondState(UUID bondId, MemberX500Name issuer, MemberX500Name owner, int amount, List<PublicKey> participants, int couponRate, Date marutiryDate) {
        this.bondId = bondId;
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.participants = participants;
        this.couponRate = couponRate;
        this.marutiryDate = marutiryDate;
    }

    public MemberX500Name getIssuer() {
        return issuer;
    }

    public MemberX500Name getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }

    public UUID getBondId() {
        return bondId;
    }

    public BondState transfer(MemberX500Name newOwner){
        return new BondState(this.bondId, this.issuer, newOwner, amount, this.participants, this.couponRate, this.marutiryDate);
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

    public Date getMarutiryDate() {
        return marutiryDate;
    }

    public int getCouponRate() {
        return couponRate;
    }
}