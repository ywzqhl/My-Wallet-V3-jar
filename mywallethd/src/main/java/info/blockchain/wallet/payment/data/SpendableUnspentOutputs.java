package info.blockchain.wallet.payment.data;

import info.blockchain.wallet.send.MyTransactionOutPoint;

import java.math.BigInteger;
import java.util.List;

public class SpendableUnspentOutputs {

    private List<MyTransactionOutPoint> spendableOutputs = null;
    private BigInteger absoluteFee = BigInteger.ZERO;
    private BigInteger consumedAmount = BigInteger.ZERO;

    public SpendableUnspentOutputs() {
    }

    public void setSpendableOutputs(List<MyTransactionOutPoint> spendableOutputs) {
        this.spendableOutputs = spendableOutputs;
    }

    public List<MyTransactionOutPoint> getSpendableOutputs() {
        return spendableOutputs;
    }

    public BigInteger getAbsoluteFee() {
        return absoluteFee;
    }

    public void setAbsoluteFee(BigInteger absoluteFee) {
        this.absoluteFee = absoluteFee;
    }

    public BigInteger getConsumedAmount() {
        return consumedAmount;
    }

    public void setConsumedAmount(BigInteger consumedAmount) {
        this.consumedAmount = consumedAmount;
    }

    @Override
    public String toString() {
        return "SpendableUnspentOutputs{" +
                "spendableOutputs=" + spendableOutputs +
                ", absoluteFee=" + absoluteFee +
                ", consumedAmount=" + consumedAmount +
                '}';
    }
}
