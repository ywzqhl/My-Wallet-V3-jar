package info.blockchain.wallet.metadata.data;

public class MetadataResponse {

    int version;
    String payload;
    String signature;
    String prev_magic_hash;
    int type_id;
    long created_at;
    long updated_at;
    String address;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPrev_magic_hash() {
        return prev_magic_hash;
    }

    public void setPrev_magic_hash(String prev_magic_hash) {
        this.prev_magic_hash = prev_magic_hash;
    }

    public int getType_id() {
        return type_id;
    }

    public void setType_id(int type_id) {
        this.type_id = type_id;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public long getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(long updated_at) {
        this.updated_at = updated_at;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
