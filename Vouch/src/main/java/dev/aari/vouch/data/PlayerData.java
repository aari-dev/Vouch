package dev.aari.vouch.data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final UUID playerId;
    private final Set<UUID> vouchers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> superVouchers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> devouchers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastVouchTimes = new ConcurrentHashMap<>();
    private long firstJoined;
    private long lastSeen;
    private boolean modified = false;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.firstJoined = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }

    public PlayerData(UUID playerId, long firstJoined, long lastSeen) {
        this.playerId = playerId;
        this.firstJoined = firstJoined;
        this.lastSeen = lastSeen;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<UUID> getVouchers() {
        return vouchers;
    }

    public Set<UUID> getSuperVouchers() {
        return superVouchers;
    }

    public Set<UUID> getDevouchers() {
        return devouchers;
    }

    public Map<UUID, Long> getLastVouchTimes() {
        return lastVouchTimes;
    }

    public boolean canVouchTarget(UUID targetId) {
        Long lastVouchTime = lastVouchTimes.get(targetId);
        if (lastVouchTime == null) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;
        return (currentTime - lastVouchTime) >= dayInMillis;
    }

    public long getTimeUntilCanVouch(UUID targetId) {
        Long lastVouchTime = lastVouchTimes.get(targetId);
        if (lastVouchTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;
        long timeSinceLastVouch = currentTime - lastVouchTime;
        return Math.max(0, dayInMillis - timeSinceLastVouch);
    }

    public void recordVouchTime(UUID targetId) {
        lastVouchTimes.put(targetId, System.currentTimeMillis());
        setModified(true);
    }

    public void addVoucher(UUID voucher) {
        devouchers.remove(voucher);
        vouchers.add(voucher);
        setModified(true);
    }

    public void addSuperVoucher(UUID voucher) {
        devouchers.remove(voucher);
        vouchers.remove(voucher);
        superVouchers.add(voucher);
        setModified(true);
    }

    public void addDevoucher(UUID devoucher) {
        vouchers.remove(devoucher);
        superVouchers.remove(devoucher);
        devouchers.add(devoucher);
        setModified(true);
    }

    public int getTotalVouches() {
        return vouchers.size() + (superVouchers.size() * 2);
    }

    public int getTotalDevouches() {
        return devouchers.size();
    }

    public int getNetReputation() {
        return getTotalVouches() - getTotalDevouches();
    }

    public double getReputationStars() {
        int netRep = getNetReputation();
        if (netRep <= 0) return 0.0;

        double stars = Math.min(5.0, Math.log(netRep + 1) * 1.5);
        return Math.round(stars * 10.0) / 10.0;
    }

    public String getTrustLevel() {
        double stars = getReputationStars();
        if (stars >= 4.5) return "Highly Trusted";
        if (stars >= 3.5) return "Trusted";
        if (stars >= 2.5) return "Reliable";
        if (stars >= 1.5) return "Neutral";
        if (stars >= 0.5) return "Caution";
        return "Untrustworthy";
    }

    public long getFirstJoined() {
        return firstJoined;
    }

    public void setFirstJoined(long firstJoined) {
        this.firstJoined = firstJoined;
        setModified(true);
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        setModified(true);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}