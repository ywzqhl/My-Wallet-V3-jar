package info.blockchain.wallet.send;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bitcoinj.core.bip44.Address;
import org.bitcoinj.core.bip44.WalletFactory;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.Hash;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.WebUtil;

/**
 *
 * SendCoins.java : singleton class for spending from Blockchain Android HD wallet
 *
 */
public class SendCoins	{

    private static SendCoins instance = null;

    private SendCoins () { ; }

    private String[] from = null;
    private HashMap<String,String> froms = null;

    private boolean sentChange = false;

    public static final BigInteger bDust = BigInteger.valueOf(Coin.parseCoin("0.00000546").longValue());
    public static final BigInteger bFee = BigInteger.valueOf(Coin.parseCoin("0.0001").longValue());

    public static SendCoins getInstance() {

        if(instance == null)	{
            instance = new SendCoins();
        }

        return instance;
    }

    /**
     * Creates, populates, and returns transaction instance for this
     * spend and returns it with calculated priority. Change output
     * is positioned randomly.
     *
     * @param  boolean isSimpleSend Always true, not currently used
     * @param  List<MyTransactionOutPoint> unspent Unspent outputs
     * @param  BigInteger amount Spending amount (not including fee)
     * @param  HashMap<String, BigInteger> receivingAddresses
     * @param  BigInteger fee Miner's fee for this spend
     * @param  String changeAddress Change address for this spend
     *
     * @return Pair<Transaction, Long>
     *
     */
    public Pair<Transaction, Long> makeTransaction(boolean isSimpleSend, List<MyTransactionOutPoint> unspent, HashMap<String, BigInteger> receivingAddresses, BigInteger fee, final String changeAddress) throws Exception {

        long priority = 0;

        if(unspent == null || unspent.size() == 0) {
//			throw new InsufficientFundsException("No free outputs to spend.");
            return null;
        }

        if(fee == null) {
            fee = BigInteger.ZERO;
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        // Construct a new transaction
        Transaction tx = new Transaction(MainNetParams.get());
        BigInteger outputValueSum = BigInteger.ZERO;

        for(Iterator<Entry<String, BigInteger>> iterator = receivingAddresses.entrySet().iterator(); iterator.hasNext();)   {
            Map.Entry<String, BigInteger> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger amount = mapEntry.getValue();

            if(amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
                throw new Exception("Invalid amount");
            }

            if(amount.compareTo(bDust) < 1)    {
              throw new Exception("Dust amount");
            }

            outputValueSum = outputValueSum.add(amount);
            // Add the output
            BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitcoinScript(new BitcoinAddress(toAddress));
            TransactionOutput output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(amount.longValue()), toOutputScript.getProgram());
            outputs.add(output);
        }

        // Now select the appropriate inputs
        BigInteger valueSelected = BigInteger.ZERO;
        BigInteger valueNeeded =  outputValueSum.add(fee);
        BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

        MyTransactionOutPoint changeOutPoint = null;

        for(MyTransactionOutPoint outPoint : unspent) {

            BitcoinScript script = new BitcoinScript(outPoint.getScriptBytes());

            if(script.getOutType() == BitcoinScript.ScriptOutTypeStrange) {
                continue;
            }

            BitcoinScript inputScript = new BitcoinScript(outPoint.getConnectedPubKeyScript());
            String address = inputScript.getAddress().toString();

            // if isSimpleSend don't use address as input if is output
            if(isSimpleSend && receivingAddresses.get(address) != null) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(MainNetParams.get(), null, new byte[0], outPoint);
            tx.addInput(input);
            valueSelected = valueSelected.add(outPoint.getValue());
            priority += outPoint.getValue().longValue() * outPoint.getConfirmations();

            if(changeAddress == null) {
                changeOutPoint = outPoint;
            }

            if(valueSelected.compareTo(valueNeeded) == 0 || valueSelected.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0) {
                break;
            }
        }

        if(valueSelected.compareTo(BigInteger.valueOf(2100000000000000L)) > 0)    {
            throw new Exception("21m limit exceeded");
        }

        // Check the amount we have selected is greater than the amount we need
        if(valueSelected.compareTo(valueNeeded) < 0) {
//			throw new InsufficientFundsException("Insufficient Funds");
            return null;
        }

        BigInteger change = valueSelected.subtract(outputValueSum).subtract(fee);
        // Now add the change if there is any
        if (change.compareTo(BigInteger.ZERO) > 0) {
            if(change.compareTo(bDust) <= 0)    {
                throw new Exception("Dust change amount");
            }
            BitcoinScript change_script;
            if (changeAddress != null) {
                change_script = BitcoinScript.createSimpleOutBitcoinScript(new BitcoinAddress(changeAddress));
                sentChange = true;
            }
            else {
                throw new Exception("Change address null");
            }
            TransactionOutput change_output = new TransactionOutput(MainNetParams.get(), null, Coin.valueOf(change.longValue()), change_script.getProgram());
            outputs.add(change_output);
        }
        else {
            sentChange = false;
        }

        Collections.shuffle(outputs, new SecureRandom());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());
        priority /= estimatedSize;

        return Pair.of(tx, priority);
    }

    /**
     * <p>Calculate signatures for inputs of a transaction.
     *
     * @param Transaction transaction  Transaction for which the inputs must be signed
     * @param Wallet wallet Wallet used as key bag, not for actual spending
     */
    public synchronized void signTx(Transaction transaction, Wallet wallet) throws ScriptException {

        List<TransactionInput> inputs = transaction.getInputs();

        TransactionSignature[] sigs = new TransactionSignature[inputs.size()];
        ECKey[] keys = new ECKey[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {

            TransactionInput input = inputs.get(i);

            // Find the signing key
            ECKey key = input.getOutpoint().getConnectedKey(wallet);
            // Keep key for script creation step below
            keys[i] = key;
            byte[] connectedPubKeyScript = input.getOutpoint().getConnectedPubKeyScript();
            if(key.hasPrivKey() || key.isEncrypted()) {
                sigs[i] = transaction.calculateSignature(i, key, connectedPubKeyScript, SigHash.ALL, false);
            }
            else {
                sigs[i] = TransactionSignature.dummy();   // watch only ?
            }
        }

        for(int i = 0; i < inputs.size(); i++) {
            if(sigs[i] == null)   {
                continue;
            }
            TransactionInput input = inputs.get(i);
            final TransactionOutput connectedOutput = input.getOutpoint().getConnectedOutput();
            Script scriptPubKey = connectedOutput.getScriptPubKey();
            if(scriptPubKey.isSentToAddress()) {
                input.setScriptSig(ScriptBuilder.createInputScript(sigs[i], keys[i]));
            }
            else if(scriptPubKey.isSentToRawPubKey()) {
                input.setScriptSig(ScriptBuilder.createInputScript(sigs[i]));
            }
            else {
                throw new RuntimeException("Unknown script type: " + scriptPubKey);
            }
        }

    }

    public String encodeHex(Transaction tx) {
      return new String(Hex.encode(tx.bitcoinSerialize()));
    }

    public String pushTx(String hexString) {

      try {
        return WebUtil.getInstance().postURL(WebUtil.SPEND_URL, "tx=" + hexString);
      }
      catch(Exception e) {
        return "Exception calling pushTx";
      }

    }

    public String pushTx(Transaction tx) {

      try {
        return WebUtil.getInstance().postURL(WebUtil.SPEND_URL, "tx=" + encodeHex(tx));
      }
      catch(Exception e) {
        return "Exception calling pushTx";
      }

    }

}